package com.github.jimmy90109.livestatus

import android.app.Notification
import android.content.Context
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.view.View
import android.view.ViewGroup
import android.widget.RemoteViews
import android.widget.TextView

class LiveStatusNotificationListenerService : NotificationListenerService() {
    private var lastUberEatsEvent = LiveStatusNotificationParser.UberEatsEvent.NONE
    private var lastUberEatsPin: String? = null
    private var lastUberEatsTitle: String? = null
    private var lastUberEatsText: String? = null

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        val notification = statusBarNotification.notification
        val notificationText = readNotificationText(
            this,
            statusBarNotification.packageName,
            notification,
        )
        when (statusBarNotification.packageName) {
            IPASS_PACKAGE -> if (AppReminderPreferences.App.IPASS.isEnabled(this)) {
                handleRideNotification(notificationText)
            }
            FOODPANDA_PACKAGE -> if (AppReminderPreferences.App.FOODPANDA.isEnabled(this)) {
                handleFoodpandaNotification(notificationText)
            }
            UBER_EATS_PACKAGE -> {
                val shortCriticalText = readShortCriticalText(notification)
                val notificationTitle = readNotificationTitle(notification)
                val notificationContentText = readNotificationContentText(notification)
                val update = LiveStatusNotificationParser.parseUberEats(
                    notificationText,
                    shortCriticalText,
                )
                UberEatsDebugPayloadStore.record(
                    this,
                    statusBarNotification,
                    notificationText,
                    shortCriticalText,
                    notificationTitle,
                    notificationContentText,
                    update,
                )
                if (AppReminderPreferences.App.UBER_EATS.isEnabled(this)) {
                    handleUberEatsNotification(
                        update,
                        notificationText,
                        notificationTitle,
                        notificationContentText,
                    )
                } else {
                    resetUberEatsState()
                }
            }
            PIKMIN_BLOOM_PACKAGE -> if (AppReminderPreferences.App.PIKMIN_BLOOM.isEnabled(this)) {
                handlePikminBloomNotification(notificationText)
            }
        }
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        if (statusBarNotification.packageName != PIKMIN_BLOOM_PACKAGE) return

        val notificationText = readNotificationText(statusBarNotification.notification)
        if (
            LiveStatusNotificationParser.parsePikminBloom(notificationText) ==
            LiveStatusNotificationParser.PikminEvent.FLOWER_PLANTING
        ) {
            LiveStatusReminder.clearPikminBloom(this)
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
        update: LiveStatusNotificationParser.UberEatsUpdate,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
    ) {
        val event = update.event

        if (event == LiveStatusNotificationParser.UberEatsEvent.ORDER_ENDED) {
            resetUberEatsState()
            LiveStatusReminder.clearUberEats(this)
            return
        }

        if (event == LiveStatusNotificationParser.UberEatsEvent.ORDER_RECEIVED) {
            lastUberEatsEvent = event
            lastUberEatsPin = update.pin
            lastUberEatsTitle = notificationTitle
            lastUberEatsText = notificationContentText ?: notificationText
        } else {
            update.pin?.let { lastUberEatsPin = it }
            notificationTitle?.let { lastUberEatsTitle = it }
            (notificationContentText ?: notificationText).takeIf { it.isNotBlank() }?.let {
                lastUberEatsText = it
            }
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

    private fun handlePikminBloomNotification(notificationText: String) {
        when (LiveStatusNotificationParser.parsePikminBloom(notificationText)) {
            LiveStatusNotificationParser.PikminEvent.FLOWER_PLANTING ->
                LiveStatusReminder.showPikminBloom(this)
            LiveStatusNotificationParser.PikminEvent.NONE ->
                LiveStatusReminder.clearPikminBloom(this)
        }
    }

    private fun resetUberEatsState() {
        lastUberEatsEvent = LiveStatusNotificationParser.UberEatsEvent.NONE
        lastUberEatsPin = null
        lastUberEatsTitle = null
        lastUberEatsText = null
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
        private const val PIKMIN_BLOOM_PACKAGE = "com.nianticlabs.pikmin"

        @JvmStatic
        fun readNotificationText(notification: Notification): String {
            val extras = notification.extras
            val text = join(
                notification.tickerText,
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
        fun readNotificationText(context: Context, notification: Notification): String =
            readNotificationText(context, null, notification)

        @JvmStatic
        fun readNotificationText(
            context: Context,
            packageName: String?,
            notification: Notification,
        ): String =
            readNotificationText(notification) + readRemoteViewsText(context, packageName, notification)

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

        private fun readRemoteViewsText(
            context: Context,
            packageName: String?,
            notification: Notification,
        ): String {
            val packageContext = packageName?.let {
                runCatching { context.createPackageContext(it, 0) }.getOrNull()
            }
            val contexts = listOfNotNull(packageContext, context).distinct()
            return contexts
                .flatMap { remoteViewContext ->
                    notification.remoteViews().flatMap { remoteViews ->
                        remoteViews.readTextViews(remoteViewContext)
                    }
                }
                .distinct()
                .joinToString(separator = "\n", postfix = "\n")
        }

        private fun Notification.remoteViews(): List<RemoteViews> =
            listOfNotNull(
                contentView,
                bigContentView,
                headsUpContentView,
                publicVersion?.contentView,
                publicVersion?.bigContentView,
                publicVersion?.headsUpContentView,
            )

        private fun RemoteViews.readTextViews(context: Context): List<String> =
            runCatching {
                val view = apply(context, null)
                view.collectTextViews()
            }.getOrDefault(emptyList())

        private fun View.collectTextViews(): List<String> = when (this) {
            is TextView -> listOfNotNull(text.toCleanString())
            is ViewGroup -> buildList {
                repeat(childCount) { index ->
                    addAll(getChildAt(index).collectTextViews())
                }
            }
            else -> emptyList()
        }

        private fun CharSequence.toCleanString(): String? =
            toString().replace(Regex("""\s+"""), " ").trim().takeIf { it.isNotEmpty() }
    }
}
