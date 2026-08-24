package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi

internal data class TeamsCallSignals(
    val sourceKey: String,
    val channelId: String?,
    val category: String?,
    val isOngoing: Boolean,
    val isForegroundService: Boolean,
    val isNoClear: Boolean,
    val isPromoted: Boolean,
    val sourceWhenEpochMillis: Long,
    val postedAtEpochMillis: Long,
    val sourceTitle: String?,
    val sourceContentText: String?,
    val contentIntent: PendingIntent? = null,
    val sourceActions: List<Notification.Action> = emptyList(),
)

internal data class TeamsCallUpdate(
    val sourceKey: String,
    val startedAtEpochMillis: Long,
    val sourceTitle: String?,
    val participantName: String?,
    val language: TeamsCallLanguage,
    val contentIntent: PendingIntent? = null,
    val sourceActions: List<Notification.Action> = emptyList(),
)

internal enum class TeamsCallLanguage {
    TRADITIONAL_CHINESE,
    ENGLISH,
}

internal data class TeamsCallExtraction(
    val update: TeamsCallUpdate?,
    val diagnostics: Map<String, String>,
)

internal object TeamsCallText {
    private val meetingWith = Regex("""^Meeting\s+with\s+(.+)$""", RegexOption.IGNORE_CASE)
    private val hanCharacter = Regex("""[\p{sc=Han}]""")
    private const val RETURN_TO_MEETING_ZH = "點一下以返回會議"
    private const val RETURN_TO_MEETING_EN = "Tap to return to the meeting"

    fun participantName(sourceTitle: String?): String? {
        val title = sourceTitle.normalizedNotificationField() ?: return null
        return meetingWith.matchEntire(title)
            ?.groupValues
            ?.getOrNull(1)
            ?.normalizedNotificationField()
    }

    fun contentText(
        update: TeamsCallUpdate,
        fallback: String,
    ): String = update.participantName ?: update.sourceTitle ?: fallback

    fun language(sourceTitle: String?, sourceContentText: String?): TeamsCallLanguage {
        val title = sourceTitle.normalizedNotificationField()
        val contentText = sourceContentText.normalizedNotificationField()
        if (contentText.equals(RETURN_TO_MEETING_ZH, ignoreCase = true)) {
            return TeamsCallLanguage.TRADITIONAL_CHINESE
        }
        if (contentText.equals(RETURN_TO_MEETING_EN, ignoreCase = true)) {
            return TeamsCallLanguage.ENGLISH
        }
        val sample = contentText ?: title
        return if (sample != null && !hanCharacter.containsMatchIn(sample)) {
            TeamsCallLanguage.ENGLISH
        } else {
            TeamsCallLanguage.TRADITIONAL_CHINESE
        }

    }

    fun actionTitle(
        sourceTitle: CharSequence,
        language: TeamsCallLanguage,
    ): String = sourceTitle.toString().normalizedNotificationField().let { title ->
        when (language) {
            TeamsCallLanguage.ENGLISH -> when (title?.lowercase()) {
                "啟用通知", "解除靜音", "unmute" -> "Unmute"
                "靜音", "mute" -> "Mute"
                "掛斷", "hang up" -> "Hang up"
                else -> title.orEmpty()
            }
            TeamsCallLanguage.TRADITIONAL_CHINESE -> when (title?.lowercase()) {
                "啟用通知", "解除靜音", "unmute" -> "解除靜音"
                "靜音", "mute" -> "靜音"
                "掛斷", "hang up" -> "掛斷"
                else -> title.orEmpty()
            }
        }
    }

    private fun String?.normalizedNotificationField(): String? =
        this?.replace(Regex("""\s+"""), " ")?.trim()?.takeIf(String::isNotEmpty)
}

internal object TeamsCallNotificationParser {
    const val PACKAGE_NAME = "com.microsoft.teams"
    const val ONGOING_CALL_CHANNEL_PREFIX = "com.microsoft.teams.CallsOngoing."

    fun supportsPackage(packageName: String): Boolean = packageName == PACKAGE_NAME

    fun parse(signals: TeamsCallSignals): TeamsCallUpdate? {
        if (!signals.channelId.orEmpty().startsWith(ONGOING_CALL_CHANNEL_PREFIX)) return null
        if (signals.category != Notification.CATEGORY_CALL) return null
        if (!signals.isOngoing || !signals.isForegroundService || !signals.isNoClear) return null
        if (signals.isPromoted || signals.sourceKey.isBlank()) return null

        val startedAtEpochMillis = signals.sourceWhenEpochMillis.takeIf { it > 0 }
            ?: signals.postedAtEpochMillis.takeIf { it > 0 }
            ?: return null
        val sourceTitle = signals.sourceTitle.normalizedNotificationField()
        return TeamsCallUpdate(
            sourceKey = signals.sourceKey,
            startedAtEpochMillis = startedAtEpochMillis,
            sourceTitle = sourceTitle,
            participantName = TeamsCallText.participantName(sourceTitle),
            language = TeamsCallText.language(sourceTitle, signals.sourceContentText),
            contentIntent = signals.contentIntent,
            sourceActions = signals.sourceActions
                .filter { it.actionIntent != null && !it.title.isNullOrBlank() }
                .take(MAX_SOURCE_ACTIONS),
        )
    }

    private fun String?.normalizedNotificationField(): String? =
        this?.replace(Regex("""\s+"""), " ")?.trim()?.takeIf(String::isNotEmpty)

    private const val MAX_SOURCE_ACTIONS = 2
}

internal object TeamsCallNotificationExtractor {
    fun extract(statusBarNotification: StatusBarNotification): TeamsCallExtraction {
        if (!TeamsCallNotificationParser.supportsPackage(statusBarNotification.packageName)) {
            return TeamsCallExtraction(null, mapOf("reason" to "unsupported_package"))
        }

        val notification = statusBarNotification.notification
        val flags = notification.flags
        val promotionState = ClockTimerPromotionPolicy.evaluate(notification)
        val signals = TeamsCallSignals(
            sourceKey = statusBarNotification.key,
            channelId = notification.channelId,
            category = notification.category,
            isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0,
            isForegroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            isNoClear = flags and Notification.FLAG_NO_CLEAR != 0,
            isPromoted = promotionState.promoted,
            sourceWhenEpochMillis = notification.`when`,
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
        val update = TeamsCallNotificationParser.parse(signals)
        val reason = when {
            !signals.channelId.orEmpty().startsWith(
                TeamsCallNotificationParser.ONGOING_CALL_CHANNEL_PREFIX,
            ) -> "unsupported_channel"
            signals.category != Notification.CATEGORY_CALL -> "not_call_category"
            !signals.isOngoing -> "not_ongoing"
            !signals.isForegroundService -> "not_foreground_service"
            !signals.isNoClear -> "clearable"
            signals.isPromoted -> "already_promoted"
            signals.sourceKey.isBlank() -> "missing_source_key"
            signals.sourceWhenEpochMillis <= 0 && signals.postedAtEpochMillis <= 0 ->
                "invalid_start_time"
            else -> "matched"
        }
        return TeamsCallExtraction(
            update = update,
            diagnostics = linkedMapOf(
                "reason" to reason,
                "sourceKey" to signals.sourceKey,
                "channelId" to signals.channelId.orEmpty(),
                "category" to signals.category.orEmpty(),
                "isOngoing" to signals.isOngoing.toString(),
                "isForegroundService" to signals.isForegroundService.toString(),
                "isNoClear" to signals.isNoClear.toString(),
                "isPromoted" to signals.isPromoted.toString(),
                "sourceWhenEpochMillis" to signals.sourceWhenEpochMillis.toString(),
                "startedAtEpochMillis" to update?.startedAtEpochMillis?.toString().orEmpty(),
                "participantName" to update?.participantName.orEmpty(),
                "language" to update?.language?.name.orEmpty(),
                "sourceActionCount" to signals.sourceActions.size.toString(),
                "mirroredActionCount" to update?.sourceActions?.size?.toString().orEmpty(),
            ),
        )
    }
}

internal sealed interface TeamsCallDecision {
    data class Show(val update: TeamsCallUpdate) : TeamsCallDecision
    data object Clear : TeamsCallDecision
    data object None : TeamsCallDecision
}

internal class TeamsCallTracker {
    private var activeSourceKey: String? = null

    fun onPosted(sourceKey: String, update: TeamsCallUpdate?): TeamsCallDecision {
        if (update != null) {
            activeSourceKey = sourceKey
            return TeamsCallDecision.Show(update)
        }
        if (activeSourceKey == sourceKey) {
            activeSourceKey = null
            return TeamsCallDecision.Clear
        }
        return TeamsCallDecision.None
    }

    fun onRemoved(sourceKey: String): TeamsCallDecision {
        if (activeSourceKey != sourceKey) return TeamsCallDecision.None
        activeSourceKey = null
        return TeamsCallDecision.Clear
    }

    fun restore(updates: List<TeamsCallUpdate>): TeamsCallDecision {
        val update = updates.maxByOrNull(TeamsCallUpdate::startedAtEpochMillis)
        activeSourceKey = update?.sourceKey
        return if (update == null) TeamsCallDecision.Clear else TeamsCallDecision.Show(update)
    }

    fun reset() {
        activeSourceKey = null
    }
}

internal object TeamsCallNotificationStyle {
    fun apply(
        builder: Notification.Builder,
        startedAtEpochMillis: Long,
        metricLabel: String,
        nowEpochMillis: Long = System.currentTimeMillis(),
        nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        if (Build.VERSION.SDK_INT >= 37) {
            TeamsCallStyleApi37.apply(
                builder,
                elapsedRealtimeStartMillis(
                    startedAtEpochMillis,
                    nowEpochMillis,
                    nowElapsedRealtimeMillis,
                ),
                metricLabel,
            )
        } else {
            builder
                .setWhen(startedAtEpochMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(false)
        }
    }

    internal fun elapsedRealtimeStartMillis(
        startedAtEpochMillis: Long,
        nowEpochMillis: Long,
        nowElapsedRealtimeMillis: Long,
    ): Long = (nowElapsedRealtimeMillis -
        (nowEpochMillis - startedAtEpochMillis).coerceAtLeast(0L)).coerceAtLeast(0L)
}

@RequiresApi(37)
private object TeamsCallStyleApi37 {
    fun apply(
        builder: Notification.Builder,
        startedAtElapsedRealtimeMillis: Long,
        metricLabel: String,
    ) {
        val stopwatch = Notification.Metric.TimeDifference.forStopwatch(
            startedAtElapsedRealtimeMillis,
            Notification.Metric.TimeDifference.FORMAT_CHRONOMETER,
        )
        builder.setStyle(
            Notification.MetricStyle()
                .addMetric(Notification.Metric(stopwatch, metricLabel))
                .setCriticalMetric(0),
        )
    }
}
