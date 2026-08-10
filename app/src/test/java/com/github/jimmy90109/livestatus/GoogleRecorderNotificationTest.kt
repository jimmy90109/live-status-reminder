package com.github.jimmy90109.livestatus

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GoogleRecorderNotificationTest {
    private val validSignals = RecorderSignals(
        channelId = GoogleRecorderNotificationParser.RECORD_CHANNEL_ID,
        isOngoing = true,
        isForegroundService = true,
        notificationText = "Currently recording\n00:07\n",
    )

    @Test
    fun supportsOnlyGoogleRecorderPackage() {
        assertTrue(
            GoogleRecorderNotificationParser.supportsPackage(
                "com.google.android.apps.recorder",
            ),
        )
        assertEquals(
            false,
            GoogleRecorderNotificationParser.supportsPackage("com.example.recorder"),
        )
    }

    @Test
    fun parsesObservedPreparingRunningAndPausedPayloads() {
        assertEquals(
            RecorderParseResult(RecorderParsedEvent.PREPARING, 0L, RecorderLanguage.ENGLISH),
            parse("Prepare to record\n00:00\n"),
        )
        assertEquals(
            RecorderParseResult(RecorderParsedEvent.RUNNING, 7_000L, RecorderLanguage.ENGLISH),
            parse("Currently recording\n00:07\n"),
        )
        assertEquals(
            RecorderParseResult(RecorderParsedEvent.PAUSED, 7_000L, RecorderLanguage.ENGLISH),
            parse("Recording paused\n00:07\n"),
        )
        assertEquals(
            RecorderParseResult(
                RecorderParsedEvent.PREPARING,
                0L,
                RecorderLanguage.TRADITIONAL_CHINESE,
            ),
            parse("準備錄音\n00:00\n"),
        )
        assertEquals(
            RecorderParseResult(
                RecorderParsedEvent.RUNNING,
                2_000L,
                RecorderLanguage.TRADITIONAL_CHINESE,
            ),
            parse("正在錄音\n00:02\n"),
        )
        assertEquals(
            RecorderParseResult(
                RecorderParsedEvent.PAUSED,
                2_000L,
                RecorderLanguage.TRADITIONAL_CHINESE,
            ),
            parse("已暫停錄音\n00:02\n"),
        )
    }

    @Test
    fun acceptsWhitespaceCaseAndLongRecordingDuration() {
        assertEquals(
            RecorderParseResult(RecorderParsedEvent.RUNNING, 7_000L, RecorderLanguage.ENGLISH),
            parse("  currently RECORDING  \n 00:07 "),
        )
        assertEquals(
            RecorderParseResult(
                RecorderParsedEvent.PAUSED,
                3_723_000L,
                RecorderLanguage.ENGLISH,
            ),
            parse("Recording paused\n1:02:03"),
        )
        assertEquals(
            3_600_000L,
            GoogleRecorderNotificationParser.parseDurationMillis("60:00"),
        )
    }

    @Test
    fun recognizesObservedEnglishAndTraditionalChinesePausedActions() {
        listOf(
            "Resume recording",
            "Save recording",
            "繼續錄製",
            "儲存錄製內容",
        ).forEach { title ->
            assertTrue(GoogleRecorderNotificationParser.isMirroredPausedAction(title))
        }
        assertEquals(
            false,
            GoogleRecorderNotificationParser.isMirroredPausedAction("Delete recording"),
        )
    }

    @Test
    fun ignoresBlankUnrelatedAndMalformedPayloads() {
        listOf(
            null,
            "",
            "Recorder is ready\n00:07",
            "Currently recording",
            "Currently recording\n00:60",
            "Currently recording\n1:60:00",
            "Currently recording\n-1:00",
            "Currently recording\n9223372036854775807:00",
        ).forEach { text ->
            assertEquals(RecorderParsedEvent.NONE, parse(text).event)
        }
    }

    @Test
    fun requiresObservedChannelAndForegroundOngoingFlags() {
        assertEquals(
            RecorderParsedEvent.NONE,
            GoogleRecorderNotificationParser.parse(
                validSignals.copy(channelId = "General"),
            ).event,
        )
        assertEquals(
            RecorderParsedEvent.NONE,
            GoogleRecorderNotificationParser.parse(
                validSignals.copy(isOngoing = false),
            ).event,
        )
        assertEquals(
            RecorderParsedEvent.NONE,
            GoogleRecorderNotificationParser.parse(
                validSignals.copy(isForegroundService = false),
            ).event,
        )
        assertEquals(
            RecorderParsedEvent.NONE,
            GoogleRecorderNotificationParser.parse(
                validSignals.copy(sourceAlreadyPromoted = true),
            ).event,
        )
    }

    @Test
    fun resumedRecordingDerivesStartFromRecordedDuration() {
        val beforePauseStart = GoogleRecorderNotificationParser.calculateStartedAtEpochMillis(
            nowEpochMillis = 10_000L,
            elapsedMillis = 7_000L,
        )
        val afterPauseStart = GoogleRecorderNotificationParser.calculateStartedAtEpochMillis(
            nowEpochMillis = 20_000L,
            elapsedMillis = 7_000L,
        )

        assertEquals(3_000L, beforePauseStart)
        assertEquals(13_000L, afterPauseStart)
        assertNull(
            GoogleRecorderNotificationParser.calculateStartedAtEpochMillis(
                nowEpochMillis = 5_000L,
                elapsedMillis = 7_000L,
            ),
        )
    }

    @Test
    fun trackerHandlesPrepareTransitionsRemovalAndRestore() {
        val tracker = RecorderTracker()
        val preparing = extraction(RecorderParsedEvent.PREPARING)
        val running = update("recorder|current", RecorderState.RUNNING, postedAt = 1_000L)
        val paused = update("recorder|current", RecorderState.PAUSED, postedAt = 2_000L)

        assertEquals(RecorderDecision.None, tracker.onPosted("recorder|current", preparing))
        assertEquals(
            RecorderDecision.Show(running),
            tracker.onPosted(running.sourceKey, extraction(running)),
        )
        assertEquals(
            RecorderDecision.Show(paused),
            tracker.onPosted(paused.sourceKey, extraction(paused)),
        )
        assertEquals(
            RecorderDecision.Clear,
            tracker.onPosted(paused.sourceKey, preparing),
        )
        tracker.onPosted(paused.sourceKey, extraction(paused))
        assertEquals(RecorderDecision.None, tracker.onRemoved("recorder|other"))
        assertEquals(RecorderDecision.Clear, tracker.onRemoved(paused.sourceKey))
        assertEquals(RecorderDecision.None, tracker.onRemoved(paused.sourceKey))

        val newer = running.copy(sourceKey = "recorder|newer", postedAtEpochMillis = 3_000L)
        assertEquals(RecorderDecision.Show(newer), tracker.restore(listOf(running, newer)))
        assertEquals(RecorderDecision.None, tracker.onRemoved(running.sourceKey))
        assertEquals(RecorderDecision.Clear, tracker.onRemoved(newer.sourceKey))
        assertEquals(RecorderDecision.Clear, tracker.restore(emptyList()))
    }

    @Test
    fun trackerClearsWhenCurrentNotificationStopsMatching() {
        val tracker = RecorderTracker()
        val running = update("recorder|current", RecorderState.RUNNING, postedAt = 1_000L)
        tracker.onPosted(running.sourceKey, extraction(running))

        assertEquals(
            RecorderDecision.Clear,
            tracker.onPosted(running.sourceKey, extraction(RecorderParsedEvent.NONE)),
        )
    }

    private fun parse(text: String?): RecorderParseResult =
        GoogleRecorderNotificationParser.parse(validSignals.copy(notificationText = text))

    private fun update(
        sourceKey: String,
        state: RecorderState,
        postedAt: Long,
    ): RecorderUpdate = RecorderUpdate(
        sourceKey = sourceKey,
        state = state,
        elapsedMillis = 7_000L,
        startedAtEpochMillis = if (state == RecorderState.RUNNING) 3_000L else null,
        postedAtEpochMillis = postedAt,
    )

    private fun extraction(update: RecorderUpdate): RecorderExtraction = RecorderExtraction(
        event = if (update.state == RecorderState.RUNNING) {
            RecorderParsedEvent.RUNNING
        } else {
            RecorderParsedEvent.PAUSED
        },
        update = update,
        diagnostics = emptyMap(),
    )

    private fun extraction(event: RecorderParsedEvent): RecorderExtraction = RecorderExtraction(
        event = event,
        update = null,
        diagnostics = emptyMap(),
    )
}
