package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.service.notification.StatusBarNotification

internal enum class RecorderState {
    RUNNING,
    PAUSED,
}

internal enum class RecorderLanguage {
    TRADITIONAL_CHINESE,
    ENGLISH,
}

internal enum class RecorderParsedEvent {
    PREPARING,
    RUNNING,
    PAUSED,
    NONE,
}

internal data class RecorderUpdate(
    val sourceKey: String,
    val state: RecorderState,
    val elapsedMillis: Long,
    val startedAtEpochMillis: Long?,
    val postedAtEpochMillis: Long,
    val language: RecorderLanguage = RecorderLanguage.TRADITIONAL_CHINESE,
    val contentIntent: PendingIntent? = null,
    val sourceActions: List<Notification.Action> = emptyList(),
) {
    init {
        require(elapsedMillis >= 0L) { "Recorded duration must not be negative." }
        require(
            (state == RecorderState.RUNNING && startedAtEpochMillis != null) ||
                (state == RecorderState.PAUSED && startedAtEpochMillis == null),
        ) { "Running recordings need a start time; paused recordings must use a fixed duration." }
    }
}

internal data class RecorderSignals(
    val channelId: String?,
    val isOngoing: Boolean,
    val isForegroundService: Boolean,
    val notificationText: String?,
    val sourceAlreadyPromoted: Boolean = false,
)

internal data class RecorderParseResult(
    val event: RecorderParsedEvent,
    val elapsedMillis: Long? = null,
    val language: RecorderLanguage? = null,
)

internal data class RecorderExtraction(
    val event: RecorderParsedEvent,
    val update: RecorderUpdate?,
    val diagnostics: Map<String, String>,
)

internal object GoogleRecorderNotificationParser {
    const val PACKAGE_NAME = "com.google.android.apps.recorder"
    const val RECORD_CHANNEL_ID = "Record"

    private val preparingTexts = mapOf(
        "Prepare to record" to RecorderLanguage.ENGLISH,
        "準備錄音" to RecorderLanguage.TRADITIONAL_CHINESE,
    )
    private val runningTexts = mapOf(
        "Currently recording" to RecorderLanguage.ENGLISH,
        "正在錄音" to RecorderLanguage.TRADITIONAL_CHINESE,
    )
    private val pausedTexts = mapOf(
        "Recording paused" to RecorderLanguage.ENGLISH,
        "已暫停錄音" to RecorderLanguage.TRADITIONAL_CHINESE,
    )
    private val resumeActionTexts = setOf("Resume recording", "繼續錄製")
    private val saveActionTexts = setOf("Save recording", "儲存錄製內容")
    private val durationPattern = Regex("""^(\d+):([0-5]\d)(?::([0-5]\d))?$""")

    fun supportsPackage(packageName: String): Boolean = packageName == PACKAGE_NAME

    fun parse(signals: RecorderSignals): RecorderParseResult {
        if (signals.channelId != RECORD_CHANNEL_ID) return RecorderParseResult(RecorderParsedEvent.NONE)
        if (!signals.isOngoing || !signals.isForegroundService || signals.sourceAlreadyPromoted) {
            return RecorderParseResult(RecorderParsedEvent.NONE)
        }

        val lines = signals.notificationText
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.toList()
            .orEmpty()
        val (event, language) = findEvent(lines)
        if (event == RecorderParsedEvent.NONE) return RecorderParseResult(event)

        val elapsedMillis = lines.firstNotNullOfOrNull(::parseDurationMillis)
            ?: return RecorderParseResult(RecorderParsedEvent.NONE)
        return RecorderParseResult(event, elapsedMillis, language)
    }

    private fun findEvent(lines: List<String>): Pair<RecorderParsedEvent, RecorderLanguage?> {
        listOf(
            RecorderParsedEvent.PAUSED to pausedTexts,
            RecorderParsedEvent.RUNNING to runningTexts,
            RecorderParsedEvent.PREPARING to preparingTexts,
        ).forEach { (event, texts) ->
            lines.forEach { line ->
                texts.entries.firstOrNull { (text) -> line.equals(text, ignoreCase = true) }
                    ?.let { return event to it.value }
            }
        }
        return RecorderParsedEvent.NONE to null
    }

    internal fun parseDurationMillis(value: String): Long? {
        val match = durationPattern.matchEntire(value.trim()) ?: return null
        val first = match.groupValues[1].toLongOrNull() ?: return null
        val second = match.groupValues[2].toLongOrNull() ?: return null
        val third = match.groupValues[3].takeIf(String::isNotEmpty)?.toLongOrNull()
        return runCatching {
            val seconds = if (third == null) {
                Math.addExact(Math.multiplyExact(first, 60L), second)
            } else {
                Math.addExact(
                    Math.addExact(Math.multiplyExact(first, 3_600L), second * 60L),
                    third,
                )
            }
            Math.multiplyExact(seconds, 1_000L)
        }.getOrNull()
    }

    internal fun calculateStartedAtEpochMillis(
        nowEpochMillis: Long,
        elapsedMillis: Long,
    ): Long? = if (elapsedMillis in 0..nowEpochMillis) {
        nowEpochMillis - elapsedMillis
    } else {
        null
    }

    internal fun isMirroredPausedAction(title: String): Boolean =
        resumeActionTexts.any { title.equals(it, ignoreCase = true) } ||
            saveActionTexts.any { title.equals(it, ignoreCase = true) }
}

internal object GoogleRecorderNotificationExtractor {
    fun extract(
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        nowEpochMillis: Long = System.currentTimeMillis(),
    ): RecorderExtraction {
        if (!GoogleRecorderNotificationParser.supportsPackage(statusBarNotification.packageName)) {
            return RecorderExtraction(
                event = RecorderParsedEvent.NONE,
                update = null,
                diagnostics = mapOf("reason" to "unsupported_package"),
            )
        }

        val notification = statusBarNotification.notification
        val promotionState = ClockTimerPromotionPolicy.evaluate(notification)
        val signals = RecorderSignals(
            channelId = notification.channelId,
            isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            isForegroundService = notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            notificationText = notificationText,
            sourceAlreadyPromoted = promotionState.promoted,
        )
        val result = GoogleRecorderNotificationParser.parse(signals)
        val elapsedMillis = result.elapsedMillis
        val update = if (
            elapsedMillis != null && elapsedMillis <= nowEpochMillis &&
            result.event in setOf(RecorderParsedEvent.RUNNING, RecorderParsedEvent.PAUSED)
        ) {
            val state = if (result.event == RecorderParsedEvent.RUNNING) {
                RecorderState.RUNNING
            } else {
                RecorderState.PAUSED
            }
            RecorderUpdate(
                sourceKey = statusBarNotification.key,
                state = state,
                elapsedMillis = elapsedMillis,
                language = requireNotNull(result.language),
                startedAtEpochMillis = if (state == RecorderState.RUNNING) {
                    GoogleRecorderNotificationParser.calculateStartedAtEpochMillis(
                        nowEpochMillis,
                        elapsedMillis,
                    )
                } else {
                    null
                },
                postedAtEpochMillis = statusBarNotification.postTime,
                contentIntent = notification.contentIntent,
                sourceActions = if (state == RecorderState.PAUSED) {
                    notification.actions.orEmpty().filter { action ->
                        action.title.toString().trim().let { title ->
                            GoogleRecorderNotificationParser.isMirroredPausedAction(title)
                        }
                    }
                } else {
                    emptyList()
                },
            )
        } else {
            null
        }
        return RecorderExtraction(
            event = if (elapsedMillis != null && elapsedMillis > nowEpochMillis) {
                RecorderParsedEvent.NONE
            } else {
                result.event
            },
            update = update,
            diagnostics = linkedMapOf(
                "channelId" to signals.channelId.orEmpty(),
                "isOngoing" to signals.isOngoing.toString(),
                "isForegroundService" to signals.isForegroundService.toString(),
                "sourcePromotedOngoing" to promotionState.promoted.toString(),
                "elapsedMillis" to elapsedMillis?.toString().orEmpty(),
                "language" to result.language?.name.orEmpty(),
                "mirroredActionCount" to update?.sourceActions?.size?.toString().orEmpty(),
            ),
        )
    }
}

internal sealed interface RecorderDecision {
    data class Show(val update: RecorderUpdate) : RecorderDecision
    data object Clear : RecorderDecision
    data object None : RecorderDecision
}

internal class RecorderTracker {
    private var activeSourceKey: String? = null

    fun onPosted(sourceKey: String, extraction: RecorderExtraction): RecorderDecision {
        extraction.update?.let { update ->
            activeSourceKey = sourceKey
            return RecorderDecision.Show(update)
        }
        if (extraction.event == RecorderParsedEvent.PREPARING) {
            if (activeSourceKey != sourceKey) return RecorderDecision.None
            activeSourceKey = null
            return RecorderDecision.Clear
        }
        if (activeSourceKey == sourceKey) {
            activeSourceKey = null
            return RecorderDecision.Clear
        }
        return RecorderDecision.None
    }

    fun onRemoved(sourceKey: String): RecorderDecision {
        if (activeSourceKey != sourceKey) return RecorderDecision.None
        activeSourceKey = null
        return RecorderDecision.Clear
    }

    fun restore(updates: List<RecorderUpdate>): RecorderDecision {
        val update = updates.maxByOrNull(RecorderUpdate::postedAtEpochMillis)
        activeSourceKey = update?.sourceKey
        return if (update == null) RecorderDecision.Clear else RecorderDecision.Show(update)
    }

    fun reset(): RecorderDecision {
        val hadActiveRecording = activeSourceKey != null
        activeSourceKey = null
        return if (hadActiveRecording) RecorderDecision.Clear else RecorderDecision.None
    }
}
