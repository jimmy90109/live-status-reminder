package com.github.jimmy90109.livestatus

import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class YouBikeNotificationParserTest {
    @Test
    fun parsesBorrowPayloadWithoutReturningPaymentIdentifier() {
        val update = YouBikeNotificationParser.parse(
            "借車成功！您於2026-08-01 18:17:08在龍江錦州街口 09車柱,使用掃碼-信用卡(20) 90246698476666654321，租借車號0102751。",
        )

        assertEquals(YouBikeEvent.BORROWED, update.event)
        assertEquals(LocalDateTime.of(2026, 8, 1, 18, 17, 8), update.occurredAt)
        assertEquals("龍江錦州街口", update.stationName)
        assertEquals("09", update.dockNumber)
        assertEquals("0102751", update.bikeNumber)
        assertNull(update.chargedAmount)
        assertFalse(update.toString().contains("90246698476666654321"))
    }

    @Test
    fun parsesReturnBeforeDuplicatedBorrowFallback() {
        val returned = "還車扣款成功！您於2026-08-01 18:22:56在五常國中 23車柱,使用掃碼-信用卡(20) 90246698476666654321，歸還車號0102751，並已扣款0元。"
        val update = YouBikeNotificationParser.parse(returned, "騎乘通知\n$returned\n借車成功")

        assertEquals(YouBikeEvent.RETURNED, update.event)
        assertEquals("五常國中", update.stationName)
        assertEquals("23", update.dockNumber)
        assertEquals("0102751", update.bikeNumber)
        assertEquals(0, update.chargedAmount)
    }

    @Test
    fun toleratesNewlinesFullWidthPunctuationAndWhitespace() {
        val update = YouBikeNotificationParser.parse(
            "借車成功！\n您於 2026-08-01 18:17:08 在 龍江錦州街口 09車柱，\n使用掃碼，租借車號 0102751。",
        )
        assertEquals(YouBikeEvent.BORROWED, update.event)
        assertEquals("龍江錦州街口", update.stationName)
    }

    @Test
    fun ignoresBlankMarketingAndKeywordOnlyText() {
        assertEquals(YouBikeEvent.NONE, YouBikeNotificationParser.parse(null).event)
        assertEquals(YouBikeEvent.NONE, YouBikeNotificationParser.parse("  ").event)
        assertEquals(YouBikeEvent.NONE, YouBikeNotificationParser.parse("YouBike 每月騎 11 天抽獎").event)
        assertEquals(YouBikeEvent.NONE, YouBikeNotificationParser.parse("借車成功").event)
        assertEquals(YouBikeEvent.NONE, YouBikeNotificationParser.parse("還車扣款成功").event)
        assertEquals(
            YouBikeEvent.NONE,
            YouBikeNotificationParser.parse(
                "借車成功！您於2026-99-99 18:17:08在龍江錦州街口 09車柱,租借車號0102751。",
            ).event,
        )
        assertEquals(
            YouBikeEvent.NONE,
            YouBikeNotificationParser.parse(
                "還車扣款成功！您於2026-08-01 18:22:56在五常國中 23車柱,歸還車號0102751。",
            ).event,
        )
    }
}
