package com.example.ridecodereminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Build;

public final class RideReminder {
    private static final String CHANNEL_ID = "live_status";
    private static final int RIDE_NOTIFICATION_ID = 1001;
    private static final int FOODPANDA_NOTIFICATION_ID = 1002;
    private static final int UBER_EATS_NOTIFICATION_ID = 1003;
    private static final String EXTRA_REQUEST_PROMOTED_ONGOING =
            "android.requestPromotedOngoing";

    private RideReminder() {
    }

    public static void createChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_description));
        channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);
        manager.createNotificationChannel(channel);
    }

    public static void show(Context context) {
        createChannel(context);
        PendingIntent openIpass = PendingIntent.getActivity(
                context,
                0,
                MainActivity.createOpenIpassIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("乘車中：準備下車時開啟乘車碼")
                .setContentText("點一下立即開啟 iPASS MONEY")
                .setContentIntent(openIpass)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.ic_notification),
                        "開啟乘車碼",
                        openIpass
                ).build())
                .setCategory(Notification.CATEGORY_NAVIGATION)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        builder.setStyle(new Notification.BigTextStyle()
                .bigText("抵達目的地前，點一下立即開啟 iPASS MONEY，準備出示乘車碼。"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setShortCriticalText("乘車中");
            requestPromotedOngoing(builder);
        }

        context.getSystemService(NotificationManager.class).notify(
                RIDE_NOTIFICATION_ID,
                builder.build()
        );
    }

    public static void clear(Context context) {
        context.getSystemService(NotificationManager.class).cancel(RIDE_NOTIFICATION_ID);
    }

    public static void showFoodpanda(
            Context context,
            RideNotificationParser.FoodpandaEvent event
    ) {
        createChannel(context);

        String title;
        String text;
        String shortText;
        if (event == RideNotificationParser.FoodpandaEvent.COURIER_ARRIVING) {
            title = "foodpanda 即將抵達";
            text = "外送夥伴即將抵達，準備取餐。";
            shortText = "即將抵達";
        } else {
            title = "foodpanda 外送中";
            text = "外送夥伴在路上，請留意手機來電或訊息。";
            shortText = "外送中";
        }

        PendingIntent openFoodpanda = PendingIntent.getActivity(
                context,
                1,
                MainActivity.createOpenFoodpandaIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_food_delivery_notification)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(openFoodpanda)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(context, R.drawable.ic_food_delivery_notification),
                        "開啟 foodpanda",
                        openFoodpanda
                ).build())
                .setCategory(Notification.CATEGORY_STATUS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .setStyle(new Notification.BigTextStyle().bigText(text));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setShortCriticalText(shortText);
            requestPromotedOngoing(builder);
        }

        context.getSystemService(NotificationManager.class).notify(
                FOODPANDA_NOTIFICATION_ID,
                builder.build()
        );
    }

    public static void clearFoodpanda(Context context) {
        context.getSystemService(NotificationManager.class).cancel(FOODPANDA_NOTIFICATION_ID);
    }

    public static void showUberEats(
            Context context,
            RideNotificationParser.UberEatsEvent event,
            String pin
    ) {
        createChannel(context);

        String title = uberEatsTitle(event);
        String statusText = uberEatsStatusText(event);
        String privateText = pin == null ? statusText : statusText + " · PIN " + pin;
        PendingIntent openUberEats = PendingIntent.getActivity(
                context,
                2,
                MainActivity.createOpenUberEatsIntent(context),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder publicBuilder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_food_delivery_notification)
                .setContentTitle(title)
                .setContentText(statusText)
                .setContentIntent(openUberEats)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC);
        applyUberEatsStyle(publicBuilder, event, statusText);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            publicBuilder.setShortCriticalText(uberEatsShortText(event));
            requestPromotedOngoing(publicBuilder);
        }

        Notification.Builder builder = new Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_food_delivery_notification)
                .setContentTitle(title)
                .setContentText(privateText)
                .setContentIntent(openUberEats)
                .addAction(new Notification.Action.Builder(
                        Icon.createWithResource(
                                context,
                                R.drawable.ic_food_delivery_notification
                        ),
                        "開啟 Uber Eats",
                        openUberEats
                ).build())
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(publicBuilder.build());
        applyUberEatsStyle(builder, event, privateText);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            builder.setShortCriticalText(pin == null ? uberEatsShortText(event) : pin);
            requestPromotedOngoing(builder);
        }

        context.getSystemService(NotificationManager.class).notify(
                UBER_EATS_NOTIFICATION_ID,
                builder.build()
        );
    }

    public static void clearUberEats(Context context) {
        context.getSystemService(NotificationManager.class).cancel(UBER_EATS_NOTIFICATION_ID);
    }

    private static void applyUberEatsStyle(
            Notification.Builder builder,
            RideNotificationParser.UberEatsEvent event,
            String expandedText
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            int uberGreen = Color.rgb(6, 193, 103);
            Notification.ProgressStyle style = new Notification.ProgressStyle()
                    .setStyledByProgress(true)
                    .setProgress(uberEatsProgress(event));
            for (int index = 0; index < 5; index++) {
                style.addProgressSegment(
                        new Notification.ProgressStyle.Segment(20)
                                .setId(index + 1)
                                .setColor(uberGreen)
                );
            }
            builder.setStyle(style);
        } else {
            builder.setStyle(new Notification.BigTextStyle().bigText(expandedText));
        }
    }

    private static int uberEatsProgress(RideNotificationParser.UberEatsEvent event) {
        switch (event) {
            case PREPARING:
                return 40;
            case PICKING_UP:
                return 60;
            case ON_THE_WAY:
                return 80;
            case ARRIVING:
                return 100;
            case ORDER_RECEIVED:
            default:
                return 20;
        }
    }

    private static String uberEatsTitle(RideNotificationParser.UberEatsEvent event) {
        switch (event) {
            case PREPARING:
                return "Uber Eats 正在準備訂單";
            case PICKING_UP:
                return "Uber Eats 正在取餐";
            case ON_THE_WAY:
                return "Uber Eats 配送中";
            case ARRIVING:
                return "Uber Eats 快到了！";
            case ORDER_RECEIVED:
            default:
                return "Uber Eats 訂單已收到";
        }
    }

    private static String uberEatsStatusText(RideNotificationParser.UberEatsEvent event) {
        switch (event) {
            case PREPARING:
                return "店家正在準備您的餐點。";
            case PICKING_UP:
                return "外送夥伴正在取餐。";
            case ON_THE_WAY:
                return "外送夥伴正前往您所在位置。";
            case ARRIVING:
                return "外送夥伴即將抵達，請準備取餐。";
            case ORDER_RECEIVED:
            default:
                return "店家已收到您的訂單。";
        }
    }

    private static String uberEatsShortText(RideNotificationParser.UberEatsEvent event) {
        switch (event) {
            case PREPARING:
                return "備餐中";
            case PICKING_UP:
                return "取餐中";
            case ON_THE_WAY:
                return "配送中";
            case ARRIVING:
                return "快到了";
            case ORDER_RECEIVED:
            default:
                return "已接單";
        }
    }

    private static void requestPromotedOngoing(Notification.Builder builder) {
        // Android 16 QPR exposes a typed setter. The extra is the documented
        // compatible request mechanism and also works when compiling against API 36.
        builder.getExtras().putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true);
    }
}
