package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TaipeiMetroGoNotificationTest {
    private val observedText = """
        台北捷運 Go - 運行中
        🔔 行經：中山國中
        市政府 - 大直
        18 分鐘
        於 21:16 從市政府發車 經 7 站
        板南線
        ›
        文湖線
    """.trimIndent()

    @Test
    fun parsesObservedRunningNotification() {
        assertEquals(
            TaipeiMetroGoRoute("市政府", "大直", "中山國中"),
            TaipeiMetroGoNotificationParser.parse(
                notificationTitle = "台北捷運 Go - 運行中",
                notificationContentText = "🔔 行經：中山國中",
                notificationText = observedText,
            ),
        )
    }

    @Test
    fun acceptsNormalizedSpacingAndCurrentStationFromJoinedText() {
        assertEquals(
            TaipeiMetroGoRoute("市政府", "大直", "中山國中"),
            TaipeiMetroGoNotificationParser.parse(
                notificationTitle = "  台北捷運   Go - 運行中 ",
                notificationContentText = null,
                notificationText = "🔔  行經：  中山國中\n市政府   -   大直",
            ),
        )
    }

    @Test
    fun rejectsMissingOrUnrelatedNotificationText() {
        val cases = listOf(
            Triple(null, "🔔 行經：中山國中", observedText),
            Triple("台北捷運 Go", "🔔 行經：中山國中", observedText),
            Triple("台北捷運 Go - 運行中", null, "市政府 - 大直"),
            Triple("台北捷運 Go - 運行中", "🔔 行經：中山國中", "18 分鐘"),
            Triple("台北捷運 Go - 運行中", "即將抵達中山國中", "市政府 - 大直"),
        )
        cases.forEach { (title, content, text) ->
            assertNull(TaipeiMetroGoNotificationParser.parse(title, content, text))
        }
    }

    @Test
    fun eligibilityRequiresObservedSourceShape() {
        assertTrue(eligible())
        assertEquals(false, eligible(packageName = "example.other"))
        assertEquals(false, eligible(channelId = "other"))
        assertEquals(false, eligible(isOngoing = false))
        assertEquals(false, eligible(isForegroundService = false))
        assertEquals(false, eligible(isGroupSummary = true))
    }

    @Test
    fun trackerUpdatesAndClearsOnlyTheActiveSource() {
        val tracker = TaipeiMetroGoTracker()
        val first = update("trip", 1, "中山國中")
        val second = update("trip", 2, "松山機場")

        assertEquals(TaipeiMetroGoDecision.Show(first), tracker.onPosted("trip", first))
        assertEquals(TaipeiMetroGoDecision.None, tracker.onRemoved("other"))
        assertEquals(TaipeiMetroGoDecision.Show(second), tracker.onPosted("trip", second))
        assertEquals(TaipeiMetroGoDecision.Clear, tracker.onPosted("trip", null))
        assertEquals(TaipeiMetroGoDecision.None, tracker.onRemoved("trip"))
    }

    @Test
    fun trackerRestoresLatestValidUpdate() {
        val latest = update("latest", 20, "大直")
        val decision = TaipeiMetroGoTracker().restore(
            listOf(update("older", 10, "中山國中"), latest),
        )

        assertEquals(TaipeiMetroGoDecision.Show(latest), decision)
        assertEquals(TaipeiMetroGoDecision.Clear, TaipeiMetroGoTracker().restore(emptyList()))
    }

    @Test
    fun payloadShowsRouteCurrentStationAndWidthLimitedCriticalText() {
        val payload = LiveStatusReminder.taipeiMetroGoPayload(
            update = update("trip", 1, "新北產業園區"),
            appName = "台北捷運 GO",
            title = "市政府 → 大直",
            contentText = "目前行經：新北產業園區",
        )

        assertEquals("市政府 → 大直", payload.title)
        assertEquals("目前行經：新北產業園區", payload.contentText)
        assertEquals("新北產業", payload.criticalText)
    }

    @Test
    fun sharedFormatterPreservesUnicodeGraphemes() {
        val familyEmoji = "👨‍👩‍👧‍👦"
        assertEquals("中山國中", ShortCriticalTextFormatter.format("中山國中"))
        assertEquals("ABC中文", ShortCriticalTextFormatter.format("ABC中文歌曲"))
        assertEquals(
            familyEmoji + "abcdef",
            ShortCriticalTextFormatter.format(familyEmoji + "abcdefgh"),
        )
    }

    private fun eligible(
        packageName: String? = TaipeiMetroGoNotificationParser.PACKAGE_NAME,
        channelId: String? = TaipeiMetroGoNotificationParser.CHANNEL_ID,
        isOngoing: Boolean = true,
        isForegroundService: Boolean = true,
        isGroupSummary: Boolean = false,
    ) = TaipeiMetroGoNotificationParser.isEligible(
        packageName,
        channelId,
        isOngoing,
        isForegroundService,
        isGroupSummary,
    )

    private fun update(sourceKey: String, postTime: Long, station: String) =
        TaipeiMetroGoUpdate(
            sourceKey = sourceKey,
            postTime = postTime,
            origin = "市政府",
            destination = "大直",
            currentStation = station,
        )
}
