package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DiscordVoiceNotificationTest {
    private val validSignals = DiscordVoiceSignals(
        sourceKey = "discord|voice",
        channelId = DiscordVoiceNotificationParser.VOICE_CHANNEL_ID,
        isOngoing = true,
        isForegroundService = true,
        isNoClear = true,
        isPromoted = false,
        postedAtEpochMillis = 1_700_000_000_000L,
        sourceTitle = " 語音已連線 — 點選即可回到通話 ",
        sourceContentText = " [beta]   office hours ",
    )

    @Test
    fun supportsOnlyOfficialDiscordPackage() {
        assertTrue(DiscordVoiceNotificationParser.supportsPackage("com.discord"))
        assertEquals(false, DiscordVoiceNotificationParser.supportsPackage("com.discord.beta"))
    }

    @Test
    fun parsesObservedServerVoiceStructureWithoutDependingOnText() {
        val update = DiscordVoiceNotificationParser.parse(validSignals)

        assertEquals(
            android.app.NotificationManager.IMPORTANCE_DEFAULT,
            LiveStatusReminder.DISCORD_VOICE_CHANNEL_IMPORTANCE,
        )
        assertEquals("discord|voice", update?.sourceKey)
        assertEquals("語音已連線 — 點選即可回到通話", update?.sourceTitle)
        assertEquals("[beta] office hours", update?.sourceContentText)
        assertEquals(android.app.Notification.VISIBILITY_PUBLIC, LiveStatusReminder.DISCORD_VOICE_VISIBILITY)
    }

    @Test
    fun acceptsWaitingForVoiceServerAndMissingDisplayText() {
        assertTrue(
            DiscordVoiceNotificationParser.parse(
                validSignals.copy(sourceTitle = "Waiting for Voice Server"),
            ) != null,
        )
        assertTrue(
            DiscordVoiceNotificationParser.parse(
                validSignals.copy(sourceTitle = null, sourceContentText = "  "),
            ) != null,
        )
    }

    @Test
    fun displayTextMirrorsSourceAndFallsBackWhenMissing() {
        val update = requireNotNull(DiscordVoiceNotificationParser.parse(validSignals))
        val missingText = update.copy(sourceTitle = null, sourceContentText = null)

        assertEquals(
            "語音已連線 — 點選即可回到通話",
            LiveStatusReminder.discordVoiceTitle(update, "fallback title"),
        )
        assertEquals(
            "[beta] office hours",
            LiveStatusReminder.discordVoiceContentText(update, "fallback content"),
        )
        assertEquals(
            "fallback title",
            LiveStatusReminder.discordVoiceTitle(missingText, "fallback title"),
        )
        assertEquals(
            "fallback content",
            LiveStatusReminder.discordVoiceContentText(missingText, "fallback content"),
        )
    }

    @Test
    fun rejectsOtherChannelsAndAnyMissingRequiredFlag() {
        assertNull(DiscordVoiceNotificationParser.parse(validSignals.copy(channelId = "messages")))
        assertNull(DiscordVoiceNotificationParser.parse(validSignals.copy(isOngoing = false)))
        assertNull(
            DiscordVoiceNotificationParser.parse(validSignals.copy(isForegroundService = false)),
        )
        assertNull(DiscordVoiceNotificationParser.parse(validSignals.copy(isNoClear = false)))
    }

    @Test
    fun rejectsAlreadyPromotedAndInvalidIdentity() {
        assertNull(DiscordVoiceNotificationParser.parse(validSignals.copy(isPromoted = true)))
        assertNull(DiscordVoiceNotificationParser.parse(validSignals.copy(sourceKey = "  ")))
        assertNull(DiscordVoiceNotificationParser.parse(validSignals.copy(postedAtEpochMillis = 0)))
    }

    @Test
    fun trackerUpdatesClearsAndIgnoresUnrelatedRemoval() {
        val tracker = DiscordVoiceTracker()
        val update = requireNotNull(DiscordVoiceNotificationParser.parse(validSignals))

        assertEquals(DiscordVoiceDecision.Show(update), tracker.onPosted(update.sourceKey, update))
        assertEquals(DiscordVoiceDecision.None, tracker.onRemoved("discord|other"))
        assertEquals(DiscordVoiceDecision.Clear, tracker.onPosted(update.sourceKey, null))
        assertEquals(DiscordVoiceDecision.None, tracker.onRemoved(update.sourceKey))
    }

    @Test
    fun trackerKeepsNewSessionWhenOldNotificationIsRemoved() {
        val tracker = DiscordVoiceTracker()
        val older = requireNotNull(DiscordVoiceNotificationParser.parse(validSignals))
        val newer = older.copy(
            sourceKey = "discord|newer",
            postedAtEpochMillis = older.postedAtEpochMillis + 1_000,
        )

        tracker.onPosted(older.sourceKey, older)
        assertEquals(DiscordVoiceDecision.Show(newer), tracker.onPosted(newer.sourceKey, newer))
        assertEquals(DiscordVoiceDecision.None, tracker.onRemoved(older.sourceKey))
        assertEquals(DiscordVoiceDecision.Clear, tracker.onRemoved(newer.sourceKey))
    }

    @Test
    fun trackerRestoresNewestActiveNotification() {
        val tracker = DiscordVoiceTracker()
        val older = requireNotNull(DiscordVoiceNotificationParser.parse(validSignals))
        val newer = older.copy(
            sourceKey = "discord|newer",
            postedAtEpochMillis = older.postedAtEpochMillis + 1_000,
        )

        assertEquals(DiscordVoiceDecision.Show(newer), tracker.restore(listOf(newer, older)))
        assertEquals(DiscordVoiceDecision.Clear, tracker.restore(emptyList()))
    }
}
