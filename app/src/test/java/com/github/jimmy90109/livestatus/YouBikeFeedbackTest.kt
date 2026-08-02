package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class YouBikeFeedbackTest {
    @Test
    fun createsReportForUnknownStationWithSupportedManualRegion() {
        val report = YouBikeFeedbackPolicy.createReport(
            session = session(
                issue = YouBikeStationResolutionIssue.UNKNOWN,
                manualRegion = YouBikeRegion.KAOHSIUNG,
            ),
            appVersionName = "1.2.3",
            appVersionCode = 123,
            stationIndexVersion = "2026-08-01T14:48:01+00:00",
        )

        assertNotNull(report)
        assertEquals(YouBikeRegion.KAOHSIUNG, report?.selectedRegion)
        assertEquals(YouBikeStationResolutionIssue.UNKNOWN, report?.issue)
    }

    @Test
    fun createsReportForAmbiguousStationAndPreservesCandidates() {
        val candidates = setOf(YouBikeRegion.TAIPEI, YouBikeRegion.TAICHUNG)
        val report = YouBikeFeedbackPolicy.createReport(
            session = session(
                issue = YouBikeStationResolutionIssue.AMBIGUOUS,
                manualRegion = YouBikeRegion.TAICHUNG,
                candidates = candidates,
            ),
            appVersionName = "1.2.3",
            appVersionCode = 123,
            stationIndexVersion = "index",
        )

        assertEquals(candidates, report?.candidateRegions)
    }

    @Test
    fun doesNotCreateReportWithoutEligibleManualResolution() {
        assertNull(createReport(session(issue = null, manualRegion = YouBikeRegion.TAIPEI)))
        assertNull(createReport(session(issue = YouBikeStationResolutionIssue.UNKNOWN, manualRegion = null)))
        assertNull(
            createReport(
                session(
                    issue = YouBikeStationResolutionIssue.UNKNOWN,
                    manualRegion = YouBikeRegion.UNSUPPORTED,
                ),
            ),
        )
    }

    @Test
    fun promptsOncePerStationAndResetsForNewIndexVersion() {
        val emptyState = YouBikeFeedbackPromptState(null, emptySet())
        val first = YouBikeFeedbackDedupPolicy.markPrompted(emptyState, "index-a", "測試站")
        val duplicate = YouBikeFeedbackDedupPolicy.markPrompted(
            first.updatedState,
            "index-a",
            "測試站",
        )
        val newIndex = YouBikeFeedbackDedupPolicy.markPrompted(
            duplicate.updatedState,
            "index-b",
            "測試站",
        )

        assertTrue(first.shouldPrompt)
        assertFalse(duplicate.shouldPrompt)
        assertTrue(newIndex.shouldPrompt)
        assertEquals(1, newIndex.updatedState.promptedStationHashes.size)
        assertFalse(newIndex.updatedState.promptedStationHashes.single().contains("測試站"))
    }

    @Test
    fun parsesGeneratedStationIndexVersion() {
        assertEquals(
            "2026-08-01T14:48:01+00:00",
            YouBikeStationIndexMetadata.parseVersion(
                sequenceOf(
                    "# Generated station-name index.",
                    "# generatedAt=2026-08-01T14:48:01+00:00 source=TDX rows=9406",
                    "YouBike2.0_測試站\tTaipei",
                ),
            ),
        )
        assertNull(YouBikeStationIndexMetadata.parseVersion(sequenceOf("invalid")))
    }

    @Test
    fun emailIsEncodedAndContainsOnlySanitizedReportFields() {
        val sourceSession = session(
            issue = YouBikeStationResolutionIssue.AMBIGUOUS,
            manualRegion = YouBikeRegion.TAIPEI,
            candidates = setOf(YouBikeRegion.TAIPEI, YouBikeRegion.NEW_TAIPEI),
        )
        val report = requireNotNull(createReport(sourceSession))
        val uri = YouBikeFeedbackEmail.mailtoUri(report)
        val parameters = uri.substringAfter('?').split('&').associate { parameter ->
            val (key, value) = parameter.split('=', limit = 2)
            key to URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }
        val body = requireNotNull(parameters["body"])

        assertTrue(uri.startsWith("mailto:${YouBikeFeedbackEmail.RECIPIENT}"))
        assertEquals("[LiveStatus] YouBike 站點辨識回報", parameters["subject"])
        assertTrue(body.contains("測試 & 站"))
        assertTrue(body.contains("臺北市"))
        assertTrue(body.contains("同名站點"))
        assertFalse(body.contains(sourceSession.bikeNumber))
        assertFalse(body.contains(sourceSession.dockNumber.orEmpty()))
        assertFalse(body.contains(sourceSession.borrowedAtMillis.toString()))
        assertFalse(body.contains("信用卡"))
    }

    private fun createReport(session: YouBikeRideSession): YouBikeFeedbackReport? =
        YouBikeFeedbackPolicy.createReport(
            session = session,
            appVersionName = "1.2.3",
            appVersionCode = 123,
            stationIndexVersion = "index",
        )

    private fun session(
        issue: YouBikeStationResolutionIssue?,
        manualRegion: YouBikeRegion?,
        candidates: Set<YouBikeRegion> = emptySet(),
    ) = YouBikeRideSession(
        id = "1000-0102751",
        borrowedAtMillis = 1_000L,
        stationName = "YouBike2.0_測試 & 站",
        dockNumber = "09",
        bikeNumber = "0102751",
        region = manualRegion ?: YouBikeRegion.UNRESOLVED,
        candidateRegions = candidates,
        manuallySelectedRegion = manualRegion,
        originalResolutionIssue = issue,
    )
}
