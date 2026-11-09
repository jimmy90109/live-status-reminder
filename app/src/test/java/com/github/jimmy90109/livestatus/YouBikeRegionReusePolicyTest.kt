package com.github.jimmy90109.livestatus

import org.junit.Assert.*
import org.junit.Test

class YouBikeRegionReusePolicyTest {
    private val candidates = setOf(YouBikeRegion.NEW_TAIPEI, YouBikeRegion.TAICHUNG, YouBikeRegion.KAOHSIUNG)
    private val ambiguous = YouBikeStationResolution.Ambiguous(candidates)
    private val session = YouBikeRideSession(
        id = "ride", borrowedAtMillis = 1_000L, stationName = "建國國小",
        dockNumber = null, bikeNumber = "0102751", region = YouBikeRegion.KAOHSIUNG,
        candidateRegions = candidates, originalResolutionIssue = YouBikeStationResolutionIssue.AMBIGUOUS,
        isRegionReused = true,
    )

    @Test
    fun reusesOnlySupportedRememberedCandidate() {
        assertEquals(YouBikeRegionSelection(YouBikeRegion.KAOHSIUNG, true),
            YouBikeRegionReusePolicy.select(ambiguous, YouBikeRegion.KAOHSIUNG))
        for (region in listOf(null, YouBikeRegion.TAIPEI, YouBikeRegion.UNSUPPORTED, YouBikeRegion.UNRESOLVED)) {
            assertEquals(YouBikeRegionSelection(YouBikeRegion.UNRESOLVED),
                YouBikeRegionReusePolicy.select(ambiguous, region))
        }
    }

    @Test
    fun indexWinsAndUnknownNeverReuses() {
        assertEquals(YouBikeRegionSelection(YouBikeRegion.TAIPEI), YouBikeRegionReusePolicy.select(
            YouBikeStationResolution.Supported(YouBikeRegion.TAIPEI), YouBikeRegion.KAOHSIUNG))
        assertEquals(YouBikeRegionSelection(YouBikeRegion.UNRESOLVED), YouBikeRegionReusePolicy.select(
            YouBikeStationResolution.Unknown, YouBikeRegion.KAOHSIUNG))
        assertEquals(YouBikeRegionSelection(YouBikeRegion.UNSUPPORTED), YouBikeRegionReusePolicy.select(
            YouBikeStationResolution.Unsupported, YouBikeRegion.KAOHSIUNG))
    }

    @Test
    fun ignoresInvalidPreferenceValues() {
        for (value in listOf(null, "", "invalid", "UNRESOLVED", "UNSUPPORTED")) {
            assertNull(YouBikeRegionReusePolicy.parseRememberedRegion(value))
        }
        assertEquals(YouBikeRegion.KAOHSIUNG, YouBikeRegionReusePolicy.parseRememberedRegion("KAOHSIUNG"))
    }

    @Test
    fun manualConfirmationClearsReuseAndEnablesFeedback() {
        assertNull(report(session))
        for (region in listOf(YouBikeRegion.KAOHSIUNG, YouBikeRegion.TAICHUNG)) {
            val updated = requireNotNull(YouBikeRegionReusePolicy.withManualRegion(session, region))
            assertFalse(updated.isRegionReused)
            assertEquals(region, updated.manuallySelectedRegion)
            assertEquals(region, report(updated)?.selectedRegion)
        }
        val other = requireNotNull(YouBikeRegionReusePolicy.withManualRegion(session, YouBikeRegion.UNSUPPORTED))
        assertNull(report(other))
        assertNull(YouBikeRegionReusePolicy.withManualRegion(session, YouBikeRegion.TAIPEI))
        assertNull(YouBikeRegionReusePolicy.withManualRegion(session, YouBikeRegion.UNRESOLVED))
    }

    private fun report(session: YouBikeRideSession) = YouBikeFeedbackPolicy.createReport(session, "test", 1L, "index")
}
