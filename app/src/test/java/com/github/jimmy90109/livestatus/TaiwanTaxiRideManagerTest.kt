package com.github.jimmy90109.livestatus

import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.TaiwanTaxiEvent
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser.TaiwanTaxiUpdate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TaiwanTaxiRideManagerTest {
    @Test
    fun merge_keepsPlateWhenVehicleArrivesWithoutRepeatingIt() {
        val merged = TaiwanTaxiRideManager.merge(
            previous = TaiwanTaxiUpdate(TaiwanTaxiEvent.DRIVER_FOUND, "ABC-1234"),
            update = TaiwanTaxiUpdate(TaiwanTaxiEvent.VEHICLE_ARRIVED),
        )

        assertEquals(TaiwanTaxiEvent.VEHICLE_ARRIVED, merged.event)
        assertEquals("ABC-1234", merged.plate)
    }

    @Test
    fun merge_doesNotRegressAfterVehicleArrived() {
        val merged = TaiwanTaxiRideManager.merge(
            previous = TaiwanTaxiUpdate(TaiwanTaxiEvent.VEHICLE_ARRIVED, "ABC-1234"),
            update = TaiwanTaxiUpdate(TaiwanTaxiEvent.DRIVER_FOUND),
        )

        assertEquals(TaiwanTaxiEvent.VEHICLE_ARRIVED, merged.event)
        assertEquals("ABC-1234", merged.plate)
    }

    @Test
    fun merge_allowsArrivalWithoutPreviousPlate() {
        val merged = TaiwanTaxiRideManager.merge(
            previous = TaiwanTaxiUpdate(TaiwanTaxiEvent.NONE),
            update = TaiwanTaxiUpdate(TaiwanTaxiEvent.VEHICLE_ARRIVED),
        )

        assertEquals(TaiwanTaxiEvent.VEHICLE_ARRIVED, merged.event)
        assertNull(merged.plate)
    }
}
