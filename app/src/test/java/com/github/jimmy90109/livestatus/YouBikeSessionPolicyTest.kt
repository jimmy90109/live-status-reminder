package com.github.jimmy90109.livestatus

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YouBikeSessionPolicyTest {
    private val session = YouBikeRideSession(
        id = "1000-0102751",
        borrowedAtMillis = 1_000L,
        stationName = "龍江錦州街口",
        dockNumber = "09",
        bikeNumber = "0102751",
        region = YouBikeRegion.TAIPEI,
    )

    @Test
    fun ignoresDuplicateAndOlderBorrowButAcceptsNewerBorrow() {
        assertFalse(YouBikeSessionPolicy.shouldReplace(session, 1_000L, "0102751"))
        assertFalse(YouBikeSessionPolicy.shouldReplace(session, 999L, "9999999"))
        assertTrue(YouBikeSessionPolicy.shouldReplace(session, 1_001L, "9999999"))
    }

    @Test
    fun endsOnlyForMatchingBikeAndNonStaleReturn() {
        assertTrue(YouBikeSessionPolicy.shouldEnd(session, "0102751", 1_100L))
        assertFalse(YouBikeSessionPolicy.shouldEnd(session, "9999999", 1_100L))
        assertFalse(YouBikeSessionPolicy.shouldEnd(session, "0102751", 999L))
    }

    @Test
    fun duplicateBorrowKeepsHiddenStateButNewRideReturnsToVisible() {
        assertTrue(
            YouBikeTrackingVisibilityPolicy.hiddenAfterBorrow(
                current = session,
                isCurrentlyHidden = true,
                borrowedAtMillis = session.borrowedAtMillis,
                bikeNumber = session.bikeNumber,
            ),
        )
        assertFalse(
            YouBikeTrackingVisibilityPolicy.hiddenAfterBorrow(
                current = session,
                isCurrentlyHidden = true,
                borrowedAtMillis = session.borrowedAtMillis + 1L,
                bikeNumber = "9999999",
            ),
        )
    }

    @Test
    fun hiddenTrackingSkipsDisplayUntilMatchingSessionIsRestored() {
        assertFalse(YouBikeTrackingVisibilityPolicy.shouldDisplay(isHidden = true))
        assertTrue(YouBikeTrackingVisibilityPolicy.shouldDisplay(isHidden = false))
        assertTrue(YouBikeTrackingVisibilityPolicy.matchesSession(session, session.id))
        assertFalse(YouBikeTrackingVisibilityPolicy.matchesSession(session, "stale-session"))
    }

    @Test
    fun updatesLegacyUnsupportedSessionWithNewResolution() {
        val legacySession = session.copy(region = YouBikeRegion.UNSUPPORTED)

        val updated = YouBikeSessionPolicy.withResolution(
            legacySession,
            YouBikeStationResolution.Supported(YouBikeRegion.KAOHSIUNG),
        )

        assertTrue(updated.region == YouBikeRegion.KAOHSIUNG)
        assertTrue(updated.candidateRegions == setOf(YouBikeRegion.KAOHSIUNG))
    }

    @Test
    fun keepsAmbiguousCandidatesForLegacySession() {
        val candidates = setOf(YouBikeRegion.CHIAYI_CITY, YouBikeRegion.CHIAYI_COUNTY)

        val updated = YouBikeSessionPolicy.withResolution(
            session.copy(region = YouBikeRegion.UNRESOLVED),
            YouBikeStationResolution.Ambiguous(candidates),
        )

        assertTrue(updated.region == YouBikeRegion.UNRESOLVED)
        assertTrue(updated.candidateRegions == candidates)
    }
}
