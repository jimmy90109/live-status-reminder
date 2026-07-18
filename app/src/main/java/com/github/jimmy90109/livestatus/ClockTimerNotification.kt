package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.os.SystemClock
import android.service.notification.StatusBarNotification
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import android.widget.RemoteViews
import androidx.annotation.RequiresApi
import java.time.Duration
import java.time.Instant

enum class ClockTimerState {
    RUNNING,
    PAUSED,
}

enum class ClockTimerSource {
    METRIC_STYLE,
    CHRONOMETER_EXTRAS,
    REMOTE_VIEWS,
}

enum class ClockTimerLanguage {
    CHINESE,
    ENGLISH,
}

data class ClockTimerUpdate(
    val sourceKey: String,
    val state: ClockTimerState,
    val endElapsedRealtimeMillis: Long? = null,
    val remainingMillis: Long? = null,
    val contentIntent: PendingIntent? = null,
    val source: ClockTimerSource,
    val language: ClockTimerLanguage = ClockTimerLanguage.ENGLISH,
) {
    init {
        require(
            (state == ClockTimerState.RUNNING && endElapsedRealtimeMillis != null && remainingMillis == null) ||
                (state == ClockTimerState.PAUSED && remainingMillis != null && endElapsedRealtimeMillis == null),
        ) { "Running timers need an end time; paused timers need a remaining duration." }
    }
}

internal data class ClockTimerSignals(
    val metricIsTimer: Boolean = false,
    val metricEndElapsedRealtimeMillis: Long? = null,
    val metricPausedMillis: Long? = null,
    val usesChronometer: Boolean = false,
    val chronometerCountsDown: Boolean = false,
    val notificationWhenMillis: Long = 0,
    val remoteChronometerBaseMillis: Long? = null,
    val hasPauseAction: Boolean = false,
    val hasPlayAction: Boolean = false,
)

internal data class ClockTimerExtraction(
    val update: ClockTimerUpdate?,
    val diagnostics: Map<String, String>,
)

internal object ClockTimerNotificationExtractor {
    const val CLOCK_PACKAGE = "com.google.android.deskclock"

    fun supportsPackage(packageName: String): Boolean = packageName == CLOCK_PACKAGE

    fun extract(
        context: Context,
        statusBarNotification: StatusBarNotification,
    ): ClockTimerExtraction {
        if (!supportsPackage(statusBarNotification.packageName)) {
            return ClockTimerExtraction(null, mapOf("reason" to "unsupported_package"))
        }

        val notification = statusBarNotification.notification
        val metricResult = if (Build.VERSION.SDK_INT >= 37) {
            ClockTimerApi37.readMetric(context, statusBarNotification.packageName, notification)
        } else {
            MetricReadResult(
                styleName = readStyleName(
                    context,
                    statusBarNotification.packageName,
                    notification,
                ),
            )
        }
        val remoteChronometer = readRemoteChronometer(
            context,
            statusBarNotification.packageName,
            notification,
        )
        val extras = notification.extras
        val signals = ClockTimerSignals(
            metricIsTimer = metricResult.isTimer,
            metricEndElapsedRealtimeMillis = metricResult.endElapsedRealtimeMillis,
            metricPausedMillis = metricResult.pausedMillis,
            usesChronometer = extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false),
            chronometerCountsDown = extras.getBoolean(
                Notification.EXTRA_CHRONOMETER_COUNT_DOWN,
                false,
            ),
            notificationWhenMillis = notification.`when`,
            remoteChronometerBaseMillis = remoteChronometer?.baseMillis,
            hasPauseAction = Build.VERSION.SDK_INT >= 37 && notification.actions?.any {
                it.semanticAction == Notification.Action.SEMANTIC_ACTION_PAUSE
            } == true,
            hasPlayAction = Build.VERSION.SDK_INT >= 37 && notification.actions?.any {
                it.semanticAction == Notification.Action.SEMANTIC_ACTION_PLAY
            } == true,
        )
        val language = ClockTimerLanguageDetector.detect(
            buildList {
                add(notification.tickerText)
                add(extras.getCharSequence(Notification.EXTRA_TITLE))
                add(extras.getCharSequence(Notification.EXTRA_TEXT))
                add(extras.getCharSequence(Notification.EXTRA_BIG_TEXT))
                add(extras.getCharSequence(Notification.EXTRA_SUB_TEXT))
                add(extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT))
                extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.let(::addAll)
                notification.actions?.map { it.title }?.let(::addAll)
            },
        )
        val update = ClockTimerInterpreter.interpret(
            sourceKey = statusBarNotification.key,
            signals = signals,
            nowEpochMillis = System.currentTimeMillis(),
            nowElapsedRealtimeMillis = SystemClock.elapsedRealtime(),
            contentIntent = notification.contentIntent,
            language = language,
        )
        return ClockTimerExtraction(
            update = update,
            diagnostics = linkedMapOf(
                "style" to metricResult.styleName,
                "metricIsTimer" to metricResult.isTimer.toString(),
                "metricEndElapsedRealtimeMillis" to
                    metricResult.endElapsedRealtimeMillis?.toString().orEmpty(),
                "metricPausedMillis" to metricResult.pausedMillis?.toString().orEmpty(),
                "usesChronometer" to signals.usesChronometer.toString(),
                "chronometerCountsDown" to signals.chronometerCountsDown.toString(),
                "notificationWhenMillis" to signals.notificationWhenMillis.toString(),
                "remoteChronometerBaseMillis" to
                    remoteChronometer?.baseMillis?.toString().orEmpty(),
                "remoteChronometerCountsDown" to
                    remoteChronometer?.countsDown?.toString().orEmpty(),
                "hasPauseAction" to signals.hasPauseAction.toString(),
                "hasPlayAction" to signals.hasPlayAction.toString(),
                "selectedSource" to update?.source?.name.orEmpty(),
                "language" to language.name,
            ),
        )
    }

    private fun readRemoteChronometer(
        context: Context,
        packageName: String,
        notification: Notification,
    ): RemoteChronometer? {
        val packageContext = runCatching { context.createPackageContext(packageName, 0) }.getOrNull()
        val contexts = listOfNotNull(packageContext, context).distinct()
        return contexts.firstNotNullOfOrNull { remoteViewContext ->
            notification.remoteViews().firstNotNullOfOrNull { remoteViews ->
                runCatching {
                    remoteViews.apply(remoteViewContext, null).findCountdownChronometer()
                }.getOrNull()
            }
        }
    }

    private fun readStyleName(
        context: Context,
        packageName: String,
        notification: Notification,
    ): String {
        val packageContext = runCatching { context.createPackageContext(packageName, 0) }
            .getOrDefault(context)
        return runCatching {
            Notification.Builder.recoverBuilder(packageContext, notification)
                .style
                ?.javaClass
                ?.name
                .orEmpty()
        }.getOrDefault("")
    }

    private fun Notification.remoteViews(): List<RemoteViews> =
        listOfNotNull(contentView, bigContentView, headsUpContentView)

    private fun View.findCountdownChronometer(): RemoteChronometer? = when (this) {
        is Chronometer -> RemoteChronometer(getBase(), isCountDown)
            .takeIf { it.countsDown }
        is ViewGroup -> (0 until childCount).firstNotNullOfOrNull { index ->
            getChildAt(index).findCountdownChronometer()
        }
        else -> null
    }
}

internal object ClockTimerInterpreter {
    fun interpret(
        sourceKey: String,
        signals: ClockTimerSignals,
        nowEpochMillis: Long,
        nowElapsedRealtimeMillis: Long,
        contentIntent: PendingIntent? = null,
        language: ClockTimerLanguage = ClockTimerLanguage.ENGLISH,
    ): ClockTimerUpdate? {
        if (signals.metricIsTimer) {
            signals.metricPausedMillis?.takeIf { it > 0 }?.let { remainingMillis ->
                return ClockTimerUpdate(
                    sourceKey = sourceKey,
                    state = ClockTimerState.PAUSED,
                    remainingMillis = remainingMillis,
                    contentIntent = contentIntent,
                    source = ClockTimerSource.METRIC_STYLE,
                    language = language,
                )
            }
            signals.metricEndElapsedRealtimeMillis
                ?.takeIf { it > nowElapsedRealtimeMillis }
                ?.let { endElapsedRealtimeMillis ->
                    return ClockTimerUpdate(
                        sourceKey = sourceKey,
                        state = ClockTimerState.RUNNING,
                        endElapsedRealtimeMillis = endElapsedRealtimeMillis,
                        contentIntent = contentIntent,
                        source = ClockTimerSource.METRIC_STYLE,
                        language = language,
                    )
                }
            return null
        }

        if (signals.hasPlayAction) {
            val chronometerRemainingMillis = if (
                signals.usesChronometer && signals.chronometerCountsDown
            ) {
                (signals.notificationWhenMillis - nowEpochMillis).takeIf { it > 0 }
            } else {
                null
            }
            val remoteRemainingMillis = signals.remoteChronometerBaseMillis
                ?.minus(nowElapsedRealtimeMillis)
                ?.takeIf { it > 0 }
            val remainingMillis = chronometerRemainingMillis ?: remoteRemainingMillis
            if (remainingMillis != null) {
                return ClockTimerUpdate(
                    sourceKey = sourceKey,
                    state = ClockTimerState.PAUSED,
                    remainingMillis = remainingMillis,
                    contentIntent = contentIntent,
                    source = if (chronometerRemainingMillis != null) {
                        ClockTimerSource.CHRONOMETER_EXTRAS
                    } else {
                        ClockTimerSource.REMOTE_VIEWS
                    },
                    language = language,
                )
            }
        }

        if (signals.usesChronometer && signals.chronometerCountsDown) {
            val remainingMillis = signals.notificationWhenMillis - nowEpochMillis
            if (remainingMillis > 0) {
                return ClockTimerUpdate(
                    sourceKey = sourceKey,
                    state = ClockTimerState.RUNNING,
                    endElapsedRealtimeMillis = nowElapsedRealtimeMillis + remainingMillis,
                    contentIntent = contentIntent,
                    source = ClockTimerSource.CHRONOMETER_EXTRAS,
                    language = language,
                )
            }
            return null
        }

        return signals.remoteChronometerBaseMillis
            ?.takeIf { it > nowElapsedRealtimeMillis }
            ?.let { endElapsedRealtimeMillis ->
                ClockTimerUpdate(
                    sourceKey = sourceKey,
                    state = ClockTimerState.RUNNING,
                    endElapsedRealtimeMillis = endElapsedRealtimeMillis,
                    contentIntent = contentIntent,
                    source = ClockTimerSource.REMOTE_VIEWS,
                    language = language,
                )
            }
    }
}

internal object ClockTimerLanguageDetector {
    private val chineseCharacter = Regex("[\\u3400-\\u4DBF\\u4E00-\\u9FFF]")

    fun detect(values: Iterable<CharSequence?>): ClockTimerLanguage =
        if (values.any { value -> chineseCharacter.containsMatchIn(value?.toString().orEmpty()) }) {
            ClockTimerLanguage.CHINESE
        } else {
            ClockTimerLanguage.ENGLISH
        }
}

internal object ClockTimerRefreshTiming {
    private const val REFRESH_GUARD_MILLIS = 25L

    fun nextDelayMillis(remainingMillis: Long): Long? {
        if (remainingMillis <= 0) return null
        if (remainingMillis == 60_000L) return REFRESH_GUARD_MILLIS

        val unitMillis = if (remainingMillis > 60_000L) 60_000L else 1_000L
        val displayedUnits = (remainingMillis + unitMillis - 1) / unitMillis
        val untilNextValue = remainingMillis - (displayedUnits - 1) * unitMillis
        return untilNextValue.coerceAtLeast(1L) + REFRESH_GUARD_MILLIS
    }
}

internal sealed interface ClockTimerDecision {
    data class Show(val update: ClockTimerUpdate) : ClockTimerDecision
    data object Clear : ClockTimerDecision
    data object None : ClockTimerDecision
}

internal class ClockTimerTracker {
    private var activeSourceKey: String? = null

    fun onPosted(sourceKey: String, update: ClockTimerUpdate?): ClockTimerDecision {
        if (update != null) {
            activeSourceKey = sourceKey
            return ClockTimerDecision.Show(update)
        }
        if (activeSourceKey == sourceKey) {
            activeSourceKey = null
            return ClockTimerDecision.Clear
        }
        return ClockTimerDecision.None
    }

    fun onRemoved(sourceKey: String): ClockTimerDecision {
        if (activeSourceKey != sourceKey) return ClockTimerDecision.None
        activeSourceKey = null
        return ClockTimerDecision.Clear
    }

    fun reset(): ClockTimerDecision {
        val hadActiveTimer = activeSourceKey != null
        activeSourceKey = null
        return if (hadActiveTimer) ClockTimerDecision.Clear else ClockTimerDecision.None
    }
}

internal object ClockTimerNotificationStyle {
    fun apply(builder: Notification.Builder, timer: LiveStatusTimer) {
        if (Build.VERSION.SDK_INT >= 37) {
            ClockTimerStyleApi37.apply(builder, timer)
            return
        }
        when (timer.state) {
            ClockTimerState.RUNNING -> {
                val endElapsedRealtimeMillis = timer.endElapsedRealtimeMillis ?: return
                val remainingMillis = endElapsedRealtimeMillis - SystemClock.elapsedRealtime()
                builder
                    .setWhen(System.currentTimeMillis() + remainingMillis.coerceAtLeast(0))
                    .setShowWhen(true)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
            }
            ClockTimerState.PAUSED -> builder.setStyle(
                Notification.BigTextStyle().bigText(
                    if (timer.language == ClockTimerLanguage.CHINESE) {
                        "倒數已暫停"
                    } else {
                        "Timer paused"
                    },
                ),
            )
        }
    }
}

private data class MetricReadResult(
    val styleName: String = "",
    val isTimer: Boolean = false,
    val endElapsedRealtimeMillis: Long? = null,
    val pausedMillis: Long? = null,
)

private data class RemoteChronometer(
    val baseMillis: Long,
    val countsDown: Boolean,
)

@RequiresApi(37)
private object ClockTimerApi37 {
    fun readMetric(
        context: Context,
        packageName: String,
        notification: Notification,
    ): MetricReadResult {
        val packageContext = runCatching { context.createPackageContext(packageName, 0) }
            .getOrDefault(context)
        val style = runCatching {
            Notification.Builder.recoverBuilder(packageContext, notification).style
        }.getOrNull()
        if (style !is Notification.MetricStyle) {
            return MetricReadResult(styleName = style?.javaClass?.name.orEmpty())
        }
        val metric = style.criticalMetric ?: style.metrics.firstOrNull { candidate ->
            (candidate.value as? Notification.Metric.TimeDifference)?.isTimer == true
        }
        val timeDifference = metric?.value as? Notification.Metric.TimeDifference
            ?: return MetricReadResult(styleName = style.javaClass.name)
        if (!timeDifference.isTimer) {
            return MetricReadResult(styleName = style.javaClass.name)
        }

        val pausedMillis = timeDifference.pausedDuration?.toMillis()
        val endElapsedRealtimeMillis = timeDifference.zeroElapsedRealtime
            ?: timeDifference.zeroTime?.toElapsedRealtimeMillis()
        return MetricReadResult(
            styleName = style.javaClass.name,
            isTimer = true,
            endElapsedRealtimeMillis = endElapsedRealtimeMillis,
            pausedMillis = pausedMillis,
        )
    }

    private fun Instant.toElapsedRealtimeMillis(): Long =
        SystemClock.elapsedRealtime() + Duration.between(Instant.now(), this).toMillis()
}

@RequiresApi(37)
private object ClockTimerStyleApi37 {
    fun apply(builder: Notification.Builder, timer: LiveStatusTimer) {
        val timeDifference = when (timer.state) {
            ClockTimerState.RUNNING -> Notification.Metric.TimeDifference.forTimer(
                requireNotNull(timer.endElapsedRealtimeMillis),
                Notification.Metric.TimeDifference.FORMAT_CHRONOMETER,
            )
            ClockTimerState.PAUSED -> Notification.Metric.TimeDifference.forPausedTimer(
                Duration.ofMillis(requireNotNull(timer.remainingMillis)),
                Notification.Metric.TimeDifference.FORMAT_CHRONOMETER,
            )
        }
        val metricLabel = if (timer.language == ClockTimerLanguage.CHINESE) {
            "剩餘時間"
        } else {
            "Time remaining"
        }
        val style = Notification.MetricStyle()
            .addMetric(Notification.Metric(timeDifference, metricLabel))
            .setCriticalMetric(0)
        builder.setStyle(style)
    }
}
