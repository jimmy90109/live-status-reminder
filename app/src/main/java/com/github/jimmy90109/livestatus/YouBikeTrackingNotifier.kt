package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri

internal object YouBikeTrackingNotifier {
    private const val CHANNEL_ID = "you_bike_tracking_controls"
    private const val NOTIFICATION_ID = 1009
    private const val TIMEOUT_MILLIS = 90_000L

    fun showHidden(context: Context, sessionId: String) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        createChannel(context, notificationManager)
        val restore = YouBikeTrackingActionReceiver.restorePendingIntent(context, sessionId)
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_bicycle_notification)
            .setContentTitle(context.getString(R.string.you_bike_tracking_hidden_title))
            .setContentText(context.getString(R.string.you_bike_tracking_hidden_description))
            .setContentIntent(restore)
            .setTimeoutAfter(TIMEOUT_MILLIS)
            .setCategory(Notification.CATEGORY_STATUS)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setColor(Color.rgb(245, 130, 32))
            .setOnlyAlertOnce(true)
            .build()
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    fun clearHidden(context: Context) {
        context.getSystemService(NotificationManager::class.java).cancel(NOTIFICATION_ID)
    }

    private fun createChannel(context: Context, notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.you_bike_tracking_controls_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.you_bike_tracking_controls_channel_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager.createNotificationChannel(channel)
    }
}

class YouBikeTrackingActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        when (intent.action) {
            ACTION_HIDE -> YouBikeRideManager.hideLiveUpdate(context, sessionId)
            ACTION_RESTORE -> YouBikeRideManager.restoreLiveUpdate(context, sessionId)
        }
    }

    companion object {
        private const val ACTION_HIDE =
            "com.github.jimmy90109.livestatus.action.HIDE_YOU_BIKE_TRACKING"
        private const val ACTION_RESTORE =
            "com.github.jimmy90109.livestatus.action.RESTORE_YOU_BIKE_TRACKING"
        private const val EXTRA_SESSION_ID = "you_bike_tracking_session_id"

        internal fun hidePendingIntent(context: Context, sessionId: String): PendingIntent =
            pendingIntent(context, sessionId, ACTION_HIDE)

        internal fun restorePendingIntent(context: Context, sessionId: String): PendingIntent =
            pendingIntent(context, sessionId, ACTION_RESTORE)

        private fun pendingIntent(
            context: Context,
            sessionId: String,
            action: String,
        ): PendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            Intent(context, YouBikeTrackingActionReceiver::class.java)
                .setAction(action)
                .setData(
                    Uri.Builder()
                        .scheme("livestatus")
                        .authority("you-bike-tracking")
                        .appendPath(action)
                        .appendPath(sessionId)
                        .build(),
                )
                .putExtra(EXTRA_SESSION_ID, sessionId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
