package com.github.jimmy90109.livestatus

import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.FoodpandaEvent
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.PikminEvent
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.RideEvent
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.UberEatsEvent
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.UberRideEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveStatusNotificationParserTest {
    @Test
    fun entryNotificationStartsReminder() {
        assertEquals(
            RideEvent.ENTERED,
            LiveStatusNotificationParser.parse(
                "乘車碼交易\nNT$0\n商店名稱：臺北捷運\n中山國中2026-05-15 14:54:08 > [尚未出站]\n進站交易已完成",
            ),
        )
    }

    @Test
    fun exitNotificationClearsReminder() {
        assertEquals(
            RideEvent.EXITED,
            LiveStatusNotificationParser.parse(
                "乘車碼交易\nNT$25\n商店名稱：臺北捷運\n中山國中 > 西湖\n出站交易已完成，謝謝您使用iPASS MONEY。",
            ),
        )
    }

    @Test
    fun unrelatedNotificationsAreIgnored() {
        assertEquals(RideEvent.NONE, LiveStatusNotificationParser.parse("付款完成"))
        assertEquals(RideEvent.NONE, LiveStatusNotificationParser.parse("乘車碼交易"))
        assertEquals(RideEvent.NONE, LiveStatusNotificationParser.parse("尚未出站"))
        assertEquals(RideEvent.NONE, LiveStatusNotificationParser.parse(null))
    }

    @Test
    fun foodpandaCourierOnTheWayUpdatesReminder() {
        assertEquals(
            FoodpandaEvent.COURIER_ON_THE_WAY,
            LiveStatusNotificationParser.parseFoodpanda(
                "foodpanda\n外送夥伴在路上囉🚴，請隨時留意手機來電或訊息！",
            ),
        )
    }

    @Test
    fun foodpandaCourierArrivingUpdatesReminder() {
        assertEquals(
            FoodpandaEvent.COURIER_ARRIVING,
            LiveStatusNotificationParser.parseFoodpanda("foodpanda\n我們的外送夥伴即將抵達💪！"),
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
                LiveStatusNotificationParser.parseFoodpanda(it),
            )
        }
    }

    @Test
    fun unrelatedFoodpandaNotificationsAreIgnored() {
        assertEquals(
            FoodpandaEvent.NONE,
            LiveStatusNotificationParser.parseFoodpanda("foodpanda\n優惠快訊"),
        )
    }

    @Test
    fun uberEatsParsesAllFiveProgressStages() {
        mapOf(
            UberEatsEvent.ORDER_RECEIVED to listOf("訂單已收到", "已收到您的訂單"),
            UberEatsEvent.PREPARING to listOf("正在準備訂單", "準備訂單"),
            UberEatsEvent.PICKING_UP to listOf("正在取餐"),
            UberEatsEvent.ON_THE_WAY to listOf(
                "正前往您所在位置",
                "正在前往您所在位置",
                "即將抵達",
            ),
            UberEatsEvent.ARRIVING to listOf("快到了"),
        ).forEach { (event, alternatives) ->
            alternatives.forEach { alternative ->
                assertUberEatsEvent(event, "Uber Eats\n$alternative")
            }
        }
    }

    @Test
    fun uberEatsEndedNotificationsEndTheJourney() {
        listOf(
            "訂單已送達",
            "已送達",
            "訂單已取消",
            "已取消",
            "訂單取消",
        ).forEach { alternative ->
            assertUberEatsEvent(UberEatsEvent.ORDER_ENDED, "Uber Eats\n$alternative")
        }
    }

    @Test
    fun uberEatsReadsExactShortCriticalPin() {
        val update = LiveStatusNotificationParser.parseUberEats("Uber Eats\n快到了！", "7616")
        assertEquals("7616", update.pin)
    }

    @Test
    fun uberEatsReadsPinDigitsSplitAcrossNotificationViewText() {
        val update = LiveStatusNotificationParser.parseUberEats(
            "Uber Eats · 現在\n正在取餐\n家承 · MCU-6563 · 抵達時間：1:47-2:10 PM\n0\n1\n5\n2",
            null,
        )
        assertEquals(UberEatsEvent.PICKING_UP, update.event)
        assertEquals("0152", update.pin)
    }

    @Test
    fun uberEatsIgnoresPinLikeTextOutsideShortCriticalText() {
        val update = LiveStatusNotificationParser.parseUberEats(
            "Uber Eats\n正前往您所在位置\n取餐碼：7616",
            null,
        )
        assertNull(update.pin)
    }

    @Test
    fun uberEatsIgnoresNonExactShortCriticalPin() {
        val update = LiveStatusNotificationParser.parseUberEats(
            "Uber Eats\n快到了！",
            "PIN 7616",
        )
        assertNull(update.pin)
    }

    @Test
    fun uberEatsReadsOfficialEstimatedArrivalRange() {
        val update = LiveStatusNotificationParser.parseUberEats(
            "Uber Eats\n訂單已收到\n抵達時間：1:58-2:08 PM",
            "7616",
        )
        assertEquals(UberEatsEvent.ORDER_RECEIVED, update.event)
        assertEquals("7616", update.pin)
    }

    @Test
    fun uberEatsParsesOfficialCourierTextWithExactArrivalTime() {
        val update = LiveStatusNotificationParser.parseUberEats(
            "Uber Eats · 現在\n正前往您所在位置\n健宇 · PAR-2688 · 抵達時間為 1:56 PM",
            "7616",
        )
        assertEquals(UberEatsEvent.ON_THE_WAY, update.event)
        assertEquals("7616", update.pin)
    }

    @Test
    fun uberEatsParsesOfficialCourierTextWhenArrivingSoon() {
        val update = LiveStatusNotificationParser.parseUberEats(
            "Uber Eats · 現在\n快到了！\n健宇 · PAR-2688 · 即將抵達",
            "7616",
        )
        assertEquals(UberEatsEvent.ARRIVING, update.event)
        assertEquals("7616", update.pin)
    }

    @Test
    fun uberEatsDoesNotTreatTimesYearsOrOrderNumbersAsPin() {
        val update = LiveStatusNotificationParser.parseUberEats(
            "抵達時間 1:58 PM\n2026-07-09\n訂單 #7616\nPAR-2688",
            null,
        )
        assertNull(update.pin)
    }

    @Test
    fun uberEatsRejectsAmbiguousContextualPins() {
        val update = LiveStatusNotificationParser.parseUberEats("PIN 7616\n交付碼 4821", null)
        assertNull(update.pin)
    }

    @Test
    fun uberEatsKeepsUpdatingWithoutPin() {
        val update = LiveStatusNotificationParser.parseUberEats(
            "Uber Eats\n正在取餐\n抵達時間 1:58 PM",
            null,
        )
        assertEquals(UberEatsEvent.PICKING_UP, update.event)
        assertNull(update.pin)
    }

    @Test
    fun uberEatsReadsPinFromJoinedTextWhenShortCriticalTextIsEmpty() {
        val update = LiveStatusNotificationParser.parseUberEats(
            "正前往您所在位置\n0\n1\n5\n2\n秋發 · PAQ-8928 · 抵達時間：1:58-2:11 PM\n秋發 · PAQ-8928\nMidnightblue Uber Motorbike",
            "",
        )

        assertEquals(UberEatsEvent.ON_THE_WAY, update.event)
        assertEquals("0152", update.pin)
    }

    @Test
    fun uberEatsDoesNotReadPlateSuffixAsPinWhenJoinedTextHasNoSplitDigits() {
        val update = LiveStatusNotificationParser.parseUberEats(
            "正前往您所在位置\n秋發 · PAQ-8928 · 抵達時間：1:58-2:11 PM\n秋發 · PAQ-8928\nMidnightblue Uber Motorbike",
            "",
        )

        assertEquals(UberEatsEvent.ON_THE_WAY, update.event)
        assertNull(update.pin)
    }

    @Test
    fun uberRideParsesPickupNotification() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Uber\nPick up in 14 min\nMeet at Demo Transit Center",
            null,
        )

        assertEquals(UberRideEvent.PICKUP_EN_ROUTE, update.event)
        assertEquals("Pick up in 14 min", update.title)
        assertEquals("Demo Transit Center", update.pickupPoint)
    }

    @Test
    fun uberRideMeetAtTextStartsPickupStageWithoutEta() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Uber\nMeet at Demo Transit Center",
            null,
        )

        assertEquals(UberRideEvent.PICKUP_EN_ROUTE, update.event)
        assertEquals("Demo Transit Center", update.pickupPoint)
    }

    @Test
    fun uberRidePatternKeywordsAreCaseInsensitiveWhereAdvertised() {
        assertEquals(
            UberRideEvent.PICKUP_EN_ROUTE,
            LiveStatusNotificationParser.parseUberRide("PICK UP IN 8 MIN", null).event,
        )
        assertEquals(
            UberRideEvent.ARRIVED,
            LiveStatusNotificationParser.parseUberRide("DRIVER ARRIVED", null).event,
        )
        assertEquals(
            UberRideEvent.ON_TRIP,
            LiveStatusNotificationParser.parseUberRide("DROPOFF AT 4:30 pm", null).event,
        )
        assertEquals(
            UberRideEvent.TRIP_ENDED,
            LiveStatusNotificationParser.parseUberRide("RATE YOUR TRIP", null).event,
        )
    }

    @Test
    fun uberRideKeepsLongPickupEtaInPickupStageEvenWithVehicleAndPin() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Pick up in 14 min\nMeet at Demo Transit Center\nABC1234 · Blue Toyota Prius\n1\n2\n3\n4",
            null,
        )

        assertEquals(UberRideEvent.PICKUP_EN_ROUTE, update.event)
        assertEquals("Demo Transit Center", update.pickupPoint)
        assertEquals("1234", update.pin)
    }

    @Test
    fun uberRideParsesNearbyVehicleAndSplitPin() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Pick up in 2 min\nABC1234 · Blue Toyota Prius\n1\n2\n3\n4",
            null,
        )

        assertEquals(UberRideEvent.PICKUP_NEARBY, update.event)
        assertEquals("Pick up in 2 min", update.title)
        assertEquals("ABC1234", update.plate)
        assertEquals("Blue Toyota Prius", update.vehicle)
        assertEquals("1234", update.pin)
    }

    @Test
    fun uberRideThreeMinuteBoundaryIsNearbyWithVehicleDetails() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Pick up in 3 min\nABC1234 · Blue Toyota Prius",
            null,
        )

        assertEquals(UberRideEvent.PICKUP_NEARBY, update.event)
    }

    @Test
    fun uberRideThreeMinuteBoundaryIsNearbyWithValidPin() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Pick up in 3 min",
            "1234",
        )

        assertEquals(UberRideEvent.PICKUP_NEARBY, update.event)
        assertEquals("1234", update.pin)
    }

    @Test
    fun uberRideThreeMinuteBoundaryNeedsVehicleDetailsOrValidPin() {
        val withoutDetails = LiveStatusNotificationParser.parseUberRide(
            "Pick up in 3 min",
            null,
        )
        val invalidPin = LiveStatusNotificationParser.parseUberRide(
            "Pick up in 3 min",
            "PIN 1234",
        )

        assertEquals(UberRideEvent.PICKUP_EN_ROUTE, withoutDetails.event)
        assertEquals(UberRideEvent.PICKUP_EN_ROUTE, invalidPin.event)
        assertNull(invalidPin.pin)
    }

    @Test
    fun uberRideFourMinutesIsNotNearbyEvenWithVehicleDetailsAndPin() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Pick up in 4 min\nABC1234 · Blue Toyota Prius",
            "1234",
        )

        assertEquals(UberRideEvent.PICKUP_EN_ROUTE, update.event)
    }

    @Test
    fun uberRideParsesArrivedVehicleAndShortCriticalPin() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Driver arrived\nABC1234 · Blue Toyota Prius",
            "1234",
        )

        assertEquals(UberRideEvent.ARRIVED, update.event)
        assertEquals("Driver arrived", update.title)
        assertEquals("ABC1234", update.plate)
        assertEquals("Blue Toyota Prius", update.vehicle)
        assertEquals("1234", update.pin)
    }

    @Test
    fun uberRideParsesDropoffNotification() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Uber\nDropoff at 4:30 PM\nHeading to Demo Office Tower",
            null,
        )

        assertEquals(UberRideEvent.ON_TRIP, update.event)
        assertEquals("Dropoff at 4:30 PM", update.title)
        assertEquals("Demo Office Tower", update.dropoffPoint)
    }

    @Test
    fun uberRideHeadingToTextStartsOnTripStageWithoutDropoffTime() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Uber\nHeading to Demo Office Tower",
            null,
        )

        assertEquals(UberRideEvent.ON_TRIP, update.event)
        assertEquals("Demo Office Tower", update.dropoffPoint)
    }

    @Test
    fun uberRideRateYourTripEndsTrip() {
        val update = LiveStatusNotificationParser.parseUberRide(
            "Uber\nRate your trip\nThanks for riding with Driver. Please rate your trip.",
            null,
        )

        assertEquals(UberRideEvent.TRIP_ENDED, update.event)
    }

    @Test
    fun pikminBloomFlowerPlantingNotificationStartsReminder() {
        assertEquals(
            PikminEvent.FLOWER_PLANTING,
            LiveStatusNotificationParser.parsePikminBloom(
                "Pikmin Bloom\n正在背景執行時種花。。 · 1m",
            ),
        )
    }

    @Test
    fun pikminBloomSimplifiedChineseFlowerPlantingNotificationStartsReminder() {
        assertEquals(
            PikminEvent.FLOWER_PLANTING,
            LiveStatusNotificationParser.parsePikminBloom(
                "Pikmin Bloom\n正在背景执行时种花。。 · 1m",
            ),
        )
    }

    @Test
    fun unrelatedPikminBloomNotificationsAreIgnored() {
        assertEquals(
            PikminEvent.NONE,
            LiveStatusNotificationParser.parsePikminBloom("Pikmin Bloom\n探險完成了！"),
        )
    }

    private fun assertUberEatsEvent(expected: UberEatsEvent, text: String) {
        assertEquals(expected, LiveStatusNotificationParser.parseUberEats(text, null).event)
    }
}
