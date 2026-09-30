package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TexpressArrivalNotificationTest {
    @Test
    fun parsesObservedArrivalReminder() {
        assertEquals(
            TexpressArrival("242", 5, "板橋站"),
            TexpressArrivalNotificationParser.parse(
                "T Express 列車到站提醒!",
                "您搭乘台灣高鐵242車次即將於5分鐘後抵達板橋站。",
            ),
        )
    }

    @Test
    fun parsesWhitespaceMultilineAndFullWidthPunctuationVariants() {
        assertEquals(
            TexpressArrival("1234", 12, "高鐵左營站"),
            TexpressArrivalNotificationParser.parse(
                " T-Express   列車到站提醒！ ",
                null,
                "其他內容\n您 搭乘 台灣高鐵 1234 車次 即將於 12 分鐘後抵達 高鐵左營站！",
            ),
        )
    }

    @Test
    fun rejectsUnrelatedOrIncompleteNotifications() {
        listOf(
            Triple(null, null, null),
            Triple("T Express 付款提醒", "請於期限內付款", null),
            Triple("T Express 列車到站提醒!", "台灣高鐵242車次即將抵達板橋站。", null),
            Triple("T Express 列車到站提醒!", "台灣高鐵242車次即將於0分鐘後抵達板橋站。", null),
            Triple("T Express 列車到站提醒!", "台灣高鐵242車次即將於5分鐘後抵達板橋。", null),
        ).forEach { (title, content, joined) ->
            assertNull(TexpressArrivalNotificationParser.parse(title, content, joined))
        }
    }

    @Test
    fun sourcePolicyRequiresExactPackageAndNonSummary() {
        assertTrue(
            TexpressArrivalNotificationParser.isEligible(
                TexpressArrivalNotificationParser.PACKAGE_NAME,
                isGroupSummary = false,
            ),
        )
        assertEquals(
            false,
            TexpressArrivalNotificationParser.isEligible("com.example.app", false),
        )
        assertEquals(
            false,
            TexpressArrivalNotificationParser.isEligible(
                TexpressArrivalNotificationParser.PACKAGE_NAME,
                isGroupSummary = true,
            ),
        )
    }

    @Test
    fun trackerRestoresPreviousArrivalWhenLatestIsRemoved() {
        val tracker = TexpressArrivalTracker()
        val older = update("older", 10, "242")
        val latest = update("latest", 20, "1234")

        assertEquals(TexpressArrivalDecision.Show(older), tracker.onPosted("older", older))
        assertEquals(TexpressArrivalDecision.Show(latest), tracker.onPosted("latest", latest))
        assertEquals(TexpressArrivalDecision.None, tracker.onRemoved("unknown"))
        assertEquals(TexpressArrivalDecision.Show(older), tracker.onRemoved("latest"))
        assertEquals(TexpressArrivalDecision.Clear, tracker.onRemoved("older"))
    }

    @Test
    fun trackerRestoreSelectsNewestAndResetClearsAll() {
        val tracker = TexpressArrivalTracker()
        val older = update("older", 10, "242")
        val latest = update("latest", 20, "1234")

        assertEquals(
            TexpressArrivalDecision.Show(latest),
            tracker.restore(listOf(latest, older)),
        )
        assertEquals(TexpressArrivalDecision.Clear, tracker.reset())
        assertEquals(TexpressArrivalDecision.None, tracker.onRemoved("latest"))
    }

    @Test
    fun olderSourceUpdateDoesNotReplaceNewestArrival() {
        val tracker = TexpressArrivalTracker()
        val older = update("older", 10, "242")
        val latest = update("latest", 20, "1234")
        tracker.restore(listOf(older, latest))

        assertEquals(
            TexpressArrivalDecision.Show(latest),
            tracker.onPosted("older", older.copy(arrival = older.arrival.copy(minutes = 3))),
        )
    }

    private fun update(key: String, postTime: Long, trainNumber: String) =
        TexpressArrivalUpdate(
            sourceKey = key,
            postTime = postTime,
            arrival = TexpressArrival(trainNumber, 5, "板橋站"),
        )
}
