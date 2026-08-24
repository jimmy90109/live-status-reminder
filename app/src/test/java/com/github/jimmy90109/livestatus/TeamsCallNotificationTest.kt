package com.github.jimmy90109.livestatus

import android.app.Notification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TeamsCallNotificationTest {
    private val validSignals = TeamsCallSignals(
        sourceKey = "0|com.microsoft.teams|-1|null|10838",
        channelId =
            "com.microsoft.teams.CallsOngoing.00000000-0000-0000-a553-e9f2c3ecada8",
        category = Notification.CATEGORY_CALL,
        isOngoing = true,
        isForegroundService = true,
        isNoClear = true,
        isPromoted = false,
        sourceWhenEpochMillis = 1_700_000_000_000L,
        postedAtEpochMillis = 1_700_000_100_000L,
        sourceTitle = " Meeting   with  吉米 黃 ",
        sourceContentText = "點一下以返回會議",
    )

    @Test
    fun supportsOnlyOfficialTeamsPackage() {
        assertTrue(TeamsCallNotificationParser.supportsPackage("com.microsoft.teams"))
        assertFalse(TeamsCallNotificationParser.supportsPackage("com.microsoft.teams.beta"))
    }

    @Test
    fun parsesObservedOngoingCallStructureAndKeepsStableSourceWhen() {
        val update = TeamsCallNotificationParser.parse(validSignals)

        assertEquals("0|com.microsoft.teams|-1|null|10838", update?.sourceKey)
        assertEquals(1_700_000_000_000L, update?.startedAtEpochMillis)
        assertEquals("Meeting with 吉米 黃", update?.sourceTitle)
        assertEquals("吉米 黃", update?.participantName)
        assertEquals(TeamsCallLanguage.TRADITIONAL_CHINESE, update?.language)
        assertEquals(
            android.app.NotificationManager.IMPORTANCE_DEFAULT,
            LiveStatusReminder.TEAMS_CALL_CHANNEL_IMPORTANCE,
        )
        assertEquals(Notification.VISIBILITY_PUBLIC, LiveStatusReminder.TEAMS_CALL_VISIBILITY)
    }

    @Test
    fun fallsBackToPostTimeWhenNotificationWhenIsInvalid() {
        val update = TeamsCallNotificationParser.parse(
            validSignals.copy(sourceWhenEpochMillis = 0L),
        )

        assertEquals(validSignals.postedAtEpochMillis, update?.startedAtEpochMillis)
    }

    @Test
    fun convertsWallClockStartToElapsedRealtimeForStopwatchMetric() {
        assertEquals(
            4_000L,
            TeamsCallNotificationStyle.elapsedRealtimeStartMillis(
                startedAtEpochMillis = 9_000L,
                nowEpochMillis = 10_000L,
                nowElapsedRealtimeMillis = 5_000L,
            ),
        )
    }

    @Test
    fun rejectsWrongChannelCategoryFlagsAndPromotedSource() {
        assertNull(TeamsCallNotificationParser.parse(validSignals.copy(channelId = "messages")))
        assertNull(
            TeamsCallNotificationParser.parse(
                validSignals.copy(category = Notification.CATEGORY_MESSAGE),
            ),
        )
        assertNull(TeamsCallNotificationParser.parse(validSignals.copy(isOngoing = false)))
        assertNull(
            TeamsCallNotificationParser.parse(validSignals.copy(isForegroundService = false)),
        )
        assertNull(TeamsCallNotificationParser.parse(validSignals.copy(isNoClear = false)))
        assertNull(TeamsCallNotificationParser.parse(validSignals.copy(isPromoted = true)))
    }

    @Test
    fun rejectsInvalidIdentityAndStartTime() {
        assertNull(TeamsCallNotificationParser.parse(validSignals.copy(sourceKey = "  ")))
        assertNull(
            TeamsCallNotificationParser.parse(
                validSignals.copy(sourceWhenEpochMillis = 0L, postedAtEpochMillis = 0L),
            ),
        )
    }

    @Test
    fun displayTextExtractsParticipantAndFallsBackSafely() {
        val named = requireNotNull(TeamsCallNotificationParser.parse(validSignals))
        val generic = requireNotNull(
            TeamsCallNotificationParser.parse(validSignals.copy(sourceTitle = "會議")),
        )
        val missing = requireNotNull(
            TeamsCallNotificationParser.parse(validSignals.copy(sourceTitle = "  ")),
        )

        assertEquals("吉米 黃", TeamsCallText.contentText(named, "會議進行中"))
        assertEquals("會議", TeamsCallText.contentText(generic, "會議進行中"))
        assertEquals("會議進行中", TeamsCallText.contentText(missing, "會議進行中"))
    }

    @Test
    fun normalizesOnlyObservedBrokenUnmuteLabel() {
        assertEquals(
            "Unmute",
            TeamsCallText.actionTitle("啟用通知", TeamsCallLanguage.ENGLISH),
        )
        assertEquals("Mute", TeamsCallText.actionTitle(" 靜音 ", TeamsCallLanguage.ENGLISH))
        assertEquals("Hang up", TeamsCallText.actionTitle("掛斷", TeamsCallLanguage.ENGLISH))
        assertEquals(
            "Custom action",
            TeamsCallText.actionTitle("Custom   action", TeamsCallLanguage.ENGLISH),
        )
        assertEquals(
            "解除靜音",
            TeamsCallText.actionTitle("Unmute", TeamsCallLanguage.TRADITIONAL_CHINESE),
        )
    }

    @Test
    fun detectsTraditionalChineseAndEnglishNotificationLanguage() {
        assertEquals(
            TeamsCallLanguage.TRADITIONAL_CHINESE,
            TeamsCallText.language("Meeting with 吉米 黃", "點一下以返回會議"),
        )
        assertEquals(
            TeamsCallLanguage.ENGLISH,
            TeamsCallText.language("Meeting with 吉米 黃", "Tap to return to the meeting"),
        )
        assertEquals(
            TeamsCallLanguage.ENGLISH,
            TeamsCallText.language("Weekly sync", "Tap to return to meeting"),
        )
    }

    @Test
    fun trackerUpdatesClearsAndIgnoresUnrelatedRemoval() {
        val tracker = TeamsCallTracker()
        val update = requireNotNull(TeamsCallNotificationParser.parse(validSignals))

        assertEquals(TeamsCallDecision.Show(update), tracker.onPosted(update.sourceKey, update))
        assertEquals(TeamsCallDecision.None, tracker.onRemoved("teams|other"))
        assertEquals(TeamsCallDecision.Clear, tracker.onPosted(update.sourceKey, null))
        assertEquals(TeamsCallDecision.None, tracker.onRemoved(update.sourceKey))
    }

    @Test
    fun trackerKeepsNewSessionAndRestoresNewestActiveCall() {
        val tracker = TeamsCallTracker()
        val older = requireNotNull(TeamsCallNotificationParser.parse(validSignals))
        val newer = older.copy(
            sourceKey = "teams|newer",
            startedAtEpochMillis = older.startedAtEpochMillis + 1_000L,
        )

        tracker.onPosted(older.sourceKey, older)
        assertEquals(TeamsCallDecision.Show(newer), tracker.onPosted(newer.sourceKey, newer))
        assertEquals(TeamsCallDecision.None, tracker.onRemoved(older.sourceKey))
        assertEquals(TeamsCallDecision.Clear, tracker.onRemoved(newer.sourceKey))
        assertEquals(TeamsCallDecision.Show(newer), tracker.restore(listOf(newer, older)))
        assertEquals(TeamsCallDecision.Clear, tracker.restore(emptyList()))
    }
}
