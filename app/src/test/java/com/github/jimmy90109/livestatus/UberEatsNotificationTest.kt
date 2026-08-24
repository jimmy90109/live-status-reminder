package com.github.jimmy90109.livestatus

import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.UberEatsEvent
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.UberEatsLanguage
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.UberEatsUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UberEatsNotificationTest {
    @Test
    fun sourcePolicyOnlySupportsNonSummaryDecoratedCustomViewNotifications() {
        assertTrue(
            UberEatsNotificationSourcePolicy.supports(
                UberEatsNotificationSourcePolicy.DECORATED_CUSTOM_VIEW_TEMPLATE,
                isGroupSummary = false,
            ),
        )
        assertEquals(
            false,
            UberEatsNotificationSourcePolicy.supports(
                "android.app.Notification\$BigTextStyle",
                isGroupSummary = false,
            ),
        )
        assertEquals(
            false,
            UberEatsNotificationSourcePolicy.supports(
                UberEatsNotificationSourcePolicy.DECORATED_CUSTOM_VIEW_TEMPLATE,
                isGroupSummary = true,
            ),
        )
    }

    @Test
    fun trackerUpdatesSameStageButDoesNotRegress() {
        val tracker = UberEatsTracker()
        tracker.onPosted(
            "persistent",
            update(UberEatsEvent.ON_THE_WAY, pin = "0152"),
            null,
            "Heading your way\nArrives at 12:36 PM",
        )

        val refreshed = tracker.onPosted(
            "persistent",
            update(UberEatsEvent.ON_THE_WAY),
            null,
            "Heading your way\nArrives at 12:37 PM",
        ) as UberEatsDecision.Show
        val regressive = tracker.onPosted(
            "persistent",
            update(UberEatsEvent.PICKING_UP),
            null,
            "Picking up your order",
        )

        assertEquals("Arrives at 12:37 PM", refreshed.update.officialText.lines().last())
        assertEquals("0152", refreshed.update.pin)
        assertEquals(UberEatsDecision.None, regressive)
    }

    @Test
    fun trackerClearsOnlyWhenActivePersistentNotificationIsRemoved() {
        val tracker = UberEatsTracker()
        tracker.onPosted(
            "persistent",
            update(UberEatsEvent.PREPARING),
            null,
            "Preparing your order",
        )

        assertEquals(UberEatsDecision.None, tracker.onRemoved("big-text"))
        assertEquals(UberEatsDecision.Clear, tracker.onRemoved("persistent"))
        assertEquals(UberEatsDecision.None, tracker.onRemoved("persistent"))
    }

    @Test
    fun trackerResetsPinAndLanguageForNewOrderAndClearsOnDelivered() {
        val tracker = UberEatsTracker()
        tracker.onPosted(
            "first",
            update(UberEatsEvent.ARRIVING, pin = "0152"),
            null,
            "Almost here!",
        )

        val nextOrder = tracker.onPosted(
            "second",
            UberEatsUpdate(
                event = UberEatsEvent.ORDER_RECEIVED,
                pin = null,
                language = UberEatsLanguage.TRADITIONAL_CHINESE,
            ),
            null,
            "訂單已收到",
        ) as UberEatsDecision.Show
        val delivered = tracker.onPosted(
            "second",
            update(UberEatsEvent.ORDER_ENDED),
            null,
            "Order delivered",
        )

        assertEquals(UberEatsLanguage.TRADITIONAL_CHINESE, nextOrder.update.language)
        assertEquals(null, nextOrder.update.pin)
        assertEquals(UberEatsDecision.Clear, delivered)
        assertEquals(UberEatsDecision.None, tracker.onRemoved("second"))
    }

    private fun update(event: UberEatsEvent, pin: String? = null) = UberEatsUpdate(
        event = event,
        pin = pin,
        language = UberEatsLanguage.ENGLISH,
    )
}
