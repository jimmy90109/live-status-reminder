package com.github.jimmy90109.livestatus

import android.app.PendingIntent
import androidx.annotation.DrawableRes

data class LiveStatusPayload(
    val id: Int,
    val appName: String,
    @param:DrawableRes val smallIconRes: Int,
    @param:DrawableRes val leftIconRes: Int,
    val criticalText: String,
    val title: String,
    val contentText: String,
    val progress: Int? = null,
    val timer: LiveStatusTimer? = null,
    val contentIntent: PendingIntent? = null,
)

data class LiveStatusTimer(
    val state: ClockTimerState,
    val endElapsedRealtimeMillis: Long? = null,
    val remainingMillis: Long? = null,
    val language: ClockTimerLanguage = ClockTimerLanguage.ENGLISH,
) {
    init {
        require(
            (state == ClockTimerState.RUNNING && endElapsedRealtimeMillis != null && remainingMillis == null) ||
                (state == ClockTimerState.PAUSED && remainingMillis != null && endElapsedRealtimeMillis == null),
        ) { "Running timers need an end time; paused timers need a remaining duration." }
    }
}
