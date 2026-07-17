package com.github.jimmy90109.livestatus

import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.FoodpandaEvent
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.UberEatsEvent
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.UberRideEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveStatusPayloadTest {
    @Test
    fun ridePayloadUsesQrIconAppNameAndCriticalText() {
        val payload = LiveStatusReminder.ridePayload()

        assertEquals(R.drawable.ic_notification, payload.smallIconRes)
        assertEquals(R.drawable.ic_notification, payload.leftIconRes)
        assertEquals("iPASS MONEY", payload.appName)
        assertEquals("乘車中", payload.criticalText)
    }

    @Test
    fun foodpandaPayloadUsesDeliveryIconAppNameAndCriticalText() {
        val onTheWay = LiveStatusReminder.foodpandaPayload(FoodpandaEvent.COURIER_ON_THE_WAY)
        val arriving = LiveStatusReminder.foodpandaPayload(FoodpandaEvent.COURIER_ARRIVING)

        assertEquals(R.drawable.ic_food_delivery_notification, onTheWay.smallIconRes)
        assertEquals(R.drawable.ic_food_delivery_notification, onTheWay.leftIconRes)
        assertEquals("foodpanda", onTheWay.appName)
        assertEquals("外送中", onTheWay.criticalText)
        assertEquals("即將抵達", arriving.criticalText)
    }

    @Test
    fun uberEatsPayloadUsesDeliveryIconAppNameAndCriticalText() {
        val payload = LiveStatusReminder.uberEatsPayload(UberEatsEvent.ON_THE_WAY)

        assertEquals(R.drawable.ic_food_delivery_notification, payload.smallIconRes)
        assertEquals(R.drawable.ic_food_delivery_notification, payload.leftIconRes)
        assertEquals("Uber Eats", payload.appName)
        assertEquals("配送中", payload.criticalText)
        assertEquals(80, payload.progress)
    }

    @Test
    fun uberEatsPayloadDoesNotExposePinAsCriticalText() {
        val update = LiveStatusNotificationParser.parseUberEats("Uber Eats\n快到了！", "7616")
        val payload = LiveStatusReminder.uberEatsPayload(update.event)

        assertEquals("7616", update.pin)
        assertEquals("快到了", payload.criticalText)
    }

    @Test
    fun uberEatsOnTheWayPrivateTextKeepsOfficialDetailsWithoutDuplicateStatus() {
        val text = LiveStatusReminder.uberEatsPrivateText(
            UberEatsEvent.ON_THE_WAY,
            "正前往您所在位置\n0\n1\n5\n2\n秋發 · PAQ-8928 · 抵達時間：1:58-2:11 PM\n秋發 · PAQ-8928\nMidnightblue Uber Motorbike",
            "0152",
        )

        assertEquals("秋發 · PAQ-8928 · 抵達時間：1:58-2:11 PM · PIN 0152", text)
    }

    @Test
    fun uberEatsOtherPrivateTextKeepsStatusAndPin() {
        val text = LiveStatusReminder.uberEatsPrivateText(
            UberEatsEvent.PICKING_UP,
            "外送夥伴正在取餐。",
            "0152",
        )

        assertEquals("外送夥伴正在取餐。 · PIN 0152", text)
    }

    @Test
    fun uberRidePayloadUsesPickupPointBeforePickup() {
        val payload = LiveStatusReminder.uberRidePayload(
            LiveStatusNotificationParser.UberRideUpdate(
                event = UberRideEvent.PICKUP_EN_ROUTE,
                title = "Pick up in 14 min",
                pickupPoint = "Meet at Demo Transit Center",
            ),
        )

        assertEquals("Uber", payload.appName)
        assertEquals("Pick up in 14 min", payload.title)
        assertEquals("Meet at Demo Transit Center", payload.contentText)
        assertEquals("To pickup", payload.criticalText)
        assertEquals(25, payload.progress)
    }

    @Test
    fun uberRidePayloadUsesVehicleAndPinNearPickup() {
        val payload = LiveStatusReminder.uberRidePayload(
            LiveStatusNotificationParser.UberRideUpdate(
                event = UberRideEvent.PICKUP_NEARBY,
                title = "Pick up in 2 min",
                plate = "ABC1234",
                vehicle = "Blue Toyota Prius",
                pin = "1234",
            ),
        )

        assertEquals("Pick up in 2 min", payload.title)
        assertEquals("ABC1234 · Blue Toyota Prius · PIN 1234", payload.contentText)
        assertEquals("Nearby", payload.criticalText)
    }

    @Test
    fun uberRidePayloadUsesDropoffPointOnTrip() {
        val payload = LiveStatusReminder.uberRidePayload(
            LiveStatusNotificationParser.UberRideUpdate(
                event = UberRideEvent.ON_TRIP,
                title = "Dropoff at 4:30 PM",
                dropoffPoint = "Demo Office Tower",
            ),
        )

        assertEquals("Dropoff at 4:30 PM", payload.title)
        assertEquals("Demo Office Tower", payload.contentText)
        assertEquals("On trip", payload.criticalText)
        assertEquals(80, payload.progress)
    }

    @Test
    fun pikminBloomPayloadUsesFlowerIconAppNameAndNoProgress() {
        val payload = LiveStatusReminder.pikminBloomPayload()

        assertEquals(R.drawable.ic_pikmin_flower_notification, payload.smallIconRes)
        assertEquals(R.drawable.ic_pikmin_flower_notification, payload.leftIconRes)
        assertEquals("Pikmin Bloom", payload.appName)
        assertEquals("種花中", payload.criticalText)
        assertEquals("Pikmin Bloom 正在種花", payload.title)
        assertEquals("記得結束種花，避免花瓣在原地耗盡。", payload.contentText)
        assertEquals(null, payload.progress)
    }

    @Test
    fun xiaomiRendererOnlyTargetsXiaomiFamilyBrands() {
        assertTrue(XiaomiHyperIslandRenderer.shouldRender("Xiaomi", "Xiaomi"))
        assertTrue(XiaomiHyperIslandRenderer.shouldRender("Xiaomi", "Redmi"))
        assertTrue(XiaomiHyperIslandRenderer.shouldRender("Xiaomi", "POCO"))
        assertFalse(XiaomiHyperIslandRenderer.shouldRender("Google", "Pixel"))
        assertFalse(XiaomiHyperIslandRenderer.shouldRender("samsung", "samsung"))
    }
}
