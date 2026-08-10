package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.StatusBarNotification

internal data class DiscordVoiceSignals(
    val sourceKey: String,
    val channelId: String?,
    val isOngoing: Boolean,
    val isForegroundService: Boolean,
    val isNoClear: Boolean,
    val isPromoted: Boolean,
    val postedAtEpochMillis: Long,
    val sourceTitle: String?,
    val sourceContentText: String?,
    val contentIntent: PendingIntent? = null,
    val sourceActions: List<Notification.Action> = emptyList(),
)

internal data class DiscordVoiceUpdate(
    val sourceKey: String,
    val postedAtEpochMillis: Long,
    val sourceTitle: String?,
    val sourceContentText: String?,
    val contentIntent: PendingIntent? = null,
    val sourceActions: List<Notification.Action> = emptyList(),
)

internal data class DiscordVoiceExtraction(
    val update: DiscordVoiceUpdate?,
    val diagnostics: Map<String, String>,
)

internal object DiscordVoiceNotificationParser {
    const val PACKAGE_NAME = "com.discord"
    const val VOICE_CHANNEL_ID = "mediaConnections"

    fun supportsPackage(packageName: String): Boolean = packageName == PACKAGE_NAME

    fun parse(signals: DiscordVoiceSignals): DiscordVoiceUpdate? {
        if (signals.channelId != VOICE_CHANNEL_ID) return null
        if (!signals.isOngoing || !signals.isForegroundService || !signals.isNoClear) return null
        if (signals.isPromoted) return null
        if (signals.sourceKey.isBlank() || signals.postedAtEpochMillis <= 0) return null

        return DiscordVoiceUpdate(
            sourceKey = signals.sourceKey,
            postedAtEpochMillis = signals.postedAtEpochMillis,
            sourceTitle = signals.sourceTitle.normalizedNotificationField(),
            sourceContentText = signals.sourceContentText.normalizedNotificationField(),
            contentIntent = signals.contentIntent,
            sourceActions = signals.sourceActions
                .filter { it.actionIntent != null }
                .take(MAX_SOURCE_ACTIONS),
        )
    }

    private fun String?.normalizedNotificationField(): String? =
        this?.replace(Regex("""\s+"""), " ")?.trim()?.takeIf(String::isNotEmpty)

    private const val MAX_SOURCE_ACTIONS = 3
}

internal object DiscordVoiceNotificationExtractor {
    fun extract(statusBarNotification: StatusBarNotification): DiscordVoiceExtraction {
        if (!DiscordVoiceNotificationParser.supportsPackage(statusBarNotification.packageName)) {
            return DiscordVoiceExtraction(null, mapOf("reason" to "unsupported_package"))
        }

        val notification = statusBarNotification.notification
        val flags = notification.flags
        val promotionState = ClockTimerPromotionPolicy.evaluate(notification)
        val signals = DiscordVoiceSignals(
            sourceKey = statusBarNotification.key,
            channelId = notification.channelId,
            isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0,
            isForegroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            isNoClear = flags and Notification.FLAG_NO_CLEAR != 0,
            isPromoted = promotionState.promoted,
            postedAtEpochMillis = statusBarNotification.postTime,
            sourceTitle = notification.extras
                .getCharSequence(Notification.EXTRA_TITLE)
                ?.toString(),
            sourceContentText = notification.extras
                .getCharSequence(Notification.EXTRA_TEXT)
                ?.toString(),
            contentIntent = notification.contentIntent,
            sourceActions = notification.actions.orEmpty().toList(),
        )
        val update = DiscordVoiceNotificationParser.parse(signals)
        val reason = when {
            signals.channelId != DiscordVoiceNotificationParser.VOICE_CHANNEL_ID -> "unsupported_channel"
            !signals.isOngoing -> "not_ongoing"
            !signals.isForegroundService -> "not_foreground_service"
            !signals.isNoClear -> "clearable"
            signals.isPromoted -> "already_promoted"
            signals.sourceKey.isBlank() -> "missing_source_key"
            signals.postedAtEpochMillis <= 0 -> "invalid_post_time"
            else -> "matched"
        }
        return DiscordVoiceExtraction(
            update = update,
            diagnostics = linkedMapOf(
                "reason" to reason,
                "sourceKey" to signals.sourceKey,
                "channelId" to signals.channelId.orEmpty(),
                "isOngoing" to signals.isOngoing.toString(),
                "isForegroundService" to signals.isForegroundService.toString(),
                "isNoClear" to signals.isNoClear.toString(),
                "isPromoted" to signals.isPromoted.toString(),
                "sourceActionCount" to signals.sourceActions.size.toString(),
                "mirroredActionCount" to update?.sourceActions?.size?.toString().orEmpty(),
            ),
        )
    }
}

internal sealed interface DiscordVoiceDecision {
    data class Show(val update: DiscordVoiceUpdate) : DiscordVoiceDecision
    data object Clear : DiscordVoiceDecision
    data object None : DiscordVoiceDecision
}

internal class DiscordVoiceTracker {
    private var activeSourceKey: String? = null

    fun onPosted(sourceKey: String, update: DiscordVoiceUpdate?): DiscordVoiceDecision {
        if (update != null) {
            activeSourceKey = sourceKey
            return DiscordVoiceDecision.Show(update)
        }
        if (activeSourceKey == sourceKey) {
            activeSourceKey = null
            return DiscordVoiceDecision.Clear
        }
        return DiscordVoiceDecision.None
    }

    fun onRemoved(sourceKey: String): DiscordVoiceDecision {
        if (activeSourceKey != sourceKey) return DiscordVoiceDecision.None
        activeSourceKey = null
        return DiscordVoiceDecision.Clear
    }

    fun restore(updates: List<DiscordVoiceUpdate>): DiscordVoiceDecision {
        val update = updates.maxByOrNull(DiscordVoiceUpdate::postedAtEpochMillis)
        activeSourceKey = update?.sourceKey
        return if (update == null) DiscordVoiceDecision.Clear else DiscordVoiceDecision.Show(update)
    }

    fun reset() {
        activeSourceKey = null
    }
}
