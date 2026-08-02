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
    fun mediaCriticalTextKeepsNarrowTitlesUpToSevenGraphemes() {
        assertEquals("123456", LiveStatusReminder.mediaShortCriticalText("123456"))
        assertEquals("1234567", LiveStatusReminder.mediaShortCriticalText("1234567"))
        assertEquals("Infohaz", LiveStatusReminder.mediaShortCriticalText("Infohaz"))
    }

    @Test
    fun mediaCriticalTextLimitsWideTitlesToFourGraphemes() {
        assertEquals("一二三四", LiveStatusReminder.mediaShortCriticalText("一二三四"))
        assertEquals(
            "一二三四",
            LiveStatusReminder.mediaShortCriticalText("一二三四五六七八九"),
        )
    }

    @Test
    fun mediaCriticalTextUsesAdaptiveWidthForMixedTitles() {
        assertEquals("ABC中文", LiveStatusReminder.mediaShortCriticalText("ABC中文歌曲"))
        assertEquals(
            "🎵🎶abcd",
            LiveStatusReminder.mediaShortCriticalText("🎵🎶abcdefgh"),
        )
    }

    @Test
    fun mediaCriticalTextDoesNotSplitUnicodeGraphemes() {
        val combiningTitle = "e\u0301e\u0301e\u0301e\u0301e\u0301e\u0301e\u0301e\u0301"
        val familyEmoji = "👨‍👩‍👧‍👦"

        assertEquals(
            "e\u0301e\u0301e\u0301e\u0301e\u0301e\u0301e\u0301",
            LiveStatusReminder.mediaShortCriticalText(combiningTitle),
        )
        assertEquals(
            familyEmoji + "abcdef",
            LiveStatusReminder.mediaShortCriticalText(familyEmoji + "abcdefgh"),
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
        listOf(
            PlaybackState.STATE_FAST_FORWARDING,
            PlaybackState.STATE_REWINDING,
            PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
            PlaybackState.STATE_SKIPPING_TO_NEXT,
            PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM,
        ).forEach { state ->
            assertTrue(
                MediaPlaybackPolicy.isOngoingPlayback(
                    state,
                    sameSessionWasPlaying = true,
                ),
            )
        }
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

    @Test
    fun retainsOnlyPreviouslyPlayingPausedSession() {
        assertTrue(
            MediaPlaybackPolicy.canRetainPaused(
                PlaybackState.STATE_PAUSED,
                sameSessionWasPlaying = true,
            ),
        )
        assertEquals(
            false,
            MediaPlaybackPolicy.canRetainPaused(
                PlaybackState.STATE_PAUSED,
                sameSessionWasPlaying = false,
            ),
        )
        assertEquals(
            false,
            MediaPlaybackPolicy.canRetainPaused(
                PlaybackState.STATE_STOPPED,
                sameSessionWasPlaying = true,
            ),
        )
    }

    @Test
    fun pauseGracePeriodExpiresAfterOneMinuteWithoutBeingReset() {
        val gracePeriod = MediaPauseGracePeriod(durationMillis = 60_000L)

        assertEquals(60_000L, gracePeriod.remainingMillis("session", nowMillis = 1_000L))
        assertEquals(1_000L, gracePeriod.remainingMillis("session", nowMillis = 60_000L))
        assertEquals(0L, gracePeriod.remainingMillis("session", nowMillis = 61_000L))
    }

    @Test
    fun pauseGracePeriodRestartsForDifferentSessionOrAfterClear() {
        val gracePeriod = MediaPauseGracePeriod(durationMillis = 60_000L)

        gracePeriod.remainingMillis("first", nowMillis = 1_000L)
        assertEquals(60_000L, gracePeriod.remainingMillis("second", nowMillis = 50_000L))
        gracePeriod.clear()
        assertEquals(60_000L, gracePeriod.remainingMillis("second", nowMillis = 100_000L))
    }

    @Test
    fun timeControlsWithSeekTakePriorityOverTrackSkipping() {
        val controls = MediaPlaybackControlPolicy.select(
            PlaybackState.ACTION_REWIND or
                PlaybackState.ACTION_FAST_FORWARD or
                PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SKIP_TO_NEXT,
        )

        assertEquals(MediaPlaybackControl.SEEK_BACK_15_SECONDS, controls.left)
        assertEquals(MediaPlaybackControl.SEEK_FORWARD_15_SECONDS, controls.right)
    }

    @Test
    fun sourceTimeControlsAreUsedWhenExactSeekIsUnavailable() {
        val controls = MediaPlaybackControlPolicy.select(
            PlaybackState.ACTION_REWIND or PlaybackState.ACTION_FAST_FORWARD,
        )

        assertEquals(MediaPlaybackControl.REWIND, controls.left)
        assertEquals(MediaPlaybackControl.FAST_FORWARD, controls.right)
    }

    @Test
    fun seekOnlySessionGetsFifteenSecondControls() {
        val controls = MediaPlaybackControlPolicy.select(PlaybackState.ACTION_SEEK_TO)

        assertEquals(MediaPlaybackControl.SEEK_BACK_15_SECONDS, controls.left)
        assertEquals(MediaPlaybackControl.SEEK_FORWARD_15_SECONDS, controls.right)
    }

    @Test
    fun trackSkippingRemainsWhenSeekHasNoTimeControlSignal() {
        val controls = MediaPlaybackControlPolicy.select(
            PlaybackState.ACTION_SEEK_TO or
                PlaybackState.ACTION_SKIP_TO_PREVIOUS or
                PlaybackState.ACTION_SKIP_TO_NEXT,
        )

        assertEquals(MediaPlaybackControl.SKIP_PREVIOUS, controls.left)
        assertEquals(MediaPlaybackControl.SKIP_NEXT, controls.right)
    }

    @Test
    fun exactSeekUsesEstimatedPlayingPositionAndClampsToDuration() {
        assertEquals(
            25_000L,
            MediaPlaybackSeekPosition.calculate(
                positionMillis = 10_000L,
                lastPositionUpdateElapsedMillis = 1_000L,
                playbackSpeed = 1f,
                playbackState = PlaybackState.STATE_PLAYING,
                nowElapsedMillis = 6_000L,
                offsetMillis = 10_000L,
                durationMillis = 60_000L,
            ),
        )
        assertEquals(
            60_000L,
            MediaPlaybackSeekPosition.calculate(
                positionMillis = 55_000L,
                lastPositionUpdateElapsedMillis = 1_000L,
                playbackSpeed = 1f,
                playbackState = PlaybackState.STATE_PAUSED,
                nowElapsedMillis = 20_000L,
                offsetMillis = 15_000L,
                durationMillis = 60_000L,
            ),
        )
    }

    @Test
    fun exactSeekClampsBackToZeroAndRejectsUnknownPosition() {
        assertEquals(
            0L,
            MediaPlaybackSeekPosition.calculate(
                positionMillis = 5_000L,
                lastPositionUpdateElapsedMillis = 1_000L,
                playbackSpeed = 0f,
                playbackState = PlaybackState.STATE_PAUSED,
                nowElapsedMillis = 10_000L,
                offsetMillis = -15_000L,
                durationMillis = null,
            ),
        )
        assertNull(
            MediaPlaybackSeekPosition.calculate(
                positionMillis = PlaybackState.PLAYBACK_POSITION_UNKNOWN,
                lastPositionUpdateElapsedMillis = 0L,
                playbackSpeed = 1f,
                playbackState = PlaybackState.STATE_PLAYING,
                nowElapsedMillis = 10_000L,
                offsetMillis = 15_000L,
                durationMillis = null,
            ),
        )
    }

    @Test
    fun mediaProgressUsesValidPositionAndDuration() {
        assertEquals(0, MediaPlaybackProgress.percent(0L, 240_000L))
        assertEquals(25, MediaPlaybackProgress.percent(60_000L, 240_000L))
        assertEquals(100, MediaPlaybackProgress.percent(300_000L, 240_000L))
    }

    @Test
    fun mediaProgressRequiresKnownPositiveDurationAndPosition() {
        assertNull(MediaPlaybackProgress.percent(null, 240_000L))
        assertNull(MediaPlaybackProgress.percent(60_000L, null))
        assertNull(MediaPlaybackProgress.percent(60_000L, 0L))
    }

    @Test
    fun mediaTextJoinsArtistAndAlbumWithHyphen() {
        assertEquals(
            "Arctic Monkeys - AM",
            MediaPlaybackText.artistAndAlbum("Arctic Monkeys", "AM"),
        )
        assertEquals("Arctic Monkeys", MediaPlaybackText.artistAndAlbum("Arctic Monkeys", null))
        assertEquals("AM", MediaPlaybackText.artistAndAlbum(null, "AM"))
    }

    @Test
    fun mediaTextCleansAndDeduplicatesArtistAndAlbum() {
        assertEquals("Artist", MediaPlaybackText.artistAndAlbum(" Artist ", "artist"))
        assertNull(MediaPlaybackText.artistAndAlbum(" ", null))
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
