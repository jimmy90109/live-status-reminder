package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Icon
import android.os.Build
import android.os.SystemClock
import com.github.jimmy90109.livestatus.ui.home.HomeScreenHostActivity
import com.github.jimmy90109.livestatus.ui.home.YouBikeRegionPickerActivity
import java.text.BreakIterator
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object LiveStatusReminder {
    private const val CHANNEL_ID = "live_status"
    private const val MEDIA_CHANNEL_ID = "media_live_status"
    private const val RIDE_NOTIFICATION_ID = 1001
    private const val FOODPANDA_NOTIFICATION_ID = 1002
    private const val UBER_EATS_NOTIFICATION_ID = 1003
    private const val PIKMIN_BLOOM_NOTIFICATION_ID = 1004
    private const val UBER_RIDE_NOTIFICATION_ID = 1005
    private const val CLOCK_TIMER_NOTIFICATION_ID = 1006
    private const val TAIWAN_PAY_NOTIFICATION_ID = 1007
    private const val YOU_BIKE_NOTIFICATION_ID = 1008
    private const val MEDIA_PLAYBACK_NOTIFICATION_ID = 1009
    private const val EXTRA_REQUEST_PROMOTED_ONGOING = "android.requestPromotedOngoing"
    private val uberEatsArrivalEstimate = Regex(
        """抵達時間(?:為|：|:)?\s*([0-9]{1,2}:[0-9]{2}(?:\s*[-–]\s*[0-9]{1,2}:[0-9]{2})?\s*(?:AM|PM)?)""",
        RegexOption.IGNORE_CASE,
    )

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
    fun createMediaChannel(context: Context) {
        val channel = NotificationChannel(
            MEDIA_CHANNEL_ID,
            context.getString(R.string.media_notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.media_notification_channel_description)
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
            setSound(null, null)
            enableVibration(false)
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
    fun showTaiwanPay(context: Context) {
        createChannel(context)
        val openTaiwanPay = PendingIntent.getActivity(
            context,
            6,
            HomeScreenHostActivity.createOpenTaiwanPayIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val payload = taiwanPayRidePayload(openTaiwanPay)
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(payload.smallIconRes)
            .setContentTitle(payload.title)
            .setContentText(payload.contentText)
            .setContentIntent(payload.contentIntent)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, payload.leftIconRes),
                    "開啟乘車碼",
                    openTaiwanPay,
                ).build(),
            )
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(
                Notification.BigTextStyle().bigText(
                    "抵達目的地前，點一下立即開啟台灣 Pay，準備出示乘車碼。",
                ),
            )
            .setShortCriticalText(payload.criticalText)
            .also(::requestPromotedOngoing)
            .also { XiaomiHyperIslandRenderer.apply(context, it, payload) }

        notificationManager(context).notify(TAIWAN_PAY_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearTaiwanPay(context: Context) {
        notificationManager(context).cancel(TAIWAN_PAY_NOTIFICATION_ID)
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
        val privatePayload = uberEatsPrivatePayload(event, payload, pin)

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(privatePayload.smallIconRes)
            .setContentTitle(privatePayload.title)
            .setContentText(privatePayload.contentText)
            .setContentIntent(privatePayload.contentIntent)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, privatePayload.leftIconRes),
                    "開啟 Uber Eats",
                    openUberEats,
                ).build(),
            )
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .also { applyUberEatsStyle(it, event) }
            .setShortCriticalText(pin ?: uberEatsShortText(event))
            .also(::requestPromotedOngoing)
            .also { XiaomiHyperIslandRenderer.apply(context, it, privatePayload) }

        notificationManager(context).notify(UBER_EATS_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearUberEats(context: Context) {
        notificationManager(context).cancel(UBER_EATS_NOTIFICATION_ID)
    }

    @JvmStatic
    fun showUberRide(
        context: Context,
        update: LiveStatusNotificationParser.UberRideUpdate,
    ) {
        createChannel(context)
        val openUber = PendingIntent.getActivity(
            context,
            4,
            HomeScreenHostActivity.createOpenUberIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val payload = uberRidePayload(update, openUber)
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(payload.smallIconRes)
            .setContentTitle(payload.title)
            .setContentText(payload.contentText)
            .setContentIntent(payload.contentIntent)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, payload.leftIconRes),
                    "Open Uber",
                    openUber,
                ).build(),
            )
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setStyle(Notification.BigTextStyle().bigText(payload.contentText))
            .setShortCriticalText(payload.criticalText)
            .also(::requestPromotedOngoing)
            .also { XiaomiHyperIslandRenderer.apply(context, it, payload) }

        notificationManager(context).notify(UBER_RIDE_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearUberRide(context: Context) {
        notificationManager(context).cancel(UBER_RIDE_NOTIFICATION_ID)
    }

    @JvmStatic
    fun showPikminBloom(
        context: Context,
        language: LiveStatusNotificationParser.PikminLanguage =
            LiveStatusNotificationParser.PikminLanguage.CHINESE,
    ) {
        createChannel(context)
        val openPikminBloom = PendingIntent.getActivity(
            context,
            3,
            HomeScreenHostActivity.createOpenPikminBloomIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val payload = pikminBloomPayload(language, openPikminBloom)
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(payload.smallIconRes)
            .setContentTitle(payload.title)
            .setContentText(payload.contentText)
            .setContentIntent(payload.contentIntent)
            .addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, payload.leftIconRes),
                    if (language == LiveStatusNotificationParser.PikminLanguage.ENGLISH) {
                        "Open Pikmin Bloom"
                    } else {
                        "開啟 Pikmin Bloom"
                    },
                    openPikminBloom,
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

        notificationManager(context).notify(PIKMIN_BLOOM_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearPikminBloom(context: Context) {
        notificationManager(context).cancel(PIKMIN_BLOOM_NOTIFICATION_ID)
    }

    @JvmStatic
    fun showClockTimer(context: Context, update: ClockTimerUpdate) {
        createChannel(context)
        val openClock = update.contentIntent ?: PendingIntent.getActivity(
            context,
            5,
            HomeScreenHostActivity.createOpenClockIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val payload = clockTimerPayload(
            update = update,
            nowElapsedRealtimeMillis = SystemClock.elapsedRealtime(),
            contentIntent = openClock,
        )
        val timer = requireNotNull(payload.timer)
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(payload.smallIconRes)
            .setContentTitle(payload.title)
            .setContentText(payload.contentText)
            .setContentIntent(payload.contentIntent)
            .setCategory(Notification.CATEGORY_ALARM)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .also { ClockTimerNotificationStyle.apply(it, timer, payload.criticalText) }
            .setShortCriticalText(payload.criticalText)
            .also(::requestPromotedOngoing)
            .also { XiaomiHyperIslandRenderer.apply(context, it, payload) }

        notificationManager(context).notify(CLOCK_TIMER_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearClockTimer(context: Context) {
        notificationManager(context).cancel(CLOCK_TIMER_NOTIFICATION_ID)
    }

    @JvmStatic
    internal fun showMediaPlayback(context: Context, update: MediaPlaybackUpdate) {
        createMediaChannel(context)
        val contentText = MediaPlaybackText.artistAndAlbum(update.artist, update.album)
            ?: context.getString(R.string.media_playback_now_playing)
        val smallIcon = update.smallIcon
            ?: Icon.createWithResource(context, R.drawable.ic_music_notification)
        val builder = Notification.Builder(context, MEDIA_CHANNEL_ID)
            .setSmallIcon(smallIcon)
            .setContentTitle(update.title)
            .setContentText(contentText)
            .setCategory(Notification.CATEGORY_STATUS)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(update.visibility.normalizedNotificationVisibility())
            .setColorized(false)
            .setShortCriticalText(mediaShortCriticalText(update.title))
            .also(::requestPromotedOngoing)
        val progressPercent = update.progressPercent
        if (progressPercent != null) {
            val progressStyle = Notification.ProgressStyle()
                .setStyledByProgress(true)
                .setProgress(progressPercent)
                .addProgressSegment(
                    Notification.ProgressStyle.Segment(100).setId(1),
                )
            builder.setStyle(progressStyle)
        } else {
            builder.setStyle(Notification.BigTextStyle().bigText(contentText))
        }
        if (Build.VERSION.SDK_INT >= 37) {
            builder.extras.putBoolean(Notification.EXTRA_PREFER_SMALL_ICON, true)
        }
        update.contentIntent?.let(builder::setContentIntent)
        update.artwork?.let(builder::setLargeIcon)
        val token = update.sessionToken
        if (token != null) {
            update.leftControl?.let { control ->
                builder.addAction(
                    mediaAction(
                        context = context,
                        title = context.getString(control.titleResource),
                        pendingIntent = MediaPlaybackActionReceiver.controlPendingIntent(
                            context,
                            token,
                            control,
                        ),
                        semanticAction = Notification.Action.SEMANTIC_ACTION_NONE,
                    ),
                )
            }
            if (update.canPause) {
                builder.addAction(
                    mediaAction(
                        context = context,
                        title = context.getString(R.string.media_playback_pause),
                        pendingIntent = MediaPlaybackActionReceiver.pausePendingIntent(context, token),
                        semanticAction = Notification.Action.SEMANTIC_ACTION_PAUSE,
                    ),
                )
            } else if (update.canPlay) {
                builder.addAction(
                    mediaAction(
                        context = context,
                        title = context.getString(R.string.media_playback_play),
                        pendingIntent = MediaPlaybackActionReceiver.playPendingIntent(context, token),
                        semanticAction = Notification.Action.SEMANTIC_ACTION_PLAY,
                    ),
                )
            }
            update.rightControl?.let { control ->
                builder.addAction(
                    mediaAction(
                        context = context,
                        title = context.getString(control.titleResource),
                        pendingIntent = MediaPlaybackActionReceiver.controlPendingIntent(
                            context,
                            token,
                            control,
                        ),
                        semanticAction = Notification.Action.SEMANTIC_ACTION_NONE,
                    ),
                )
            }
        }

        notificationManager(context).notify(MEDIA_PLAYBACK_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearMediaPlayback(context: Context) {
        notificationManager(context).cancel(MEDIA_PLAYBACK_NOTIFICATION_ID)
    }

    @JvmStatic
    fun showYouBike(
        context: Context,
        session: YouBikeRideSession,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        createChannel(context)
        val estimate = YouBikeFarePolicy.estimate(
            session.borrowedAtMillis,
            nowMillis,
            session.region,
            session.vehicleType,
        )
        val nextTime = estimate?.nextBoundaryMillis?.let {
            Instant.ofEpochMilli(it).atZone(ZoneId.of("Asia/Taipei")).format(DateTimeFormatter.ofPattern("HH:mm"))
        }
        val title = when {
            estimate != null -> "$nextTime 後預估 NT$${estimate.nextAmount}"
            session.region == YouBikeRegion.UNSUPPORTED -> "此服務區域尚未支援費率"
            else -> "選擇服務區域以估算費用"
        }
        val shortCriticalText = youBikeShortCriticalText(estimate)
        val criticalText = shortCriticalText ?: "騎乘中"
        val contentText = "${session.vehicleType.displayName}・借自 ${session.stationName}"
        val openYouBike = PendingIntent.getActivity(
            context,
            8,
            HomeScreenHostActivity.createOpenYouBikeIntent(context),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val payload = LiveStatusPayload(
            id = YOU_BIKE_NOTIFICATION_ID,
            appName = "YouBike",
            smallIconRes = R.drawable.ic_bicycle_notification,
            leftIconRes = R.drawable.ic_bicycle_notification,
            criticalText = criticalText,
            title = title,
            contentText = contentText,
            contentIntent = openYouBike,
        )
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(payload.smallIconRes)
            .setContentTitle(payload.title)
            .setContentText(payload.contentText)
            .setContentIntent(openYouBike)
            .setDeleteIntent(YouBikeTrackingActionReceiver.hidePendingIntent(context, session.id))
            .setCategory(Notification.CATEGORY_NAVIGATION)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .setColor(Color.rgb(245, 130, 32))
            .setShortCriticalText(shortCriticalText)
            .also { YouBikeNotificationStyle.apply(it, session, estimate, nowMillis) }
            .also(::requestPromotedOngoing)
            .also { XiaomiHyperIslandRenderer.apply(context, it, payload) }
        if (session.region == YouBikeRegion.UNRESOLVED) {
            val chooseRegion = PendingIntent.getActivity(
                context,
                session.id.hashCode(),
                YouBikeRegionPickerActivity.createIntent(context, session.id),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_bicycle_notification),
                    context.getString(R.string.you_bike_choose_region_action),
                    chooseRegion,
                ).build(),
            )
        }
        notificationManager(context).notify(YOU_BIKE_NOTIFICATION_ID, builder.build())
    }

    @JvmStatic
    fun clearYouBike(context: Context) {
        notificationManager(context).cancel(YOU_BIKE_NOTIFICATION_ID)
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

    internal fun uberEatsPrivateText(
        event: LiveStatusNotificationParser.UberEatsEvent,
        contentText: String,
        pin: String?,
    ): String {
        val officialDetails = uberEatsOfficialDetails(event, contentText)
        return pin?.let { "$officialDetails · PIN $it" } ?: officialDetails
    }

    internal fun uberEatsPrivatePayload(
        event: LiveStatusNotificationParser.UberEatsEvent,
        payload: LiveStatusPayload,
        pin: String?,
    ): LiveStatusPayload =
        payload.copy(
            criticalText = pin ?: payload.criticalText,
            contentText = uberEatsPrivateText(event, payload.contentText, pin),
        )

    private fun uberEatsOfficialDetails(
        event: LiveStatusNotificationParser.UberEatsEvent,
        contentText: String,
    ): String {
        val lines = contentText
            .lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filterNot { it == "Uber Eats" || it.startsWith("Uber Eats ·") }
            .filterNot { it == "正前往您所在位置" || it == "正在前往您所在位置" }
            .filterNot { it.length == 1 && it[0].isDigit() }

        val arrivalLine = lines.firstOrNull { it.contains("抵達時間") }
        val details = (arrivalLine ?: lines.joinToString(" · "))
            .replace(Regex("""\s+"""), " ")
            .trim()

        return details.takeIf { it.isNotEmpty() }
            ?: uberEatsArrivalEstimate.find(contentText)?.groupValues?.getOrNull(1)?.let {
                "預估 $it"
            }
            ?: uberEatsStatusText(event)
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

    internal fun taiwanPayRidePayload(
        contentIntent: PendingIntent? = null,
    ): LiveStatusPayload =
        LiveStatusPayload(
            id = TAIWAN_PAY_NOTIFICATION_ID,
            appName = "台灣 Pay",
            smallIconRes = R.drawable.ic_notification,
            leftIconRes = R.drawable.ic_notification,
            criticalText = "乘車中",
            title = "乘車中：準備下車時開啟乘車碼",
            contentText = "點一下立即開啟台灣 Pay",
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

    internal fun uberRidePayload(
        update: LiveStatusNotificationParser.UberRideUpdate,
        contentIntent: PendingIntent? = null,
    ): LiveStatusPayload =
        LiveStatusPayload(
            id = UBER_RIDE_NOTIFICATION_ID,
            appName = "Uber",
            smallIconRes = R.drawable.ic_car_notification,
            leftIconRes = R.drawable.ic_car_notification,
            criticalText = uberRideShortText(update),
            title = uberRideTitle(update),
            contentText = uberRideContentText(update),
            progress = uberRideProgress(update),
            contentIntent = contentIntent,
        )

    internal fun pikminBloomPayload(
        language: LiveStatusNotificationParser.PikminLanguage =
            LiveStatusNotificationParser.PikminLanguage.CHINESE,
        contentIntent: PendingIntent? = null,
    ): LiveStatusPayload =
        LiveStatusPayload(
            id = PIKMIN_BLOOM_NOTIFICATION_ID,
            appName = "Pikmin Bloom",
            smallIconRes = R.drawable.ic_pikmin_flower_notification,
            leftIconRes = R.drawable.ic_pikmin_flower_notification,
            criticalText = if (language == LiveStatusNotificationParser.PikminLanguage.ENGLISH) {
                "Planting"
            } else {
                "種花中"
            },
            title = if (language == LiveStatusNotificationParser.PikminLanguage.ENGLISH) {
                "Planting flowers in the background"
            } else {
                "Pikmin Bloom 正在種花"
            },
            contentText = if (language == LiveStatusNotificationParser.PikminLanguage.ENGLISH) {
                "Remember to stop planting flowers so you don't use up petals in one place."
            } else {
                "記得結束種花，避免花瓣在原地耗盡。"
            },
            contentIntent = contentIntent,
        )

    internal fun clockTimerPayload(
        update: ClockTimerUpdate,
        nowElapsedRealtimeMillis: Long,
        contentIntent: PendingIntent? = null,
    ): LiveStatusPayload {
        val timer = LiveStatusTimer(
            state = update.state,
            endElapsedRealtimeMillis = update.endElapsedRealtimeMillis,
            remainingMillis = update.remainingMillis,
            language = update.language,
        )
        val pausedText = update.remainingMillis?.let(::formatClockTimerDuration)
        val isChinese = update.language == ClockTimerLanguage.CHINESE
        return LiveStatusPayload(
            id = CLOCK_TIMER_NOTIFICATION_ID,
            appName = if (isChinese) "時鐘" else "Clock",
            smallIconRes = R.drawable.ic_timer_notification,
            leftIconRes = R.drawable.ic_timer_notification,
            criticalText = formatClockTimerCriticalText(
                when (update.state) {
                    ClockTimerState.RUNNING ->
                        requireNotNull(update.endElapsedRealtimeMillis) - nowElapsedRealtimeMillis
                    ClockTimerState.PAUSED -> requireNotNull(update.remainingMillis)
                },
                update.language,
            ),
            title = if (isChinese) "時鐘倒數計時" else "Clock timer",
            contentText = if (pausedText != null) {
                if (isChinese) "剩餘 $pausedText（已暫停）" else "$pausedText remaining (paused)"
            } else {
                if (isChinese) "倒數進行中" else "Timer running"
            },
            timer = timer,
            contentIntent = contentIntent,
        )
    }

    internal fun formatClockTimerDuration(durationMillis: Long): String {
        val totalSeconds = ((durationMillis.coerceAtLeast(0) + 999) / 1_000)
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    internal fun youBikeShortCriticalText(estimate: YouBikeFareEstimate?): String? =
        estimate?.let { "NT$${it.amount}" }

    internal fun formatClockTimerCriticalText(
        remainingMillis: Long,
        language: ClockTimerLanguage = ClockTimerLanguage.ENGLISH,
    ): String {
        val safeRemainingMillis = remainingMillis.coerceAtLeast(0)
        val isChinese = language == ClockTimerLanguage.CHINESE
        return if (safeRemainingMillis >= 60_000L) {
            val minutes = (safeRemainingMillis + 59_999L) / 60_000L
            if (isChinese) "$minutes 分" else "$minutes min"
        } else {
            val seconds = (safeRemainingMillis + 999L) / 1_000L
            if (isChinese) "$seconds 秒" else "$seconds s"
        }
    }

    private fun uberRideTitle(update: LiveStatusNotificationParser.UberRideUpdate): String =
        if (update.rideType == LiveStatusNotificationParser.UberRideType.UBER_TAXI) {
            when (update.event) {
                LiveStatusNotificationParser.UberRideEvent.PICKUP_EN_ROUTE ->
                    "職業駕駛正在前往上車點"
                LiveStatusNotificationParser.UberRideEvent.PICKUP_APPROACHING ->
                    "職業駕駛已在附近"
                LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY ->
                    "職業駕駛即將抵達"
                else -> "職業駕駛行程"
            }
        } else {
            update.title ?: uberRideFallbackTitle(update.event)
        }

    private fun uberRideContentText(update: LiveStatusNotificationParser.UberRideUpdate): String {
        if (update.rideType == LiveStatusNotificationParser.UberRideType.UBER_TAXI) {
            return update.officialText ?: uberTaxiFallbackContentText(update)
        }
        return when (update.event) {
            LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY,
            LiveStatusNotificationParser.UberRideEvent.ARRIVED,
            -> uberRideVehicleText(update)
            LiveStatusNotificationParser.UberRideEvent.ON_TRIP ->
                update.dropoffPoint ?: "Heading to your destination"
            else -> update.pickupPoint ?: "Meet your driver at the pickup point"
        }
    }

    private fun uberTaxiFallbackContentText(
        update: LiveStatusNotificationParser.UberRideUpdate,
    ): String =
        when (update.event) {
            LiveStatusNotificationParser.UberRideEvent.PICKUP_EN_ROUTE ->
                update.pickupEtaMinutes?.let { "職業駕駛將在 $it 分鐘內抵達。" }
                    ?: "職業駕駛正在前往上車點。"
            LiveStatusNotificationParser.UberRideEvent.PICKUP_APPROACHING ->
                "請準備好與職業駕駛碰面。"
            LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY ->
                uberRideVehicleText(update)
            else -> "請開啟 Uber 查看最新行程資訊。"
        }

    private fun uberRideVehicleText(update: LiveStatusNotificationParser.UberRideUpdate): String {
        val vehicle = listOfNotNull(update.plate, update.vehicle)
            .joinToString(" · ")
            .takeIf { it.isNotBlank() }
            ?: "Check vehicle details in Uber"
        return update.pin?.let { "$vehicle · PIN $it" } ?: vehicle
    }

    private fun uberRideFallbackTitle(event: LiveStatusNotificationParser.UberRideEvent): String =
        when (event) {
            LiveStatusNotificationParser.UberRideEvent.PICKUP_APPROACHING ->
                "Driver is approaching"
            LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY -> "Driver is nearby"
            LiveStatusNotificationParser.UberRideEvent.ARRIVED -> "Driver arrived"
            LiveStatusNotificationParser.UberRideEvent.ON_TRIP -> "On your way"
            else -> "Driver on the way"
        }

    private fun uberRideShortText(update: LiveStatusNotificationParser.UberRideUpdate): String =
        if (update.rideType == LiveStatusNotificationParser.UberRideType.UBER_TAXI) {
            when (update.event) {
                LiveStatusNotificationParser.UberRideEvent.PICKUP_EN_ROUTE ->
                    update.pickupEtaMinutes?.let { "$it 分鐘" } ?: "前往上車點"
                LiveStatusNotificationParser.UberRideEvent.PICKUP_APPROACHING -> "已在附近"
                LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY -> "即將抵達"
                else -> "行程中"
            }
        } else {
            if (
                update.language ==
                LiveStatusNotificationParser.UberRideLanguage.TRADITIONAL_CHINESE
            ) {
                when (update.event) {
                    LiveStatusNotificationParser.UberRideEvent.PICKUP_APPROACHING -> "接近中"
                    LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY -> "即將抵達"
                    LiveStatusNotificationParser.UberRideEvent.ARRIVED -> "已抵達"
                    LiveStatusNotificationParser.UberRideEvent.ON_TRIP -> "行程中"
                    else -> update.pickupEtaMinutes?.let { "$it 分鐘" } ?: "前往上車點"
                }
            } else {
                when (update.event) {
                    LiveStatusNotificationParser.UberRideEvent.PICKUP_APPROACHING -> "Approaching"
                    LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY -> "Nearby"
                    LiveStatusNotificationParser.UberRideEvent.ARRIVED -> "Arrived"
                    LiveStatusNotificationParser.UberRideEvent.ON_TRIP ->
                        dropoffTimeText(update.title) ?: "On trip"
                    else -> pickupEtaText(update.title) ?: "To pickup"
                }
            }
        }

    private fun pickupEtaText(title: String?): String? =
        title?.let {
            Regex("""(?i)\bpick up in (\d+\s*min)\b""")
                .find(it)
                ?.groupValues
                ?.getOrNull(1)
                ?.replace(Regex("""\s+"""), " ")
        }

    private fun dropoffTimeText(title: String?): String? =
        title?.let {
            Regex("""(?i)\bdropoff at (\d{1,2}:\d{2}\s*(?:AM|PM)?)\b""")
                .find(it)
                ?.groupValues
                ?.getOrNull(1)
                ?.replace(Regex("""\s+"""), " ")
        }

    private fun uberRideProgress(update: LiveStatusNotificationParser.UberRideUpdate): Int =
        when (update.event) {
            LiveStatusNotificationParser.UberRideEvent.PICKUP_APPROACHING -> 40
            LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY -> 50
            LiveStatusNotificationParser.UberRideEvent.ARRIVED -> 60
            LiveStatusNotificationParser.UberRideEvent.ON_TRIP -> 80
            else -> 25
        }

    private fun requestPromotedOngoing(builder: Notification.Builder) {
        builder.extras.putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true)
    }

    internal fun mediaShortCriticalText(title: String): String {
        val graphemeIterator = BreakIterator.getCharacterInstance(Locale.ROOT).apply {
            setText(title)
        }
        var graphemeCount = 0
        var usedWidth = 0
        var acceptedEnd = graphemeIterator.first()
        var graphemeEnd = graphemeIterator.next()
        while (
            graphemeEnd != BreakIterator.DONE &&
            graphemeCount < MEDIA_SHORT_CRITICAL_TEXT_MAX_GRAPHEMES
        ) {
            val graphemeWidth = mediaCriticalTextGraphemeWidth(
                title = title,
                startIndex = acceptedEnd,
                endIndex = graphemeEnd,
            )
            if (usedWidth + graphemeWidth > MEDIA_SHORT_CRITICAL_TEXT_MAX_WIDTH) break
            usedWidth += graphemeWidth
            graphemeCount += 1
            acceptedEnd = graphemeEnd
            graphemeEnd = graphemeIterator.next()
        }
        return title.substring(0, acceptedEnd)
    }

    private fun mediaCriticalTextGraphemeWidth(
        title: String,
        startIndex: Int,
        endIndex: Int,
    ): Int {
        var index = startIndex
        while (index < endIndex) {
            val codePoint = title.codePointAt(index)
            if (codePoint.isWideMediaCharacter()) return MEDIA_SHORT_CRITICAL_TEXT_WIDE_WIDTH
            index += Character.charCount(codePoint)
        }
        return MEDIA_SHORT_CRITICAL_TEXT_NARROW_WIDTH
    }

    private fun Int.isWideMediaCharacter(): Boolean {
        val script = Character.UnicodeScript.of(this)
        return Character.isIdeographic(this) ||
            script == Character.UnicodeScript.HAN ||
            script == Character.UnicodeScript.HANGUL ||
            script == Character.UnicodeScript.HIRAGANA ||
            script == Character.UnicodeScript.KATAKANA ||
            this in 0x2E80..0xA4CF ||
            this in 0xFE10..0xFE6F ||
            this in 0xFF01..0xFF60 ||
            this in 0xFFE0..0xFFE6 ||
            Character.getType(this) == Character.OTHER_SYMBOL.toInt()
    }

    private fun mediaAction(
        context: Context,
        title: String,
        pendingIntent: PendingIntent,
        semanticAction: Int,
    ): Notification.Action {
        val builder = Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_music_notification),
            title,
            pendingIntent,
        )
        if (Build.VERSION.SDK_INT >= 37) builder.setSemanticAction(semanticAction)
        return builder.build()
    }

    private val MediaPlaybackControl.titleResource: Int
        get() = when (this) {
            MediaPlaybackControl.SKIP_PREVIOUS -> R.string.media_playback_previous
            MediaPlaybackControl.SEEK_BACK_15_SECONDS -> R.string.media_playback_back_15_seconds
            MediaPlaybackControl.REWIND -> R.string.media_playback_rewind
            MediaPlaybackControl.SKIP_NEXT -> R.string.media_playback_next
            MediaPlaybackControl.SEEK_FORWARD_15_SECONDS ->
                R.string.media_playback_forward_15_seconds
            MediaPlaybackControl.FAST_FORWARD -> R.string.media_playback_fast_forward
        }

    private fun Int.normalizedNotificationVisibility(): Int = when (this) {
        Notification.VISIBILITY_PUBLIC,
        Notification.VISIBILITY_PRIVATE,
        Notification.VISIBILITY_SECRET,
        -> this
        else -> Notification.VISIBILITY_PRIVATE
    }

    private fun notificationManager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)

    private const val MEDIA_SHORT_CRITICAL_TEXT_MAX_GRAPHEMES = 7
    private const val MEDIA_SHORT_CRITICAL_TEXT_MAX_WIDTH = 8
    private const val MEDIA_SHORT_CRITICAL_TEXT_NARROW_WIDTH = 1
    private const val MEDIA_SHORT_CRITICAL_TEXT_WIDE_WIDTH = 2
}

internal object YouBikeNotificationStyle {
    fun apply(
        builder: Notification.Builder,
        session: YouBikeRideSession,
        estimate: YouBikeFareEstimate?,
        nowMillis: Long,
    ) {
        val borrowedElapsedRealtime = SystemClock.elapsedRealtime() -
            (nowMillis - session.borrowedAtMillis).coerceAtLeast(0L)
        if (Build.VERSION.SDK_INT >= 37) {
            val stopwatch = Notification.Metric(
                Notification.Metric.TimeDifference.forStopwatch(
                    borrowedElapsedRealtime,
                    Notification.Metric.TimeDifference.FORMAT_CHRONOMETER,
                ),
                "騎乘時間",
            )
            val style = Notification.MetricStyle()
            if (estimate != null) {
                style
                    .addMetric(Notification.Metric(Notification.Metric.FixedInt(estimate.amount, "元"), "預估費用"))
                    .addMetric(stopwatch)
                    .setCriticalMetric(0)
            } else {
                style.addMetric(stopwatch).setCriticalMetric(0)
            }
            builder.setStyle(style)
        } else {
            builder
                .setWhen(session.borrowedAtMillis)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(false)
        }
    }
}
