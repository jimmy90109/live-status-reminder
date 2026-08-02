package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri

object YouBikeFeedbackPromptStore {
    private const val PREFERENCES = "you_bike_feedback_prompts"
    private const val KEY_INDEX_VERSION = "station_index_version"
    private const val KEY_STATION_HASHES = "prompted_station_hashes"

    fun shouldPromptAndMark(context: Context, report: YouBikeFeedbackReport): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val state = YouBikeFeedbackPromptState(
            stationIndexVersion = preferences.getString(KEY_INDEX_VERSION, null),
            promptedStationHashes = preferences.getStringSet(KEY_STATION_HASHES, emptySet()).orEmpty(),
        )
        val decision = YouBikeFeedbackDedupPolicy.markPrompted(
            state = state,
            stationIndexVersion = report.stationIndexVersion,
            normalizedStationName = report.normalizedStationName,
        )
        if (!decision.shouldPrompt) return false
        return preferences.edit()
            .putString(KEY_INDEX_VERSION, decision.updatedState.stationIndexVersion)
            .putStringSet(KEY_STATION_HASHES, decision.updatedState.promptedStationHashes)
            .commit()
    }
}

object YouBikeFeedbackNotifier {
    private const val CHANNEL_ID = "you_bike_feedback"
    private const val NOTIFICATION_ID = 109
    private const val PENDING_INTENT_REQUEST_CODE = 109
    private const val TIMEOUT_MILLIS = 24 * 60 * 60_000L

    fun show(context: Context, report: YouBikeFeedbackReport) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        if (!notificationManager.areNotificationsEnabled()) return
        if (!YouBikeFeedbackPromptStore.shouldPromptAndMark(context, report)) return
        createChannel(context, notificationManager)
        val emailIntent = Intent(
            Intent.ACTION_SENDTO,
            Uri.parse(YouBikeFeedbackEmail.mailtoUri(report)),
        )
        val chooserIntent = Intent.createChooser(
            emailIntent,
            context.getString(R.string.you_bike_feedback_chooser_title),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val openEmail = PendingIntent.getActivity(
            context,
            PENDING_INTENT_REQUEST_CODE,
            chooserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bicycle_notification)
            .setContentTitle(context.getString(R.string.you_bike_feedback_notification_title))
            .setContentText(context.getString(R.string.you_bike_feedback_notification_description))
            .setContentIntent(openEmail)
            .setAutoCancel(true)
            .setTimeoutAfter(TIMEOUT_MILLIS)
            .setCategory(Notification.CATEGORY_RECOMMENDATION)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setColor(Color.rgb(245, 130, 32))
            .setOnlyAlertOnce(true)
            .addAction(
                Notification.Action.Builder(
                    null,
                    context.getString(R.string.you_bike_feedback_send_action),
                    openEmail,
                ).build(),
            )
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context, notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.you_bike_feedback_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.you_bike_feedback_channel_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        }
        notificationManager.createNotificationChannel(channel)
    }
}
