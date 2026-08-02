package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Test

class YouBikeVehicleClassifierTest {
    @Test
    fun identifiesElectricBikeFromThirdDigit() {
        assertEquals(
            YouBikeVehicleType.ELECTRIC_2_0E,
            YouBikeVehicleClassifier.classify("0162898"),
        )
        assertEquals(
            YouBikeVehicleType.ELECTRIC_2_0E,
            YouBikeVehicleClassifier.classify("0790676"),
        )
    }

    @Test
    fun treatsOtherSevenDigitNumbersAsStandardBike() {
        assertEquals(
            YouBikeVehicleType.STANDARD_2_0,
            YouBikeVehicleClassifier.classify("0102751"),
        )
    }

    @Test
    fun fallsBackToStandardBikeForUnrecognizedFormats() {
        listOf(null, "", "162898", "00162898", "01A2898", "016-898").forEach { bikeNumber ->
            assertEquals(
                YouBikeVehicleType.STANDARD_2_0,
                YouBikeVehicleClassifier.classify(bikeNumber),
            )
        }
    }

    @Test
    fun derivesVehicleTypeFromPersistedSessionBikeNumber() {
        val session = YouBikeRideSession(
            id = "1000-0162898",
            borrowedAtMillis = 1_000L,
            stationName = "龍江錦州街口",
            dockNumber = "09",
            bikeNumber = "0162898",
            region = YouBikeRegion.TAIPEI,
        )

        assertEquals(YouBikeVehicleType.ELECTRIC_2_0E, session.vehicleType)
    }
}
