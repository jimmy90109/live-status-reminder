package com.github.jimmy90109.livestatus

import android.media.session.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaPlaybackNotificationTest {
    @Test
    fun mediaPlaybackDefaultsOnOnlyForGoogleDevices() {
        assertTrue(MediaPlaybackDefaultPolicy.isDefaultEnabled("Google", "google"))
        assertTrue(MediaPlaybackDefaultPolicy.isDefaultEnabled(" google ", "pixel"))
        assertTrue(MediaPlaybackDefaultPolicy.isDefaultEnabled("unknown", "GOOGLE"))
        assertEquals(false, MediaPlaybackDefaultPolicy.isDefaultEnabled("samsung", "samsung"))
        assertEquals(false, MediaPlaybackDefaultPolicy.isDefaultEnabled("Xiaomi", "POCO"))
        assertEquals(false, MediaPlaybackDefaultPolicy.isDefaultEnabled("motorola", "motorola"))
    }

    @Test
    fun mediaCriticalTextKeepsTitlesUpToSevenCharacters() {
        assertEquals("123456", LiveStatusReminder.mediaShortCriticalText("123456"))
        assertEquals("1234567", LiveStatusReminder.mediaShortCriticalText("1234567"))
    }

    @Test
    fun mediaCriticalTextTruncatesLongTitleToSevenUnicodeCodePoints() {
        assertEquals(
            "一二三四五六七",
            LiveStatusReminder.mediaShortCriticalText("一二三四五六七八九"),
        )
        assertEquals(
            "🎵🎶abcde",
            LiveStatusReminder.mediaShortCriticalText("🎵🎶abcdefgh"),
        )
    }

    @Test
    fun selectsFirstPlayingSessionWithSourceNotification() {
        val selected = MediaPlaybackPolicy.select(
            candidates = listOf(
                candidate("paused", PlaybackState.STATE_PAUSED),
                candidate("playing", PlaybackState.STATE_PLAYING),
                candidate("later", PlaybackState.STATE_PLAYING),
            ),
            lastPlayingSessionKey = null,
        )

        assertEquals("playing", selected?.sessionKey)
    }

    @Test
    fun ignoresPlayingSessionWithoutMediaNotification() {
        val selected = MediaPlaybackPolicy.select(
            candidates = listOf(
                candidate(
                    sessionKey = "headless",
                    playbackState = PlaybackState.STATE_PLAYING,
                    hasSourceNotification = false,
                ),
            ),
            lastPlayingSessionKey = null,
        )

        assertNull(selected)
    }

    @Test
    fun promotedHighestPrioritySessionDoesNotFallThroughToLowerSession() {
        val selected = MediaPlaybackPolicy.select(
            candidates = listOf(
                candidate(
                    sessionKey = "promoted",
                    playbackState = PlaybackState.STATE_PLAYING,
                    sourcePromoted = true,
                ),
                candidate("lower", PlaybackState.STATE_PLAYING),
            ),
            lastPlayingSessionKey = null,
        )

        assertNull(selected)
    }

    @Test
    fun keepsBufferingOnlyForPreviouslyPlayingSession() {
        assertTrue(
            MediaPlaybackPolicy.isOngoingPlayback(
                PlaybackState.STATE_BUFFERING,
                sameSessionWasPlaying = true,
            ),
        )
        assertEquals(
            false,
            MediaPlaybackPolicy.isOngoingPlayback(
                PlaybackState.STATE_BUFFERING,
                sameSessionWasPlaying = false,
            ),
        )
        assertTrue(
            MediaPlaybackPolicy.isOngoingPlayback(
                PlaybackState.STATE_CONNECTING,
                sameSessionWasPlaying = true,
            ),
        )
    }

    @Test
    fun pausedStoppedAndErroredSessionsAreNotOngoing() {
        listOf(
            PlaybackState.STATE_PAUSED,
            PlaybackState.STATE_STOPPED,
            PlaybackState.STATE_ERROR,
            PlaybackState.STATE_NONE,
        ).forEach { state ->
            assertEquals(
                false,
                MediaPlaybackPolicy.isOngoingPlayback(state, sameSessionWasPlaying = true),
            )
        }
    }

    private fun candidate(
        sessionKey: String,
        playbackState: Int,
        hasSourceNotification: Boolean = true,
        sourcePromoted: Boolean = false,
    ) = MediaPlaybackCandidate(
        sessionKey = sessionKey,
        playbackState = playbackState,
        hasSourceNotification = hasSourceNotification,
        sourcePromoted = sourcePromoted,
    )
}
