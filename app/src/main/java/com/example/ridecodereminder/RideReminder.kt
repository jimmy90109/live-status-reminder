package com.example.ridecodereminder

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import com.example.ridecodereminder.ui.home.HomeScreenHostActivity

object RideReminder {
    private const val CHANNEL_ID = "live_status"
    private const val RIDE_NOTIFICATION_ID = 1001
    private const val FOODPANDA_NOTIFICATION_ID = 1002
    private const val UBER_EATS_NOTIFICATION_ID = 1003
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"

    @JvmStatic
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        notificationManager(context).createNotificationChannel(channel)
    }

    @JvmStatic
    fun show(context: Context) {
        createChannel(context)
        val openIpass = PendingIntent.getActivity(
            context,
            0,
            HomeScreenHostActivity.createOpenIpassIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("乘車中：準備下車時開啟乘車碼")
            .setContentText("點一下立即開啟 iPASS MONEY")
            .setContentIntent(openIpass)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_notification),
                    "開啟乘車碼",
                    openIpass,
                ).build(),
            )
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(
                Notification.BigTextStyle()
                    .bigText("抵達目的地前，點一下立即開啟 iPASS MONEY，準備出示乘車碼。"),
            )
            .setShortCriticalText("乘車中")
            .also(::requestPromotedOngoing)

        notificationManager(context).notify(RIDE_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clear(context: Context) {
        notificationManager(context).cancel(RIDE_NOTIFICATION_ID)
    }

    @JvmStatic
    fun showFoodpanda(
        context: Context,
        event: RideNotificationParser.FoodpandaEvent,
    ) {
        createChannel(context)
        val arriving = event == RideNotificationParser.FoodpandaEvent.COURIER_ARRIVING
        val title = if (arriving) "foodpanda 即將抵達" else "foodpanda 外送中"
        val text = if (arriving) {
            "外送夥伴即將抵達，準備取餐。"
        } else {
            "外送夥伴在路上，請留意手機來電或訊息。"
        }
        val shortText = if (arriving) "即將抵達" else "外送中"
        val openFoodpanda = PendingIntent.getActivity(
            context,
            1,
            HomeScreenHostActivity.createOpenFoodpandaIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_food_delivery_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openFoodpanda)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_food_delivery_notification),
                    "開啟 foodpanda",
                    openFoodpanda,
                ).build(),
            )
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setShortCriticalText(shortText)
            .also(::requestPromotedOngoing)

        notificationManager(context).notify(FOODPANDA_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearFoodpanda(context: Context) {
        notificationManager(context).cancel(FOODPANDA_NOTIFICATION_ID)
    }

    @JvmStatic
    fun showUberEats(
        context: Context,
        event: RideNotificationParser.UberEatsEvent,
        pin: String?,
    ) {
        createChannel(context)
        val title = uberEatsTitle(event)
        val statusText = uberEatsStatusText(event)
        val privateText = pin?.let { "$statusText · PIN $it" } ?: statusText
        val openUberEats = PendingIntent.getActivity(
            context,
            2,
            HomeScreenHostActivity.createOpenUberEatsIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val publicNotification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_food_delivery_notification)
            .setContentTitle(title)
            .setContentText(statusText)
            .setContentIntent(openUberEats)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .also { applyUberEatsStyle(it, event) }
            .setShortCriticalText(uberEatsShortText(event))
            .also(::requestPromotedOngoing)
            .build()

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_food_delivery_notification)
            .setContentTitle(title)
            .setContentText(privateText)
            .setContentIntent(openUberEats)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_food_delivery_notification),
                    "開啟 Uber Eats",
                    openUberEats,
                ).build(),
            )
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicNotification)
            .also { applyUberEatsStyle(it, event) }
            .setShortCriticalText(pin ?: uberEatsShortText(event))
            .also(::requestPromotedOngoing)

        notificationManager(context).notify(UBER_EATS_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearUberEats(context: Context) {
        notificationManager(context).cancel(UBER_EATS_NOTIFICATION_ID)
    }

    private fun applyUberEatsStyle(
        builder: Notification.Builder,
        event: RideNotificationParser.UberEatsEvent,
    ) {
        val style = Notification.ProgressStyle()
            .setStyledByProgress(true)
            .setProgress(uberEatsProgress(event))
        repeat(5) { index ->
            style.addProgressSegment(
                Notification.ProgressStyle.Segment(20)
                    .setId(index + 1)
                    .setColor(Color.rgb(6, 193, 103)),
            )
        }
        builder.setStyle(style)
    }

    private fun uberEatsProgress(event: RideNotificationParser.UberEatsEvent): Int = when (event) {
        RideNotificationParser.UberEatsEvent.PREPARING -> 40
        RideNotificationParser.UberEatsEvent.PICKING_UP -> 60
        RideNotificationParser.UberEatsEvent.ON_THE_WAY -> 80
        RideNotificationParser.UberEatsEvent.ARRIVING -> 100
        else -> 20
    }

    private fun uberEatsTitle(event: RideNotificationParser.UberEatsEvent): String = when (event) {
        RideNotificationParser.UberEatsEvent.PREPARING -> "Uber Eats 正在準備訂單"
        RideNotificationParser.UberEatsEvent.PICKING_UP -> "Uber Eats 正在取餐"
        RideNotificationParser.UberEatsEvent.ON_THE_WAY -> "Uber Eats 配送中"
        RideNotificationParser.UberEatsEvent.ARRIVING -> "Uber Eats 快到了！"
        else -> "Uber Eats 訂單已收到"
    }

    private fun uberEatsStatusText(event: RideNotificationParser.UberEatsEvent): String =
        when (event) {
            RideNotificationParser.UberEatsEvent.PREPARING -> "店家正在準備您的餐點。"
            RideNotificationParser.UberEatsEvent.PICKING_UP -> "外送夥伴正在取餐。"
            RideNotificationParser.UberEatsEvent.ON_THE_WAY -> "外送夥伴正前往您所在位置。"
            RideNotificationParser.UberEatsEvent.ARRIVING -> "外送夥伴即將抵達，請準備取餐。"
            else -> "店家已收到您的訂單。"
        }

    private fun uberEatsShortText(event: RideNotificationParser.UberEatsEvent): String =
        when (event) {
            RideNotificationParser.UberEatsEvent.PREPARING -> "備餐中"
            RideNotificationParser.UberEatsEvent.PICKING_UP -> "取餐中"
            RideNotificationParser.UberEatsEvent.ON_THE_WAY -> "配送中"
            RideNotificationParser.UberEatsEvent.ARRIVING -> "快到了"
            else -> "已接單"
        }

    private fun requestPromotedOngoing(builder: Notification.Builder) {
        builder.extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
    }

    private fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)
}
