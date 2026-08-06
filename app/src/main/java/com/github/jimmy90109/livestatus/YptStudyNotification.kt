package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.os.Build
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import androidx.annotation.RequiresApi

internal data class YptStudyUpdate(
    val sourceKey: String,
    val startedAtEpochMillis: Long,
    val sourceContentText: String?,
    val contentIntent: PendingIntent? = null,
)

internal data class YptStudySignals(
    val channelId: String?,
    val usesChronometer: Boolean,
    val chronometerCountsDown: Boolean,
    val startedAtEpochMillis: Long,
    val sourceContentText: String?,
    val sourceAlreadyPromoted: Boolean = false,
)

internal data class YptStudyExtraction(
    val update: YptStudyUpdate?,
    val diagnostics: Map<String, String>,
)

internal object YptStudyNotificationParser {
    const val PACKAGE_NAME = "com.pallo.passiontimerscoped"
    const val STUDY_CHANNEL_ID = "PassionStudyNotification"

    fun supportsPackage(packageName: String): Boolean = packageName == PACKAGE_NAME

    fun parse(
        sourceKey: String,
        signals: YptStudySignals,
        nowEpochMillis: Long,
        contentIntent: PendingIntent? = null,
    ): YptStudyUpdate? {
        if (signals.sourceAlreadyPromoted) return null
        if (signals.channelId != STUDY_CHANNEL_ID) return null
        if (!signals.usesChronometer || signals.chronometerCountsDown) return null
        if (signals.startedAtEpochMillis !in 1..nowEpochMillis) return null

        return YptStudyUpdate(
            sourceKey = sourceKey,
            startedAtEpochMillis = signals.startedAtEpochMillis,
            sourceContentText = signals.sourceContentText?.trim()?.takeIf(String::isNotEmpty),
            contentIntent = contentIntent,
        )
    }
}

internal object YptStudyNotificationExtractor {
    fun extract(statusBarNotification: StatusBarNotification): YptStudyExtraction {
        if (!YptStudyNotificationParser.supportsPackage(statusBarNotification.packageName)) {
            return YptStudyExtraction(null, mapOf("reason" to "unsupported_package"))
        }

        val notification = statusBarNotification.notification
        val promotionState = ClockTimerPromotionPolicy.evaluate(notification)
        val signals = YptStudySignals(
            channelId = notification.channelId,
            usesChronometer = notification.extras.getBoolean(
                Notification.EXTRA_SHOW_CHRONOMETER,
                false,
            ),
            chronometerCountsDown = notification.extras.getBoolean(
                Notification.EXTRA_CHRONOMETER_COUNT_DOWN,
                false,
            ),
            startedAtEpochMillis = notification.`when`,
            sourceContentText = notification.extras
                .getCharSequence(Notification.EXTRA_TEXT)
                ?.toString(),
            sourceAlreadyPromoted = promotionState.promoted,
        )
        val update = YptStudyNotificationParser.parse(
            sourceKey = statusBarNotification.key,
            signals = signals,
            nowEpochMillis = System.currentTimeMillis(),
            contentIntent = notification.contentIntent,
        )
        return YptStudyExtraction(
            update = update,
            diagnostics = linkedMapOf(
                "channelId" to signals.channelId.orEmpty(),
                "usesChronometer" to signals.usesChronometer.toString(),
                "chronometerCountsDown" to signals.chronometerCountsDown.toString(),
                "startedAtEpochMillis" to signals.startedAtEpochMillis.toString(),
                "sourceRequestedPromotion" to promotionState.requested.toString(),
                "sourcePromotedOngoing" to promotionState.promoted.toString(),
            ),
        )
    }
}

internal sealed interface YptStudyDecision {
    data class Show(val update: YptStudyUpdate) : YptStudyDecision
    data object Clear : YptStudyDecision
    data object None : YptStudyDecision
}

internal class YptStudyTracker {
    private var activeSourceKey: String? = null

    fun onPosted(sourceKey: String, update: YptStudyUpdate?): YptStudyDecision {
        if (update != null) {
            activeSourceKey = sourceKey
            return YptStudyDecision.Show(update)
        }
        if (activeSourceKey == sourceKey) {
            activeSourceKey = null
            return YptStudyDecision.Clear
        }
        return YptStudyDecision.None
    }

    fun onRemoved(sourceKey: String): YptStudyDecision {
        if (activeSourceKey != sourceKey) return YptStudyDecision.None
        activeSourceKey = null
        return YptStudyDecision.Clear
    }

    fun restore(updates: List<YptStudyUpdate>): YptStudyDecision {
        val update = updates.maxByOrNull(YptStudyUpdate::startedAtEpochMillis)
        activeSourceKey = update?.sourceKey
        return if (update == null) YptStudyDecision.Clear else YptStudyDecision.Show(update)
    }

    fun reset(): YptStudyDecision {
        val hadActiveStudy = activeSourceKey != null
        activeSourceKey = null
        return if (hadActiveStudy) YptStudyDecision.Clear else YptStudyDecision.None
    }
}

internal object YptStudyNotificationStyle {
    fun apply(
        builder: Notification.Builder,
        startedAtEpochMillis: Long,
        metricLabel: String,
        nowEpochMillis: Long = System.currentTimeMillis(),
        nowElapsedRealtimeMillis: Long = SystemClock.elapsedRealtime(),
    ) {
        if (Build.VERSION.SDK_INT >= 37) {
            YptStudyStyleApi37.apply(
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
private object YptStudyStyleApi37 {
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
