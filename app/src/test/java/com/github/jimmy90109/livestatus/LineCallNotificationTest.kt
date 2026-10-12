package com.github.jimmy90109.livestatus

import org.junit.Assert.*
import org.junit.Test

class LineCallNotificationTest {
    private val signals = LineCallSignals(
        packageName = "jp.naver.line.android",
        sourceKey = "line-call-1",
        channelId = "jp.naver.line.android.notification.VoIP.02.Ongoing",
        category = "call",
        template = "android.app.Notification\$CallStyle",
        callType = 2,
        isVideo = false,
        isOngoing = true,
        isForegroundService = true,
        isNoClear = true,
        isPromoted = false,
        sourceTitle = " 測試 聯絡人 ",
        sourceContentText = "群組通話\n(測試 聯絡人)",
        postedAtEpochMillis = 1000L,
        sourceWhenEpochMillis = 992L,
    )

    @Test
    fun parsesObservedVoiceCallAndPreservesSourceStartTime() {
        val update = requireNotNull(LineCallNotificationParser.parse(signals))
        assertEquals("測試 聯絡人", update.sourceTitle)
        assertEquals("群組通話 (測試 聯絡人)", update.sourceContentText)
        assertEquals(1000L, update.postedAtEpochMillis)
        assertEquals(992L, update.sourceWhenEpochMillis)
        val blank = requireNotNull(LineCallNotificationParser.parse(signals.copy(sourceTitle = null, sourceContentText = " \n ")))
        assertNull(blank.sourceTitle)
        assertNull(blank.sourceContentText)
    }

    @Test
    fun rejectsMessagesIncomingScreeningVideoAndMissingStructure() {
        val rejected = listOf(
            signals.copy(packageName = "com.other.app"),
            signals.copy(sourceKey = "  "),
            signals.copy(channelId = "messages", sourceContentText = "群組通話"),
            signals.copy(channelId = null),
            signals.copy(channelId = signals.channelId + ".other"),
            signals.copy(category = "msg"),
            signals.copy(category = null),
            signals.copy(template = null),
            signals.copy(template = "android.app.Notification\$BigTextStyle"),
            signals.copy(callType = null),
            signals.copy(callType = 1),
            signals.copy(callType = 3),
            signals.copy(callType = 0),
            signals.copy(isVideo = true),
            signals.copy(isVideo = null),
            signals.copy(isOngoing = false),
            signals.copy(isForegroundService = false),
            signals.copy(isNoClear = false),
            signals.copy(isPromoted = true),
        )
        rejected.forEach { assertNull(it.toString(), LineCallNotificationParser.parse(it)) }
    }

    @Test
    fun enablesOnlyExplicitlyEnabledXiaomiFamilyDevices() {
        for (brand in listOf("xiaomi", "Redmi", " POCO ")) {
            assertTrue(LineCallPolicy.shouldMirror(brand, null, true))
            assertTrue(LineCallPolicy.shouldMirror(null, brand, true))
            assertFalse(LineCallPolicy.shouldMirror(brand, brand, false))
        }
        for (brand in listOf(null, "", "Google", "Samsung", "xiaomi-other")) {
            assertFalse(LineCallPolicy.shouldMirror(brand, brand, true))
        }
    }

    @Test
    fun debugAllowsEveryBrandButDoesNotBypassTheUserSwitch() {
        for (brand in listOf(null, "", "Google", "Samsung", "Xiaomi")) {
            assertTrue(LineCallPolicy.supportsDevice(brand, brand, debugBuild = true))
            assertTrue(LineCallPolicy.shouldMirror(brand, brand, enabled = true, debugBuild = true))
            assertFalse(LineCallPolicy.shouldMirror(brand, brand, enabled = false, debugBuild = true))
        }
    }

    @Test
    fun clearsRemovedActiveSourceEvenWithOldOngoingFieldsAndIgnoresOtherKeys() {
        val tracker = LineCallTracker()
        val update = requireNotNull(LineCallNotificationParser.parse(signals))
        assertEquals(LineCallDecision.Show(update), tracker.onPosted(update.sourceKey, update))
        assertEquals(LineCallDecision.None, tracker.onRemoved("unrelated"))
        // REMOVED goes straight to the tracker; it never re-parses the stale CallStyle fields.
        assertEquals(LineCallDecision.Clear, tracker.onRemoved(update.sourceKey))
        assertEquals(LineCallDecision.None, tracker.onRemoved(update.sourceKey))
    }

    @Test
    fun clearsWhenCurrentSourceStopsMatchingButOldSourceCannotClearReplacement() {
        val tracker = LineCallTracker()
        val first = requireNotNull(LineCallNotificationParser.parse(signals))
        tracker.onPosted(first.sourceKey, first)
        assertEquals(LineCallDecision.None, tracker.onPosted("chat", null))
        assertEquals(LineCallDecision.Clear, tracker.onPosted(first.sourceKey, null))
        tracker.onPosted(first.sourceKey, first)
        val newer = first.copy(sourceKey = "line-call-2", postedAtEpochMillis = 2000)
        tracker.onPosted(newer.sourceKey, newer)
        assertEquals(LineCallDecision.None, tracker.onRemoved(first.sourceKey))
        assertEquals(LineCallDecision.Clear, tracker.onRemoved(newer.sourceKey))
    }

    @Test
    fun restoresLatestNotificationAndResetDropsTrackedData() {
        val tracker = LineCallTracker()
        val older = requireNotNull(LineCallNotificationParser.parse(signals))
        val newer = older.copy(sourceKey = "newer", postedAtEpochMillis = 2000)
        assertEquals(LineCallDecision.Show(newer), tracker.restore(listOf(newer, older)))
        assertEquals(LineCallDecision.None, tracker.onRemoved(older.sourceKey))
        assertEquals(LineCallDecision.Clear, tracker.reset())
        assertEquals(LineCallDecision.None, tracker.onRemoved(newer.sourceKey))
        tracker.restore(listOf(older))
        assertEquals(LineCallDecision.Clear, tracker.restore(emptyList()))
        assertEquals(LineCallDecision.None, tracker.onRemoved(older.sourceKey))
    }
}
