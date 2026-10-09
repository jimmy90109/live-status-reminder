package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.StatusBarNotification

internal object LineCallPolicy {
    fun supportsDevice(manufacturer: String?, brand: String?, debugBuild: Boolean = false): Boolean =
        debugBuild || XiaomiHyperIslandRenderer.shouldRender(manufacturer, brand)

    fun shouldMirror(
        manufacturer: String?,
        brand: String?,
        enabled: Boolean,
        debugBuild: Boolean = false,
    ): Boolean = enabled && supportsDevice(manufacturer, brand, debugBuild)
}

internal data class LineCallSignals(
    val packageName: String,
    val sourceKey: String,
    val channelId: String?,
    val category: String?,
    val template: String?,
    val callType: Int?,
    val isVideo: Boolean?,
    val isOngoing: Boolean,
    val isForegroundService: Boolean,
    val isNoClear: Boolean,
    val isPromoted: Boolean,
    val sourceTitle: String?,
    val sourceContentText: String?,
    val postedAtEpochMillis: Long,
    val sourceWhenEpochMillis: Long = 0,
)

internal data class LineCallUpdate(
    val sourceKey: String,
    val sourceTitle: String?,
    val sourceContentText: String?,
    val postedAtEpochMillis: Long,
    val visibility: Int = Notification.VISIBILITY_PRIVATE,
    val contentIntent: PendingIntent? = null,
    val hangUpAction: Notification.Action? = null,
    val sourceWhenEpochMillis: Long = 0,
)

internal object LineCallNotificationParser {
    const val PACKAGE_NAME = "jp.naver.line.android"
    const val ONGOING_CHANNEL = "jp.naver.line.android.notification.VoIP.02.Ongoing"
    const val CALL_TEMPLATE = "android.app.Notification\$CallStyle"

    fun parse(signals: LineCallSignals): LineCallUpdate? {
        if (signals.packageName != PACKAGE_NAME || signals.sourceKey.isBlank()) return null
        if (signals.channelId != ONGOING_CHANNEL || signals.category != "call") return null
        if (signals.template != CALL_TEMPLATE || signals.callType != 2 || signals.isVideo != false) return null
        if (!signals.isOngoing || !signals.isForegroundService || !signals.isNoClear || signals.isPromoted) return null
        return LineCallUpdate(
            sourceKey = signals.sourceKey,
            sourceTitle = signals.sourceTitle.normalized(),
            sourceContentText = signals.sourceContentText.normalized(),
            postedAtEpochMillis = signals.postedAtEpochMillis,
            sourceWhenEpochMillis = signals.sourceWhenEpochMillis,
        )
    }

    private fun String?.normalized(): String? =
        this?.replace(Regex("\\s+"), " ")?.trim()?.takeIf(String::isNotEmpty)
}

internal object LineCallNotificationExtractor {
    fun extract(context: Context, source: StatusBarNotification): LineCallUpdate? {
        val notification = source.notification
        val extras = notification.extras
        val flags = notification.flags
        val update = LineCallNotificationParser.parse(
            LineCallSignals(
                packageName = source.packageName,
                sourceKey = source.key,
                channelId = notification.channelId,
                category = notification.category,
                template = extras.getString(Notification.EXTRA_TEMPLATE),
                callType = if (extras.containsKey("android.callType")) extras.getInt("android.callType") else null,
                isVideo = if (extras.containsKey("android.callIsVideo")) extras.getBoolean("android.callIsVideo") else null,
                isOngoing = flags and Notification.FLAG_ONGOING_EVENT != 0,
                isForegroundService = flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
                isNoClear = flags and Notification.FLAG_NO_CLEAR != 0,
                isPromoted = flags and Notification.FLAG_PROMOTED_ONGOING != 0,
                sourceTitle = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                sourceContentText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                postedAtEpochMillis = source.postTime,
                sourceWhenEpochMillis = notification.`when`,
            ),
        ) ?: return null
        val hangUpIntent = extras.getParcelable("android.hangUpIntent", PendingIntent::class.java)
        // Use the original executable intent, including LINE's semanticAction=0 case.
        val sourceHangUp = notification.actions.orEmpty().firstOrNull { action ->
            action.actionIntent != null && (
                action.actionIntent == hangUpIntent ||
                    action.title?.toString()?.trim()?.lowercase() in listOf("hang up", "掛斷", "挂断")
                )
        }?.actionIntent ?: hangUpIntent
        return update.copy(
            visibility = notification.visibility,
            contentIntent = notification.contentIntent,
            hangUpAction = sourceHangUp?.let {
                Notification.Action.Builder(null, context.getString(R.string.line_call_hang_up), it).build()
            },
        )
    }
}

internal sealed interface LineCallDecision {
    data class Show(val update: LineCallUpdate) : LineCallDecision
    data object Clear : LineCallDecision
    data object None : LineCallDecision
}

internal class LineCallTracker {
    private var activeSourceKey: String? = null

    fun onPosted(sourceKey: String, update: LineCallUpdate?): LineCallDecision {
        if (update != null) {
            activeSourceKey = sourceKey
            return LineCallDecision.Show(update)
        }
        return if (activeSourceKey == sourceKey) reset() else LineCallDecision.None
    }

    fun onRemoved(sourceKey: String): LineCallDecision =
        if (activeSourceKey == sourceKey) reset() else LineCallDecision.None

    fun restore(updates: List<LineCallUpdate>): LineCallDecision {
        val update = updates.maxByOrNull(LineCallUpdate::postedAtEpochMillis) ?: return reset()
        activeSourceKey = update.sourceKey
        return LineCallDecision.Show(update)
    }

    fun reset(): LineCallDecision {
        activeSourceKey = null
        return LineCallDecision.Clear
    }
}
