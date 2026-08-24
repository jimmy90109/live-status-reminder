package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent

internal enum class StravaRecordingState {
    RECORDING,
    WAITING_FOR_GPS,
    PAUSED,
}

internal enum class StravaRecordingLanguage {
    TRADITIONAL_CHINESE,
    ENGLISH,
}

internal data class StravaRecordingUpdate(
    val sourceKey: String,
    val state: StravaRecordingState,
    val language: StravaRecordingLanguage,
    val officialTitle: String,
    val officialText: String?,
    val contentIntent: PendingIntent? = null,
    val sourceActions: List<Notification.Action> = emptyList(),
)

internal object StravaRecordingNotificationParser {
    const val PACKAGE_NAME = "com.strava"
    const val RECORDING_CHANNEL_ID = "recording"

    fun parse(
        sourceKey: String,
        channelId: String?,
        isOngoing: Boolean,
        isForegroundService: Boolean,
        notificationTitle: String?,
        notificationContentText: String?,
        contentIntent: PendingIntent? = null,
        sourceActions: List<Notification.Action> = emptyList(),
    ): StravaRecordingUpdate? {
        if (channelId != RECORDING_CHANNEL_ID || !isOngoing || !isForegroundService) return null
        val title = notificationTitle?.trim()?.takeIf(String::isNotEmpty) ?: return null
        val titleParts = title.split('·').map(String::trim)
        if (titleParts.size != 3 || titleParts.any(String::isEmpty)) return null
        val contentText = notificationContentText?.trim()?.takeIf(String::isNotEmpty)
        val isEnglish = contentText.equals("No GPS", ignoreCase = true) ||
            contentText.equals("Paused", ignoreCase = true) ||
            contentText.equals("Stopped", ignoreCase = true) ||
            sourceActions.any { action ->
                action.title.toString().trim().let { title ->
                    title.equals("Stop", ignoreCase = true) ||
                        title.equals("Start", ignoreCase = true) ||
                        title.equals("Resume", ignoreCase = true)
                }
            } || titleParts.first().any { it in 'A'..'Z' || it in 'a'..'z' }
        val state = when {
            contentText == "已停止" ||
                contentText.equals("Paused", ignoreCase = true) ||
                contentText.equals("Stopped", ignoreCase = true) -> StravaRecordingState.PAUSED
            contentText == "沒有 GPS" ||
                contentText.equals("No GPS", ignoreCase = true) ->
                StravaRecordingState.WAITING_FOR_GPS
            else -> StravaRecordingState.RECORDING
        }
        return StravaRecordingUpdate(
            sourceKey = sourceKey,
            state = state,
            language = if (isEnglish) {
                StravaRecordingLanguage.ENGLISH
            } else {
                StravaRecordingLanguage.TRADITIONAL_CHINESE
            },
            officialTitle = title,
            officialText = contentText,
            contentIntent = contentIntent,
            sourceActions = sourceActions,
        )
    }
}

internal sealed interface StravaRecordingDecision {
    data class Show(val update: StravaRecordingUpdate) : StravaRecordingDecision
    data object Clear : StravaRecordingDecision
    data object None : StravaRecordingDecision
}

internal class StravaRecordingTracker {
    private var activeSourceKey: String? = null

    fun onPosted(sourceKey: String, update: StravaRecordingUpdate?): StravaRecordingDecision {
        if (update != null) {
            activeSourceKey = sourceKey
            return StravaRecordingDecision.Show(update)
        }
        if (activeSourceKey == sourceKey) {
            activeSourceKey = null
            return StravaRecordingDecision.Clear
        }
        return StravaRecordingDecision.None
    }

    fun onRemoved(sourceKey: String): StravaRecordingDecision {
        if (activeSourceKey != sourceKey) return StravaRecordingDecision.None
        activeSourceKey = null
        return StravaRecordingDecision.Clear
    }

    fun restore(updates: List<StravaRecordingUpdate>): StravaRecordingDecision {
        val update = updates.firstOrNull()
        activeSourceKey = update?.sourceKey
        return if (update == null) {
            StravaRecordingDecision.Clear
        } else {
            StravaRecordingDecision.Show(update)
        }
    }

    fun reset() {
        activeSourceKey = null
    }
}
