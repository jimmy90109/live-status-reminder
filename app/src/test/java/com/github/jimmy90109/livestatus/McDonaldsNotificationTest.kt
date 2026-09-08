package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Test

class McDonaldsNotificationTest {
    @Test
    fun validReadyOrderShowsAndLatestOrderReplacesSourceKey() {
        val tracker = McDonaldsTracker()
        val first = tracker.onPosted("first", ready("97167"))
        val second = tracker.onPosted("second", ready("12345"))

        assertEquals(McDonaldsDecision.Show(ready("97167")), first)
        assertEquals(McDonaldsDecision.Show(ready("12345")), second)
        assertEquals(McDonaldsDecision.None, tracker.onRemoved("first"))
        assertEquals(McDonaldsDecision.Clear, tracker.onRemoved("second"))
    }

    @Test
    fun unrelatedUpdatesAndRemovalsDoNotChangeActiveOrder() {
        val tracker = McDonaldsTracker()
        tracker.onPosted("ready", ready("97167"))

        assertEquals(
            McDonaldsDecision.None,
            tracker.onPosted(
                "promo",
                LiveStatusNotificationParser.McDonaldsUpdate(
                    LiveStatusNotificationParser.McDonaldsEvent.NONE,
                ),
            ),
        )
        assertEquals(McDonaldsDecision.None, tracker.onRemoved("promo"))
        assertEquals(McDonaldsDecision.Clear, tracker.onRemoved("ready"))
        assertEquals(McDonaldsDecision.None, tracker.onRemoved("ready"))
    }

    private fun ready(orderNumber: String) = LiveStatusNotificationParser.McDonaldsUpdate(
        event = LiveStatusNotificationParser.McDonaldsEvent.READY_FOR_PICKUP,
        orderNumber = orderNumber,
    )
}
