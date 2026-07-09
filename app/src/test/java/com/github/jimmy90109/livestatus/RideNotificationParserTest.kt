package com.github.jimmy90109.livestatus

import com.github.jimmy90109.livestatus.RideNotificationParser.FoodpandaEvent
import com.github.jimmy90109.livestatus.RideNotificationParser.RideEvent
import com.github.jimmy90109.livestatus.RideNotificationParser.UberEatsEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RideNotificationParserTest {
    @Test
    fun entryNotificationStartsReminder() {
        assertEquals(
            RideEvent.ENTERED,
            RideNotificationParser.parse(
                "乘車碼交易\nNT$0\n商店名稱：臺北捷運\n中山國中2026-05-15 14:54:08 > [尚未出站]\n進站交易已完成",
            ),
        )
    }

    @Test
    fun exitNotificationClearsReminder() {
        assertEquals(
            RideEvent.EXITED,
            RideNotificationParser.parse(
                "乘車碼交易\nNT$25\n商店名稱：臺北捷運\n中山國中 > 西湖\n出站交易已完成，謝謝您使用iPASS MONEY。",
            ),
        )
    }

    @Test
    fun unrelatedNotificationsAreIgnored() {
        assertEquals(RideEvent.NONE, RideNotificationParser.parse("付款完成"))
    }

    @Test
    fun foodpandaCourierOnTheWayUpdatesReminder() {
        assertEquals(
            FoodpandaEvent.COURIER_ON_THE_WAY,
            RideNotificationParser.parseFoodpanda(
                "foodpanda\n外送夥伴在路上囉🚴，請隨時留意手機來電或訊息！",
            ),
        )
    }

    @Test
    fun foodpandaCourierArrivingUpdatesReminder() {
        assertEquals(
            FoodpandaEvent.COURIER_ARRIVING,
            RideNotificationParser.parseFoodpanda("foodpanda\n我們的外送夥伴即將抵達💪！"),
        )
    }

    @Test
    fun foodpandaEndedNotificationsClearReminder() {
        listOf(
            "foodpanda\n您的訂單已送達",
            "foodpanda\n訂單完成，祝您用餐愉快！",
            "foodpanda\n您的訂單已取消",
            "foodpanda\n訂單取消",
        ).forEach {
            assertEquals(
                FoodpandaEvent.ORDER_ENDED,
                RideNotificationParser.parseFoodpanda(it),
            )
        }
    }

    @Test
    fun unrelatedFoodpandaNotificationsAreIgnored() {
        assertEquals(
            FoodpandaEvent.NONE,
            RideNotificationParser.parseFoodpanda("foodpanda\n優惠快訊"),
        )
    }

    @Test
    fun uberEatsParsesAllFiveProgressStages() {
        assertUberEatsEvent(UberEatsEvent.ORDER_RECEIVED, "Uber Eats\n訂單已收到")
        assertUberEatsEvent(UberEatsEvent.PREPARING, "Uber Eats\n正在準備訂單")
        assertUberEatsEvent(UberEatsEvent.PICKING_UP, "Uber Eats\n正在取餐")
        assertUberEatsEvent(UberEatsEvent.ON_THE_WAY, "Uber Eats\n正前往您所在位置")
        assertUberEatsEvent(UberEatsEvent.ARRIVING, "Uber Eats\n快到了！")
    }

    @Test
    fun uberEatsEndedNotificationsEndTheJourney() {
        assertUberEatsEvent(UberEatsEvent.ORDER_ENDED, "Uber Eats\n訂單已送達")
        assertUberEatsEvent(UberEatsEvent.ORDER_ENDED, "Uber Eats\n您的訂單已取消")
    }

    @Test
    fun uberEatsReadsExactShortCriticalPin() {
        val update = RideNotificationParser.parseUberEats("Uber Eats\n快到了！", "7616")
        assertEquals("7616", update.pin)
    }

    @Test
    fun uberEatsReadsContextualPinFromNotificationText() {
        val update = RideNotificationParser.parseUberEats(
            "Uber Eats\n正前往您所在位置\n取餐碼：7616",
            null,
        )
        assertEquals("7616", update.pin)
    }

    @Test
    fun uberEatsDoesNotTreatTimesYearsOrOrderNumbersAsPin() {
        val update = RideNotificationParser.parseUberEats(
            "抵達時間 1:58 PM\n2026-07-09\n訂單 #7616",
            null,
        )
        assertNull(update.pin)
    }

    @Test
    fun uberEatsRejectsAmbiguousContextualPins() {
        val update = RideNotificationParser.parseUberEats("PIN 7616\n交付碼 4821", null)
        assertNull(update.pin)
    }

    @Test
    fun uberEatsKeepsUpdatingWithoutPin() {
        val update = RideNotificationParser.parseUberEats(
            "Uber Eats\n正在取餐\n抵達時間 1:58 PM",
            null,
        )
        assertEquals(UberEatsEvent.PICKING_UP, update.event)
        assertNull(update.pin)
    }

    private fun assertUberEatsEvent(expected: UberEatsEvent, text: String) {
        assertEquals(expected, RideNotificationParser.parseUberEats(text, null).event)
    }
}
