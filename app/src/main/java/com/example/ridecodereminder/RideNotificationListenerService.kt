package com.example.ridecodereminder

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class RideNotificationListenerService : NotificationListenerService() {
    private var lastUberEatsEvent = RideNotificationParser.UberEatsEvent.NONE
    private var lastUberEatsPin: String? = null

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        val notification = statusBarNotification.notification
        val notificationText = readNotificationText(notification)
        when (statusBarNotification.packageName) {
            IPASS_PACKAGE -> handleRideNotification(notificationText)
            FOODPANDA_PACKAGE -> handleFoodpandaNotification(notificationText)
            UBER_EATS_PACKAGE -> {
                handleUberEatsNotification(notificationText, readShortCriticalText(notification))
            }
        }
    }

    private fun handleRideNotification(notificationText: String) {
        when (RideNotificationParser.parse(notificationText)) {
            RideNotificationParser.RideEvent.ENTERED -> RideReminder.show(this)
            RideNotificationParser.RideEvent.EXITED -> RideReminder.clear(this)
            RideNotificationParser.RideEvent.NONE -> Unit
        }
    }

    private fun handleFoodpandaNotification(notificationText: String) {
        when (val event = RideNotificationParser.parseFoodpanda(notificationText)) {
            RideNotificationParser.FoodpandaEvent.COURIER_ON_THE_WAY,
            RideNotificationParser.FoodpandaEvent.COURIER_ARRIVING,
            -> RideReminder.showFoodpanda(this, event)
            RideNotificationParser.FoodpandaEvent.ORDER_ENDED -> {
                RideReminder.clearFoodpanda(this)
            }
            RideNotificationParser.FoodpandaEvent.NONE -> Unit
        }
    }

    private fun handleUberEatsNotification(
        notificationText: String,
        shortCriticalText: String?,
    ) {
        val update = RideNotificationParser.parseUberEats(notificationText, shortCriticalText)
        val event = update.event

        if (event == RideNotificationParser.UberEatsEvent.ORDER_ENDED) {
            lastUberEatsEvent = RideNotificationParser.UberEatsEvent.NONE
            lastUberEatsPin = null
            RideReminder.clearUberEats(this)
            return
        }

        if (event == RideNotificationParser.UberEatsEvent.ORDER_RECEIVED) {
            lastUberEatsEvent = event
            lastUberEatsPin = update.pin
        } else {
            update.pin?.let { lastUberEatsPin = it }
            if (
                event != RideNotificationParser.UberEatsEvent.NONE &&
                eventRank(event) >= eventRank(lastUberEatsEvent)
            ) {
                lastUberEatsEvent = event
            }
        }

        if (
            lastUberEatsEvent != RideNotificationParser.UberEatsEvent.NONE &&
            (event != RideNotificationParser.UberEatsEvent.NONE || update.pin != null)
        ) {
            RideReminder.showUberEats(this, lastUberEatsEvent, lastUberEatsPin)
        }
    }

    private fun eventRank(event: RideNotificationParser.UberEatsEvent): Int = when (event) {
        RideNotificationParser.UberEatsEvent.ORDER_RECEIVED -> 1
        RideNotificationParser.UberEatsEvent.PREPARING -> 2
        RideNotificationParser.UberEatsEvent.PICKING_UP -> 3
        RideNotificationParser.UberEatsEvent.ON_THE_WAY -> 4
        RideNotificationParser.UberEatsEvent.ARRIVING -> 5
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

        private fun join(vararg values: CharSequence?): String = buildString {
            values.forEach { value ->
                if (value != null) append(value).append('\n')
            }
        }
    }
}
