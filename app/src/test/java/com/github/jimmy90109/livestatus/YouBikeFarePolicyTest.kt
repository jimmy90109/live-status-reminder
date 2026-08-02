package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class YouBikeFarePolicyTest {
    private val start = 1_000_000_000L
    private val halfHour = 30 * 60_000L

    @Test
    fun appliesGeneralMemberFirstHalfHourSubsidy() {
        listOf(
            YouBikeRegion.TAIPEI,
            YouBikeRegion.NEW_TAIPEI,
            YouBikeRegion.HSINCHU_CITY,
            YouBikeRegion.MIAOLI,
            YouBikeRegion.TAICHUNG,
        ).forEach { region ->
            assertEquals(0, estimate(0, region).amount)
            assertEquals(0, estimate(halfHour - 1, region).amount)
            assertEquals(0, estimate(halfHour, region).amount)
            assertEquals(10, estimate(halfHour + 1, region).amount)
        }
    }

    @Test
    fun appliesTaoyuanFirstHourSubsidyAndSkipsUnchangedBoundary() {
        assertEquals(0, estimate(0, YouBikeRegion.TAOYUAN).amount)
        assertEquals(0, estimate(halfHour + 1, YouBikeRegion.TAOYUAN).amount)
        assertEquals(0, estimate(2 * halfHour, YouBikeRegion.TAOYUAN).amount)
        assertEquals(10, estimate(2 * halfHour + 1, YouBikeRegion.TAOYUAN).amount)
        assertEquals(start + 2 * halfHour, estimate(0, YouBikeRegion.TAOYUAN).nextBoundaryMillis)
        assertEquals(10, estimate(0, YouBikeRegion.TAOYUAN).nextAmount)
    }

    @Test
    fun chargesBaseFareWithoutGeneralMemberSubsidy() {
        listOf(
            YouBikeRegion.HSINCHU_COUNTY,
            YouBikeRegion.HSINCHU_SCIENCE_PARK,
        ).forEach { region ->
            assertEquals(10, estimate(0, region).amount)
            assertEquals(10, estimate(halfHour, region).amount)
            assertEquals(20, estimate(halfHour + 1, region).amount)
        }
    }

    @Test
    fun calculatesElectricFareAndChangesRateAfterTwoHours() {
        assertEquals(20, electricEstimate(0).amount)
        assertEquals(20, electricEstimate(halfHour).amount)
        assertEquals(40, electricEstimate(halfHour + 1).amount)
        assertEquals(80, electricEstimate(4 * halfHour).amount)
        assertEquals(120, electricEstimate(4 * halfHour + 1).amount)
        assertEquals(start + halfHour, electricEstimate(0).nextBoundaryMillis)
        assertEquals(40, electricEstimate(0).nextAmount)
    }

    @Test
    fun appliesElectricFirstHalfHourSubsidyInThreeRegions() {
        listOf(
            YouBikeRegion.HSINCHU_CITY,
            YouBikeRegion.MIAOLI,
            YouBikeRegion.TAICHUNG,
        ).forEach { region ->
            assertEquals(10, electricEstimate(0, region).amount)
            assertEquals(10, electricEstimate(halfHour, region).amount)
            assertEquals(30, electricEstimate(halfHour + 1, region).amount)
        }
    }

    @Test
    fun doesNotApplyElectricSubsidyInOtherSupportedRegions() {
        listOf(
            YouBikeRegion.TAIPEI,
            YouBikeRegion.NEW_TAIPEI,
            YouBikeRegion.TAOYUAN,
            YouBikeRegion.HSINCHU_COUNTY,
            YouBikeRegion.HSINCHU_SCIENCE_PARK,
        ).forEach { region ->
            assertEquals(20, electricEstimate(0, region).amount)
        }
    }

    @Test
    fun appliesRemainingRegionsGeneralMemberFares() {
        assertEquals(0, estimate(0, YouBikeRegion.CHIAYI_CITY).amount)
        assertEquals(0, estimate(0, YouBikeRegion.CHIAYI_COUNTY).amount)
        assertEquals(10, electricEstimate(0, YouBikeRegion.CHIAYI_CITY).amount)
        assertEquals(10, electricEstimate(0, YouBikeRegion.CHIAYI_COUNTY).amount)

        assertEquals(10, estimate(0, YouBikeRegion.TAINAN).amount)
        assertEquals(20, electricEstimate(0, YouBikeRegion.TAINAN).amount)

        assertEquals(5, estimate(0, YouBikeRegion.KAOHSIUNG).amount)
        assertEquals(10, electricEstimate(0, YouBikeRegion.KAOHSIUNG).amount)

        assertEquals(0, estimate(0, YouBikeRegion.PINGTUNG).amount)
        assertEquals(10, estimate(halfHour + 1, YouBikeRegion.PINGTUNG).amount)
        assertEquals(20, electricEstimate(0, YouBikeRegion.PINGTUNG).amount)

        assertEquals(0, estimate(0, YouBikeRegion.TAITUNG).amount)
        assertEquals(12, estimate(halfHour + 1, YouBikeRegion.TAITUNG).amount)
        assertEquals(13, electricEstimate(0, YouBikeRegion.TAITUNG).amount)
        assertEquals(38, electricEstimate(halfHour + 1, YouBikeRegion.TAITUNG).amount)
    }

    @Test
    fun appliesPingtungAndTaitungRegionalRateTiers() {
        assertEquals(70, estimate(8 * halfHour, YouBikeRegion.PINGTUNG).amount)
        assertEquals(90, estimate(8 * halfHour + 1, YouBikeRegion.PINGTUNG).amount)
        assertEquals(230, estimate(16 * halfHour, YouBikeRegion.PINGTUNG).amount)
        assertEquals(270, estimate(16 * halfHour + 1, YouBikeRegion.PINGTUNG).amount)

        assertEquals(84, estimate(8 * halfHour, YouBikeRegion.TAITUNG).amount)
        assertEquals(108, estimate(8 * halfHour + 1, YouBikeRegion.TAITUNG).amount)
        assertEquals(276, estimate(16 * halfHour, YouBikeRegion.TAITUNG).amount)
        assertEquals(324, estimate(16 * halfHour + 1, YouBikeRegion.TAITUNG).amount)
        assertEquals(88, electricEstimate(4 * halfHour, YouBikeRegion.TAITUNG).amount)
        assertEquals(138, electricEstimate(4 * halfHour + 1, YouBikeRegion.TAITUNG).amount)
    }

    @Test
    fun keepsRemainingRegionsStableAcrossCalendarDays() {
        val standardAmounts = mapOf(
            YouBikeRegion.CHIAYI_CITY to 1_510,
            YouBikeRegion.CHIAYI_COUNTY to 1_510,
            YouBikeRegion.TAINAN to 1_520,
            YouBikeRegion.KAOHSIUNG to 1_515,
            YouBikeRegion.PINGTUNG to 1_510,
            YouBikeRegion.TAITUNG to 1_812,
        )
        val electricAmounts = mapOf(
            YouBikeRegion.CHIAYI_CITY to 1_830,
            YouBikeRegion.CHIAYI_COUNTY to 1_830,
            YouBikeRegion.TAINAN to 1_840,
            YouBikeRegion.KAOHSIUNG to 1_830,
            YouBikeRegion.PINGTUNG to 1_840,
            YouBikeRegion.TAITUNG to 2_288,
        )

        standardAmounts.forEach { (region, expected) ->
            val result = estimate(48 * halfHour, region)
            assertEquals(expected, result.amount)
            assertEquals(start + 49 * halfHour, result.nextBoundaryMillis)
        }
        electricAmounts.forEach { (region, expected) ->
            val result = electricEstimate(48 * halfHour, region)
            assertEquals(expected, result.amount)
            assertEquals(start + 49 * halfHour, result.nextBoundaryMillis)
        }
    }

    @Test
    fun expiresChiayiSubsidyBasedOnBorrowedTime() {
        val beforeExpiry = taipeiMillis(2026, 12, 31, 23, 50)
        val afterExpiry = taipeiMillis(2027, 1, 1, 0, 0)

        assertEquals(
            0,
            requireNotNull(
                YouBikeFarePolicy.estimate(
                    beforeExpiry,
                    beforeExpiry,
                    YouBikeRegion.CHIAYI_CITY,
                ),
            ).amount,
        )
        assertEquals(
            10,
            requireNotNull(
                YouBikeFarePolicy.estimate(
                    beforeExpiry,
                    beforeExpiry + halfHour + 1,
                    YouBikeRegion.CHIAYI_CITY,
                ),
            ).amount,
        )
        assertEquals(
            10,
            requireNotNull(
                YouBikeFarePolicy.estimate(afterExpiry, afterExpiry, YouBikeRegion.CHIAYI_COUNTY),
            ).amount,
        )
        assertEquals(
            20,
            requireNotNull(
                YouBikeFarePolicy.estimate(
                    afterExpiry,
                    afterExpiry,
                    YouBikeRegion.CHIAYI_COUNTY,
                    YouBikeVehicleType.ELECTRIC_2_0E,
                ),
            ).amount,
        )
    }

    @Test
    fun changesRatesAfterFourAndEightHours() {
        assertEquals(70, estimate(8 * halfHour).amount)
        assertEquals(90, estimate(8 * halfHour + 1).amount)
        assertEquals(230, estimate(16 * halfHour).amount)
        assertEquals(270, estimate(16 * halfHour + 1).amount)
    }

    @Test
    fun returnsNextHalfHourBoundary() {
        assertEquals(start + halfHour, estimate(0).nextBoundaryMillis)
        assertEquals(start + halfHour, estimate(halfHour - 1).nextBoundaryMillis)
        assertEquals(start + 2 * halfHour, estimate(halfHour).nextBoundaryMillis)
        assertEquals(10, estimate(0).nextAmount)
        assertEquals(20, estimate(halfHour + 1).nextAmount)
    }

    @Test
    fun keepsRateCalculationStableAcrossCalendarDays() {
        assertEquals(1_510, estimate(48 * halfHour).amount)
        assertEquals(1_550, estimate(48 * halfHour + 1).amount)
        assertEquals(1_840, electricEstimate(48 * halfHour).amount)
        assertEquals(1_880, electricEstimate(48 * halfHour + 1).amount)
    }

    @Test
    fun doesNotEstimateUnknownOrUnsupportedRegions() {
        assertNull(YouBikeFarePolicy.estimate(start, start, YouBikeRegion.UNRESOLVED))
        assertNull(YouBikeFarePolicy.estimate(start, start, YouBikeRegion.UNSUPPORTED))
    }

    private fun estimate(
        elapsed: Long,
        region: YouBikeRegion = YouBikeRegion.TAIPEI,
    ) = requireNotNull(YouBikeFarePolicy.estimate(start, start + elapsed, region))

    private fun electricEstimate(
        elapsed: Long,
        region: YouBikeRegion = YouBikeRegion.TAIPEI,
    ) = requireNotNull(
        YouBikeFarePolicy.estimate(
            start,
            start + elapsed,
            region,
            YouBikeVehicleType.ELECTRIC_2_0E,
        ),
    )

    private fun taipeiMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Long = LocalDateTime.of(year, month, day, hour, minute)
        .atZone(ZoneId.of("Asia/Taipei"))
        .toInstant()
        .toEpochMilli()
}
