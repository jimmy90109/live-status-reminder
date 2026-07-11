package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import com.github.jimmy90109.livestatus.ui.home.HomeScreenHostActivity

object LiveStatusReminder {
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
        val payload = ridePayload(openIpass)
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(payload.smallIconRes)
            .setContentTitle(payload.title)
            .setContentText(payload.contentText)
            .setContentIntent(payload.contentIntent)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, payload.leftIconRes),
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
            .setShortCriticalText(payload.criticalText)
            .also(::requestPromotedOngoing)
            .also { XiaomiHyperIslandRenderer.apply(context, it, payload) }

        notificationManager(context).notify(RIDE_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clear(context: Context) {
        notificationManager(context).cancel(RIDE_NOTIFICATION_ID)
    }

    @JvmStatic
    fun showFoodpanda(
        context: Context,
        event: LiveStatusNotificationParser.FoodpandaEvent,
    ) {
        createChannel(context)
        val openFoodpanda = PendingIntent.getActivity(
            context,
            1,
            HomeScreenHostActivity.createOpenFoodpandaIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val payload = foodpandaPayload(event, openFoodpanda)
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(payload.smallIconRes)
            .setContentTitle(payload.title)
            .setContentText(payload.contentText)
            .setContentIntent(payload.contentIntent)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, payload.leftIconRes),
                    "開啟 foodpanda",
                    openFoodpanda,
                ).build(),
            )
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(Notification.BigTextStyle().bigText(payload.contentText))
            .setShortCriticalText(payload.criticalText)
            .also(::requestPromotedOngoing)
            .also { XiaomiHyperIslandRenderer.apply(context, it, payload) }

        notificationManager(context).notify(FOODPANDA_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearFoodpanda(context: Context) {
        notificationManager(context).cancel(FOODPANDA_NOTIFICATION_ID)
    }

    @JvmStatic
    fun showUberEats(
        context: Context,
        event: LiveStatusNotificationParser.UberEatsEvent,
        pin: String?,
        officialTitle: String? = null,
        officialText: String? = null,
    ) {
        createChannel(context)
        val openUberEats = PendingIntent.getActivity(
            context,
            2,
            HomeScreenHostActivity.createOpenUberEatsIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val payload = uberEatsPayload(event, officialTitle, officialText, openUberEats)
        val privateText = pin?.let { "${payload.contentText} · PIN $it" } ?: payload.contentText
        val publicNotification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(payload.smallIconRes)
            .setContentTitle(payload.title)
            .setContentText(payload.contentText)
            .setContentIntent(payload.contentIntent)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .also { applyUberEatsStyle(it, event) }
            .setShortCriticalText(payload.criticalText)
            .also(::requestPromotedOngoing)
            .also { XiaomiHyperIslandRenderer.apply(context, it, payload) }
            .build()

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(payload.smallIconRes)
            .setContentTitle(payload.title)
            .setContentText(privateText)
            .setContentIntent(payload.contentIntent)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, payload.leftIconRes),
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
            .also { XiaomiHyperIslandRenderer.apply(context, it, payload) }

        notificationManager(context).notify(UBER_EATS_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearUberEats(context: Context) {
        notificationManager(context).cancel(UBER_EATS_NOTIFICATION_ID)
    }

    private fun applyUberEatsStyle(
        builder: Notification.Builder,
        event: LiveStatusNotificationParser.UberEatsEvent,
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

    private fun uberEatsProgress(event: LiveStatusNotificationParser.UberEatsEvent): Int = when (event) {
        LiveStatusNotificationParser.UberEatsEvent.PREPARING -> 40
        LiveStatusNotificationParser.UberEatsEvent.PICKING_UP -> 60
        LiveStatusNotificationParser.UberEatsEvent.ON_THE_WAY -> 80
        LiveStatusNotificationParser.UberEatsEvent.ARRIVING -> 100
        else -> 20
    }

    private fun uberEatsTitle(
        event: LiveStatusNotificationParser.UberEatsEvent,
        officialTitle: String?,
    ): String {
        val cleanOfficialTitle = officialTitle
            ?.takeUnless { it.contains("Uber Eats", ignoreCase = true) && it.contains("·") }
            ?.takeUnless { it.equals("Uber Eats", ignoreCase = true) }
        return cleanOfficialTitle?.let { title ->
            if (title.startsWith("Uber Eats", ignoreCase = true)) title else "Uber Eats $title"
        } ?: uberEatsFallbackTitle(event)
    }

    private fun uberEatsFallbackTitle(event: LiveStatusNotificationParser.UberEatsEvent): String =
        when (event) {
            LiveStatusNotificationParser.UberEatsEvent.PREPARING -> "Uber Eats 正在準備訂單"
            LiveStatusNotificationParser.UberEatsEvent.PICKING_UP -> "Uber Eats 正在取餐"
            LiveStatusNotificationParser.UberEatsEvent.ON_THE_WAY -> "Uber Eats 正前往您所在位置"
            LiveStatusNotificationParser.UberEatsEvent.ARRIVING -> "Uber Eats 快到了！"
            else -> "Uber Eats 訂單已收到"
        }

    private fun uberEatsStatusText(event: LiveStatusNotificationParser.UberEatsEvent): String =
        when (event) {
            LiveStatusNotificationParser.UberEatsEvent.PREPARING -> "抵達時間更新中"
            LiveStatusNotificationParser.UberEatsEvent.PICKING_UP -> "外送夥伴正在取餐。"
            LiveStatusNotificationParser.UberEatsEvent.ON_THE_WAY -> "外送夥伴正前往您所在位置。"
            LiveStatusNotificationParser.UberEatsEvent.ARRIVING -> "外送夥伴即將抵達，請準備取餐。"
            else -> "抵達時間更新中"
        }

    private fun uberEatsShortText(event: LiveStatusNotificationParser.UberEatsEvent): String =
        when (event) {
            LiveStatusNotificationParser.UberEatsEvent.PREPARING -> "備餐中"
            LiveStatusNotificationParser.UberEatsEvent.PICKING_UP -> "取餐中"
            LiveStatusNotificationParser.UberEatsEvent.ON_THE_WAY -> "配送中"
            LiveStatusNotificationParser.UberEatsEvent.ARRIVING -> "快到了"
            else -> "已接單"
        }

    internal fun ridePayload(contentIntent: PendingIntent? = null): LiveStatusPayload =
        LiveStatusPayload(
            id = RIDE_NOTIFICATION_ID,
            appName = "iPASS MONEY",
            smallIconRes = R.drawable.ic_notification,
            leftIconRes = R.drawable.ic_notification,
            criticalText = "乘車中",
            title = "乘車中：準備下車時開啟乘車碼",
            contentText = "點一下立即開啟 iPASS MONEY",
            contentIntent = contentIntent,
        )

    internal fun foodpandaPayload(
        event: LiveStatusNotificationParser.FoodpandaEvent,
        contentIntent: PendingIntent? = null,
    ): LiveStatusPayload {
        val arriving = event == LiveStatusNotificationParser.FoodpandaEvent.COURIER_ARRIVING
        return LiveStatusPayload(
            id = FOODPANDA_NOTIFICATION_ID,
            appName = "foodpanda",
            smallIconRes = R.drawable.ic_food_delivery_notification,
            leftIconRes = R.drawable.ic_food_delivery_notification,
            criticalText = if (arriving) "即將抵達" else "外送中",
            title = if (arriving) "foodpanda 即將抵達" else "foodpanda 外送中",
            contentText = if (arriving) {
                "外送夥伴即將抵達，準備取餐。"
            } else {
                "外送夥伴在路上，請留意手機來電或訊息。"
            },
            contentIntent = contentIntent,
        )
    }

    internal fun uberEatsPayload(
        event: LiveStatusNotificationParser.UberEatsEvent,
        officialTitle: String? = null,
        officialText: String? = null,
        contentIntent: PendingIntent? = null,
    ): LiveStatusPayload =
        LiveStatusPayload(
            id = UBER_EATS_NOTIFICATION_ID,
            appName = "Uber Eats",
            smallIconRes = R.drawable.ic_food_delivery_notification,
            leftIconRes = R.drawable.ic_food_delivery_notification,
            criticalText = uberEatsShortText(event),
            title = uberEatsTitle(event, officialTitle),
            contentText = officialText ?: uberEatsStatusText(event),
            progress = uberEatsProgress(event),
            contentIntent = contentIntent,
        )

    private fun requestPromotedOngoing(builder: Notification.Builder) {
        builder.extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
    }

    private fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)
}
