package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClockTimerNotificationTest {
    private val nowEpochMillis = 1_700_000_000_000L
    private val nowElapsedRealtimeMillis = 50_000L

    @Test
    fun supportsOnlyGoogleClockPackage() {
        assertTrue(
            ClockTimerNotificationExtractor.supportsPackage("com.google.android.deskclock"),
        )
        assertEquals(
            false,
            ClockTimerNotificationExtractor.supportsPackage("com.sec.android.app.clockpackage"),
        )
    }

    @Test
    fun detectsChineseFromNotificationFieldsAndDefaultsToEnglish() {
        assertEquals(
            ClockTimerLanguage.CHINESE,
            ClockTimerLanguageDetector.detect(listOf("時鐘", "倒數計時器")),
        )
        assertEquals(
            ClockTimerLanguage.CHINESE,
            ClockTimerLanguageDetector.detect(listOf("Clock", "暂停")),
        )
        assertEquals(
            ClockTimerLanguage.ENGLISH,
            ClockTimerLanguageDetector.detect(listOf("Clock", "Pause", "Add 1 min")),
        )
        assertEquals(ClockTimerLanguage.ENGLISH, ClockTimerLanguageDetector.detect(emptyList()))
    }

    @Test
    fun runningMetricTimerUsesElapsedRealtimeEnd() {
        val update = interpret(
            ClockTimerSignals(
                metricIsTimer = true,
                metricEndElapsedRealtimeMillis = 1_370_000L,
                usesChronometer = true,
                chronometerCountsDown = true,
                notificationWhenMillis = nowEpochMillis + 60_000L,
            ),
        )

        assertEquals(ClockTimerState.RUNNING, update?.state)
        assertEquals(1_370_000L, update?.endElapsedRealtimeMillis)
        assertEquals(ClockTimerSource.METRIC_STYLE, update?.source)
    }

    @Test
    fun pausedMetricTimerKeepsFixedRemainingDuration() {
        val update = interpret(
            ClockTimerSignals(
                metricIsTimer = true,
                metricPausedMillis = 754_000L,
            ),
        )

        assertEquals(ClockTimerState.PAUSED, update?.state)
        assertEquals(754_000L, update?.remainingMillis)
        assertNull(update?.endElapsedRealtimeMillis)
    }

    @Test
    fun countdownChronometerConvertsWallClockEndToElapsedRealtime() {
        val update = interpret(
            ClockTimerSignals(
                usesChronometer = true,
                chronometerCountsDown = true,
                notificationWhenMillis = nowEpochMillis + 90_000L,
            ),
        )

        assertEquals(ClockTimerSource.CHRONOMETER_EXTRAS, update?.source)
        assertEquals(nowElapsedRealtimeMillis + 90_000L, update?.endElapsedRealtimeMillis)
    }

    @Test
    fun remoteViewsCountdownIsLastFallback() {
        val update = interpret(
            ClockTimerSignals(remoteChronometerBaseMillis = nowElapsedRealtimeMillis + 42_000L),
        )

        assertEquals(ClockTimerSource.REMOTE_VIEWS, update?.source)
        assertEquals(nowElapsedRealtimeMillis + 42_000L, update?.endElapsedRealtimeMillis)
    }

    @Test
    fun playSemanticActionFreezesRemoteViewsRemainingTime() {
        val update = interpret(
            ClockTimerSignals(
                remoteChronometerBaseMillis = nowElapsedRealtimeMillis + 754_000L,
                hasPlayAction = true,
            ),
        )

        assertEquals(ClockTimerState.PAUSED, update?.state)
        assertEquals(754_000L, update?.remainingMillis)
        assertEquals(ClockTimerSource.REMOTE_VIEWS, update?.source)
    }

    @Test
    fun invalidMetricDoesNotFallBackToTextOrChronometer() {
        assertNull(
            interpret(
                ClockTimerSignals(
                    metricIsTimer = true,
                    metricEndElapsedRealtimeMillis = nowElapsedRealtimeMillis,
                    usesChronometer = true,
                    chronometerCountsDown = true,
                    notificationWhenMillis = nowEpochMillis + 60_000L,
                ),
            ),
        )
    }

    @Test
    fun countUpAndMissingEndTimeAreIgnored() {
        assertNull(
            interpret(
                ClockTimerSignals(
                    usesChronometer = true,
                    chronometerCountsDown = false,
                    notificationWhenMillis = nowEpochMillis + 60_000L,
                ),
            ),
        )
        assertNull(
            interpret(
                ClockTimerSignals(
                    usesChronometer = true,
                    chronometerCountsDown = true,
                ),
            ),
        )
    }

    @Test
    fun addMinuteUpdateReplacesEndTime() {
        val before = interpret(
            ClockTimerSignals(
                metricIsTimer = true,
                metricEndElapsedRealtimeMillis = 1_370_000L,
            ),
        )
        val after = interpret(
            ClockTimerSignals(
                metricIsTimer = true,
                metricEndElapsedRealtimeMillis = 1_430_000L,
            ),
        )

        assertEquals(60_000L, after!!.endElapsedRealtimeMillis!! - before!!.endElapsedRealtimeMillis!!)
    }

    @Test
    fun trackerClearsOnlyActiveSourceNotification() {
        val tracker = ClockTimerTracker()
        val update = requireNotNull(
            interpret(
                ClockTimerSignals(
                    metricIsTimer = true,
                    metricEndElapsedRealtimeMillis = 1_370_000L,
                ),
            ),
        )

        assertTrue(tracker.onPosted("clock|timer", update) is ClockTimerDecision.Show)
        assertEquals(ClockTimerDecision.None, tracker.onRemoved("clock|alarm"))
        assertEquals(ClockTimerDecision.None, tracker.onPosted("clock|alarm", null))
        assertEquals(ClockTimerDecision.Clear, tracker.onRemoved("clock|timer"))
        assertEquals(ClockTimerDecision.None, tracker.onRemoved("clock|timer"))
    }

    @Test
    fun trackerClearsWhenActiveTimerBecomesInvalid() {
        val tracker = ClockTimerTracker()
        val update = requireNotNull(
            interpret(
                ClockTimerSignals(
                    metricIsTimer = true,
                    metricEndElapsedRealtimeMillis = 1_370_000L,
                ),
            ),
        )

        tracker.onPosted("clock|timer", update)

        assertEquals(ClockTimerDecision.Clear, tracker.onPosted("clock|timer", null))
    }

    @Test
    fun refreshTimingTargetsOnlyTheNextVisibleValueChange() {
        assertEquals(55_025L, ClockTimerRefreshTiming.nextDelayMillis(21 * 60_000L + 55_000L))
        assertEquals(60_025L, ClockTimerRefreshTiming.nextDelayMillis(2 * 60_000L))
        assertEquals(25L, ClockTimerRefreshTiming.nextDelayMillis(60_000L))
        assertEquals(26L, ClockTimerRefreshTiming.nextDelayMillis(59_001L))
        assertEquals(1_025L, ClockTimerRefreshTiming.nextDelayMillis(59_000L))
        assertNull(ClockTimerRefreshTiming.nextDelayMillis(0L))
    }

    @Test
    fun metricUsesChronometerUntilFinalMinuteThenSwitchesToFixedText() {
        val runningTimer = LiveStatusTimer(
            state = ClockTimerState.RUNNING,
            endElapsedRealtimeMillis = 170_000L,
        )
        val pausedTimer = LiveStatusTimer(
            state = ClockTimerState.PAUSED,
            remainingMillis = 120_000L,
        )

        assertEquals(false, ClockTimerMetricPresentation.usesFixedText(runningTimer, 50_000L))
        assertTrue(ClockTimerMetricPresentation.usesFixedText(runningTimer, 110_000L))
        assertTrue(ClockTimerMetricPresentation.usesFixedText(pausedTimer, 50_000L))
    }

    private fun interpret(signals: ClockTimerSignals): ClockTimerUpdate? =
        ClockTimerInterpreter.interpret(
            sourceKey = "clock|timer",
            signals = signals,
            nowEpochMillis = nowEpochMillis,
            nowElapsedRealtimeMillis = nowElapsedRealtimeMillis,
        )
}
