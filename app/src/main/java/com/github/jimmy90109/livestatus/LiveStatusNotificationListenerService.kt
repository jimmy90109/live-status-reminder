package com.github.jimmy90109.livestatus

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class LiveStatusNotificationListenerService : NotificationListenerService() {
    private var lastUberEatsEvent = LiveStatusNotificationParser.UberEatsEvent.NONE
    private var lastUberEatsPin: String? = null
    private var lastUberEatsTitle: String? = null
    private var lastUberEatsText: String? = null

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        val notification = statusBarNotification.notification
        val notificationText = readNotificationText(notification)
        when (statusBarNotification.packageName) {
            IPASS_PACKAGE -> handleRideNotification(notificationText)
            FOODPANDA_PACKAGE -> handleFoodpandaNotification(notificationText)
            UBER_EATS_PACKAGE -> {
                handleUberEatsNotification(
                    notificationText,
                    readShortCriticalText(notification),
                    readNotificationTitle(notification),
                    readNotificationContentText(notification),
                )
            }
        }
    }

    private fun handleRideNotification(notificationText: String) {
        when (LiveStatusNotificationParser.parse(notificationText)) {
            LiveStatusNotificationParser.RideEvent.ENTERED -> LiveStatusReminder.show(this)
            LiveStatusNotificationParser.RideEvent.EXITED -> LiveStatusReminder.clear(this)
            LiveStatusNotificationParser.RideEvent.NONE -> Unit
        }
    }

    private fun handleFoodpandaNotification(notificationText: String) {
        when (val event = LiveStatusNotificationParser.parseFoodpanda(notificationText)) {
            LiveStatusNotificationParser.FoodpandaEvent.COURIER_ON_THE_WAY,
            LiveStatusNotificationParser.FoodpandaEvent.COURIER_ARRIVING,
            -> LiveStatusReminder.showFoodpanda(this, event)
            LiveStatusNotificationParser.FoodpandaEvent.ORDER_ENDED -> {
                LiveStatusReminder.clearFoodpanda(this)
            }
            LiveStatusNotificationParser.FoodpandaEvent.NONE -> Unit
        }
    }

    private fun handleUberEatsNotification(
        notificationText: String,
        shortCriticalText: String?,
        notificationTitle: String?,
        notificationContentText: String?,
    ) {
        val update = LiveStatusNotificationParser.parseUberEats(notificationText, shortCriticalText)
        val event = update.event

        if (event == LiveStatusNotificationParser.UberEatsEvent.ORDER_ENDED) {
            lastUberEatsEvent = LiveStatusNotificationParser.UberEatsEvent.NONE
            lastUberEatsPin = null
            lastUberEatsTitle = null
            lastUberEatsText = null
            LiveStatusReminder.clearUberEats(this)
            return
        }

        if (event == LiveStatusNotificationParser.UberEatsEvent.ORDER_RECEIVED) {
            lastUberEatsEvent = event
            lastUberEatsPin = update.pin
            lastUberEatsTitle = notificationTitle
            lastUberEatsText = notificationContentText
        } else {
            update.pin?.let { lastUberEatsPin = it }
            notificationTitle?.let { lastUberEatsTitle = it }
            notificationContentText?.let { lastUberEatsText = it }
            if (
                event != LiveStatusNotificationParser.UberEatsEvent.NONE &&
                eventRank(event) >= eventRank(lastUberEatsEvent)
            ) {
                lastUberEatsEvent = event
            }
        }

        if (
            lastUberEatsEvent != LiveStatusNotificationParser.UberEatsEvent.NONE &&
            (
                event != LiveStatusNotificationParser.UberEatsEvent.NONE ||
                    update.pin != null ||
                    notificationTitle != null ||
                    notificationContentText != null
                )
        ) {
            LiveStatusReminder.showUberEats(
                this,
                lastUberEatsEvent,
                lastUberEatsPin,
                lastUberEatsTitle,
                lastUberEatsText,
            )
        }
    }

    private fun eventRank(event: LiveStatusNotificationParser.UberEatsEvent): Int = when (event) {
        LiveStatusNotificationParser.UberEatsEvent.ORDER_RECEIVED -> 1
        LiveStatusNotificationParser.UberEatsEvent.PREPARING -> 2
        LiveStatusNotificationParser.UberEatsEvent.PICKING_UP -> 3
        LiveStatusNotificationParser.UberEatsEvent.ON_THE_WAY -> 4
        LiveStatusNotificationParser.UberEatsEvent.ARRIVING -> 5
        else -> 0
    }

    companion object {
        private const val IPASS_PACKAGE = "com.ipass.ipassmoney"
        private const val FOODPANDA_PACKAGE = "com.global.foodpanda.android"
        private const val UBER_EATS_PACKAGE = "com.ubercab.eats"

        @JvmStatic
        fun readNotificationText(notification: Notification): String {
            val extras = notification.extras
            val text = join(
                extras.getCharSequence(Notification.EXTRA_TITLE),
                extras.getCharSequence(Notification.EXTRA_TEXT),
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
                extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
                extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT),
            )
            val lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            return if (lines == null) text else text + join(*lines)
        }

        @JvmStatic
        fun readShortCriticalText(notification: Notification): String? =
            notification.shortCriticalText?.toString()

        @JvmStatic
        fun readNotificationTitle(notification: Notification): String? =
            notification.extras.getCharSequence(Notification.EXTRA_TITLE)?.toCleanString()

        @JvmStatic
        fun readNotificationContentText(notification: Notification): String? {
            val extras = notification.extras
            return extras.getCharSequence(Notification.EXTRA_TEXT)?.toCleanString()
                ?: extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toCleanString()
                ?: extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
                    ?.mapNotNull { it.toCleanString() }
                    ?.joinToString(" · ")
                    ?.takeIf { it.isNotBlank() }
        }

        private fun join(vararg values: CharSequence?): String = buildString {
            values.forEach { value ->
                if (value != null) append(value).append('\n')
            }
        }

        private fun CharSequence.toCleanString(): String? =
            toString().replace(Regex("""\s+"""), " ").trim().takeIf { it.isNotEmpty() }
    }
}
