package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GenericProgressNotificationTest {
    @Test
    fun acceptsDeterminateProgressAndNormalizesPercent() {
        assertEquals(0, GenericProgressPolicy.progressPercent(signals(progress = 0)))
        assertEquals(42, GenericProgressPolicy.progressPercent(signals(progress = 42)))
        assertEquals(100, GenericProgressPolicy.progressPercent(signals(progress = 100)))
        assertEquals(100, GenericProgressPolicy.progressPercent(signals(progress = 125)))
        assertEquals(0, GenericProgressPolicy.progressPercent(signals(progress = -10)))
        assertEquals(
            33,
            GenericProgressPolicy.progressPercent(
                signals(progress = 1, progressMax = 3),
            ),
        )
    }

    @Test
    fun rejectsInvalidOrIndeterminateProgress() {
        assertNull(GenericProgressPolicy.progressPercent(signals(progressMax = 0)))
        assertNull(GenericProgressPolicy.progressPercent(signals(progressMax = -1)))
        assertNull(
            GenericProgressPolicy.progressPercent(
                signals(progressIndeterminate = true),
            ),
        )
    }

    @Test
    fun rejectsNotificationsThatMustNotBeMirrored() {
        assertNull(
            GenericProgressPolicy.progressPercent(
                signals(sourcePackageName = "com.example.owner"),
            ),
        )
        assertNull(GenericProgressPolicy.progressPercent(signals(isGroupSummary = true)))
        assertNull(GenericProgressPolicy.progressPercent(signals(isPromotedOngoing = true)))
        assertNull(GenericProgressPolicy.progressPercent(signals(isMediaNotification = true)))
        assertNull(GenericProgressPolicy.progressPercent(signals(hasDedicatedIntegration = true)))
    }

    @Test
    fun dedicatedSourcePolicyReservesExistingIntegrationsOnly() {
        assertTrue(GenericProgressSourcePolicy.hasDedicatedIntegration("com.ubercab"))
        assertTrue(
            GenericProgressSourcePolicy.hasDedicatedIntegration("tw.com.trtc.is.android05"),
        )
        assertTrue(
            GenericProgressSourcePolicy.hasDedicatedIntegration(
                "com.google.android.apps.recorder",
            ),
        )
        assertFalse(GenericProgressSourcePolicy.hasDedicatedIntegration("com.example.downloader"))
    }

    @Test
    fun actionPolicyKeepsFirstThreeActionableItemsInOrder() {
        val selected = GenericProgressActionPolicy.select(
            listOf("open", "invalid", "pause", "cancel", "extra"),
        ) { it != "invalid" }

        assertEquals(listOf("open", "pause", "cancel"), selected)
    }

    @Test
    fun trackerMaintainsIndependentSourceKeys() {
        val tracker = GenericProgressTracker()
        val first = update("first", 10)
        val second = update("second", 20)

        assertEquals(GenericProgressDecision.Show(first), tracker.onPosted("first", first))
        assertEquals(GenericProgressDecision.Show(second), tracker.onPosted("second", second))
        assertEquals(GenericProgressDecision.Clear("first"), tracker.onRemoved("first"))
        assertEquals(listOf("second"), tracker.reset())
    }

    @Test
    fun ineligibleUpdateClearsOnlyItsPreviousMirror() {
        val tracker = GenericProgressTracker()
        tracker.onPosted("first", update("first", 10))
        tracker.onPosted("second", update("second", 20))

        assertEquals(GenericProgressDecision.Clear("first"), tracker.onPosted("first", null))
        assertEquals(listOf("second"), tracker.reset())
    }

    @Test
    fun notificationTagIsStableOpaqueAndSourceSpecific() {
        val first = GenericProgressNotificationIdentity.tag("source|private-title|42")
        val repeated = GenericProgressNotificationIdentity.tag("source|private-title|42")
        val second = GenericProgressNotificationIdentity.tag("another-source")

        assertEquals(first, repeated)
        assertFalse(first.contains("private-title"))
        assertFalse(first == second)
        assertTrue(GenericProgressNotificationIdentity.isGenericProgressTag(first))
        assertFalse(GenericProgressNotificationIdentity.isGenericProgressTag("unrelated"))
    }

    private fun signals(
        sourcePackageName: String = "com.example.downloader",
        progress: Int = 50,
        progressMax: Int = 100,
        progressIndeterminate: Boolean = false,
        isGroupSummary: Boolean = false,
        isPromotedOngoing: Boolean = false,
        isMediaNotification: Boolean = false,
        hasDedicatedIntegration: Boolean = false,
    ) = GenericProgressSignals(
        sourcePackageName = sourcePackageName,
        ownPackageName = "com.example.owner",
        progress = progress,
        progressMax = progressMax,
        progressIndeterminate = progressIndeterminate,
        isGroupSummary = isGroupSummary,
        isPromotedOngoing = isPromotedOngoing,
        isMediaNotification = isMediaNotification,
        hasDedicatedIntegration = hasDedicatedIntegration,
    )

    private fun update(sourceKey: String, progressPercent: Int) = GenericProgressUpdate(
        sourceKey = sourceKey,
        sourcePackageName = "com.example.downloader",
        sourceAppName = "Downloader",
        title = "Downloading",
        contentText = "example.zip",
        progressPercent = progressPercent,
        smallIcon = null,
        largeIcon = null,
        contentIntent = null,
        visibility = 0,
        sourceActions = emptyList(),
    )
}
