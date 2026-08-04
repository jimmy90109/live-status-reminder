package com.github.jimmy90109.livestatus

import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.TaiwanTaxiEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaiwanTaxiNotificationParserTest {
    @Test
    fun driverFound_extractsPlateFromObservedNotification() {
        val update = LiveStatusNotificationParser.parseTaiwanTaxi(
            notificationTitle = "已找到司機",
            notificationContentText =
                "已替您找到車輛ABC-1234，預估抵達時間會隨著路況更新，請留意司機抵達時間。",
            notificationText = null,
        )

        assertEquals(TaiwanTaxiEvent.DRIVER_FOUND, update.event)
        assertEquals("ABC-1234", update.plate)
    }

    @Test
    fun vehicleArrived_matchesObservedNotificationWithoutGuessingPlate() {
        val update = LiveStatusNotificationParser.parseTaiwanTaxi(
            notificationTitle = "車輛已抵達",
            notificationContentText =
                "司機已抵達上車位置，請在指定時間內上車，以免耍誤您的行程。",
            notificationText = null,
        )

        assertEquals(TaiwanTaxiEvent.VEHICLE_ARRIVED, update.event)
        assertNull(update.plate)
    }

    @Test
    fun tripEnded_ignoresPaymentAmountAndPunctuation() {
        listOf(
            "您已支付 ${'$'}x元,如需查看明細,請於App 「歷程紀錄」查詢付款紀錄。",
            "您已支付 ${'$'}1,250 元，如需查看明細，請於 App 查詢。",
        ).forEach { content ->
            val update = LiveStatusNotificationParser.parseTaiwanTaxi(
                notificationTitle = "行程已完成",
                notificationContentText = content,
                notificationText = null,
            )

            assertEquals(TaiwanTaxiEvent.TRIP_ENDED, update.event)
            assertNull(update.plate)
        }
    }

    @Test
    fun joinedTextFallback_handlesWhitespaceDuplicatesAndLowercasePlate() {
        val update = LiveStatusNotificationParser.parseTaiwanTaxi(
            notificationTitle = "  ",
            notificationContentText = null,
            notificationText =
                "\n  已找到司機  \n已替您找到車輛 abc-1234，請留意。\n已找到司機\n",
        )

        assertEquals(TaiwanTaxiEvent.DRIVER_FOUND, update.event)
        assertEquals("ABC-1234", update.plate)
    }

    @Test
    fun tripEndedTakesPriorityInFallbackPayload() {
        val update = LiveStatusNotificationParser.parseTaiwanTaxi(
            notificationTitle = null,
            notificationContentText = null,
            notificationText = "已找到司機\n已替您找到車輛ABC-1234\n行程已完成",
        )

        assertEquals(TaiwanTaxiEvent.TRIP_ENDED, update.event)
        assertNull(update.plate)
    }

    @Test
    fun unrelatedAndIncompleteNotificationsDoNotMatch() {
        val inputs = listOf(
            Triple("優惠通知", "輸入折扣碼 ABC-1234", null),
            Triple("已找到車位", "已替您找到車輛ABC-1234", null),
            Triple("付款成功", "您已支付 ${'$'}556 元", null),
            Triple("已找到司機", "訂單編號 ABC-1234", null),
            Triple(null, null, null),
            Triple(" ", "\n", "  "),
        )

        inputs.forEach { (title, content, text) ->
            val update = LiveStatusNotificationParser.parseTaiwanTaxi(title, content, text)
            if (title == "已找到司機") {
                assertEquals(TaiwanTaxiEvent.DRIVER_FOUND, update.event)
                assertNull(update.plate)
            } else {
                assertEquals(TaiwanTaxiEvent.NONE, update.event)
                assertNull(update.plate)
            }
        }
    }
}
