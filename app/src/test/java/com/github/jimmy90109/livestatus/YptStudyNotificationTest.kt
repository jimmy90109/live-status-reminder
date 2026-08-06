package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YptStudyNotificationTest {
    private val nowEpochMillis = 1_700_000_000_000L
    private val validSignals = YptStudySignals(
        channelId = YptStudyNotificationParser.STUDY_CHANNEL_ID,
        usesChronometer = true,
        chronometerCountsDown = false,
        startedAtEpochMillis = nowEpochMillis - 10_000L,
        sourceContentText = "YPT - Study Group",
    )

    @Test
    fun supportsOnlyYptPackage() {
        assertTrue(YptStudyNotificationParser.supportsPackage("com.pallo.passiontimerscoped"))
        assertEquals(false, YptStudyNotificationParser.supportsPackage("com.example.ypt"))
    }

    @Test
    fun parsesObservedCountUpStudyNotification() {
        val update = parse(validSignals)

        assertEquals("ypt|1001", update?.sourceKey)
        assertEquals(nowEpochMillis - 10_000L, update?.startedAtEpochMillis)
        assertEquals("YPT - Study Group", update?.sourceContentText)
    }

    @Test
    fun trimsSourceContentAndTreatsBlankAsMissing() {
        assertEquals(
            "YPT - Math",
            parse(validSignals.copy(sourceContentText = "  YPT - Math  "))?.sourceContentText,
        )
        assertNull(parse(validSignals.copy(sourceContentText = "  "))?.sourceContentText)
    }

    @Test
    fun ignoresOtherChannelsAndOrdinaryNotifications() {
        assertNull(parse(validSignals.copy(channelId = "GeneralNotification")))
        assertNull(parse(validSignals.copy(usesChronometer = false)))
    }

    @Test
    fun ignoresCountdownMissingFutureAndAlreadyPromotedSources() {
        assertNull(parse(validSignals.copy(chronometerCountsDown = true)))
        assertNull(parse(validSignals.copy(startedAtEpochMillis = 0L)))
        assertNull(parse(validSignals.copy(startedAtEpochMillis = nowEpochMillis + 1L)))
        assertNull(parse(validSignals.copy(sourceAlreadyPromoted = true)))
    }

    @Test
    fun trackerClearsOnlyCurrentSource() {
        val tracker = YptStudyTracker()
        val update = requireNotNull(parse(validSignals))

        assertTrue(tracker.onPosted(update.sourceKey, update) is YptStudyDecision.Show)
        assertEquals(YptStudyDecision.None, tracker.onRemoved("ypt|other"))
        assertEquals(YptStudyDecision.Clear, tracker.onRemoved(update.sourceKey))
        assertEquals(YptStudyDecision.None, tracker.onRemoved(update.sourceKey))
    }

    @Test
    fun trackerClearsWhenCurrentNotificationStopsMatching() {
        val tracker = YptStudyTracker()
        val update = requireNotNull(parse(validSignals))

        tracker.onPosted(update.sourceKey, update)

        assertEquals(YptStudyDecision.Clear, tracker.onPosted(update.sourceKey, null))
    }

    @Test
    fun trackerRestoresNewestActiveStudyOrClearsWhenNoneRemain() {
        val tracker = YptStudyTracker()
        val older = requireNotNull(parse(validSignals)).copy(sourceKey = "ypt|older")
        val newer = older.copy(
            sourceKey = "ypt|newer",
            startedAtEpochMillis = older.startedAtEpochMillis + 5_000L,
        )

        assertEquals(YptStudyDecision.Show(newer), tracker.restore(listOf(newer, older)))
        assertEquals(YptStudyDecision.None, tracker.onRemoved(older.sourceKey))
        assertEquals(YptStudyDecision.Clear, tracker.onRemoved(newer.sourceKey))
        assertEquals(YptStudyDecision.Clear, tracker.restore(emptyList()))
    }

    @Test
    fun epochStartConvertsToElapsedRealtimeBase() {
        assertEquals(
            40_000L,
            YptStudyNotificationStyle.elapsedRealtimeStartMillis(
                startedAtEpochMillis = nowEpochMillis - 10_000L,
                nowEpochMillis = nowEpochMillis,
                nowElapsedRealtimeMillis = 50_000L,
            ),
        )
    }

    private fun parse(signals: YptStudySignals): YptStudyUpdate? =
        YptStudyNotificationParser.parse(
            sourceKey = "ypt|1001",
            signals = signals,
            nowEpochMillis = nowEpochMillis,
        )
}
