package com.github.jimmy90109.livestatus

import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.FoodpandaEvent
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.UberEatsEvent
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
    fun xiaomiRendererOnlyTargetsXiaomiFamilyBrands() {
        assertTrue(XiaomiHyperIslandRenderer.shouldRender("Xiaomi", "Xiaomi"))
        assertTrue(XiaomiHyperIslandRenderer.shouldRender("Xiaomi", "Redmi"))
        assertTrue(XiaomiHyperIslandRenderer.shouldRender("Xiaomi", "POCO"))
        assertFalse(XiaomiHyperIslandRenderer.shouldRender("Google", "Pixel"))
        assertFalse(XiaomiHyperIslandRenderer.shouldRender("samsung", "samsung"))
    }
}
