package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StravaRecordingNotificationTest {
    @Test
    fun parsesObservedRecordingWaitingForGpsAndPausedPayloads() {
        val recording = parse("跑步 · 0:08 · 0 公里", null)
        val waitingForGps = parse("跑步 · 0:10 · 0 公里", "沒有 GPS")
        val paused = parse("跑步 · 0:53 · 0 公里", "已停止")

        assertEquals(StravaRecordingState.RECORDING, recording?.state)
        assertEquals(StravaRecordingState.WAITING_FOR_GPS, waitingForGps?.state)
        assertEquals(StravaRecordingState.PAUSED, paused?.state)
        assertEquals("跑步 · 0:53 · 0 公里", paused?.officialTitle)
        assertEquals("已停止", paused?.officialText)
        assertEquals(StravaRecordingLanguage.TRADITIONAL_CHINESE, paused?.language)
    }

    @Test
    fun parsesObservedEnglishRecordingAndNoGpsPayloads() {
        val recording = parse("Run · 0:41 · 0 km", null)
        val waitingForGps = parse("Run · 0:40 · 0 km", "No GPS")

        assertEquals(StravaRecordingLanguage.ENGLISH, recording?.language)
        assertEquals(StravaRecordingState.RECORDING, recording?.state)
        assertEquals(StravaRecordingLanguage.ENGLISH, waitingForGps?.language)
        assertEquals(StravaRecordingState.WAITING_FOR_GPS, waitingForGps?.state)
    }

    @Test
    fun ignoresUnrelatedMalformedAndUnsupportedPayloads() {
        assertNull(parse("", null))
        assertNull(parse("今天完成了一次跑步", null))
        assertNull(parse("今天完成了一次跑步", null, channelId = "social"))
        assertNull(parse("跑步 · 0:08 · 0 公里", null, isOngoing = false))
        assertNull(parse("跑步 · 0:08 · 0 公里", null, isForegroundService = false))
    }

    @Test
    fun trackerKeepsPausedRecordingAndClearsOnlyMatchingRemoval() {
        val tracker = StravaRecordingTracker()
        val recording = requireNotNull(parse("跑步 · 0:53 · 0 公里", "已停止"))

        assertTrue(tracker.onPosted(recording.sourceKey, recording) is StravaRecordingDecision.Show)
        assertEquals(StravaRecordingDecision.None, tracker.onRemoved("strava|other"))
        assertEquals(StravaRecordingDecision.Clear, tracker.onRemoved(recording.sourceKey))
    }

    @Test
    fun liveStatusPayloadCopiesOfficialTitleWithoutParsingDistance() {
        val update = requireNotNull(parse("跑步 · 0:53 · 0 公里", "已停止"))

        val payload = LiveStatusReminder.stravaRecordingPayload(update)

        assertEquals("Strava", payload.appName)
        assertEquals("已暫停", payload.criticalText)
        assertEquals("跑步 · 0:53 · 0 公里", payload.title)
        assertEquals("已停止", payload.contentText)
    }

    @Test
    fun recordingPayloadUsesUserFocusedCriticalText() {
        val update = requireNotNull(parse("跑步 · 0:53 · 0 公里", null))

        val payload = LiveStatusReminder.stravaRecordingPayload(update)

        assertEquals(R.drawable.ic_running_notification, payload.smallIconRes)
        assertEquals(R.drawable.ic_running_notification, payload.leftIconRes)
        assertEquals("運動中", payload.criticalText)
    }

    @Test
    fun englishPayloadUsesEnglishStatusCopy() {
        val recording = requireNotNull(parse("Run · 0:41 · 0 km", null))
        val waitingForGps = requireNotNull(parse("Run · 0:40 · 0 km", "No GPS"))
        val paused = requireNotNull(parse("Run · 0:53 · 0 km", "Paused"))

        assertEquals("Active", LiveStatusReminder.stravaRecordingPayload(recording).criticalText)
        assertEquals(
            "Strava activity in progress",
            LiveStatusReminder.stravaRecordingPayload(recording).contentText,
        )
        assertEquals(
            "Locating",
            LiveStatusReminder.stravaRecordingPayload(waitingForGps).criticalText,
        )
        assertEquals("Paused", LiveStatusReminder.stravaRecordingPayload(paused).criticalText)
    }

    private fun parse(
        title: String?,
        contentText: String?,
        channelId: String = StravaRecordingNotificationParser.RECORDING_CHANNEL_ID,
        isOngoing: Boolean = true,
        isForegroundService: Boolean = true,
    ): StravaRecordingUpdate? = StravaRecordingNotificationParser.parse(
        sourceKey = "strava|recording",
        channelId = channelId,
        isOngoing = isOngoing,
        isForegroundService = isForegroundService,
        notificationTitle = title,
        notificationContentText = contentText,
    )
}
