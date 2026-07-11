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
    val contentIntent: PendingIntent? = null,
)
