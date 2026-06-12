package com.example.ridecodereminder;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.drawable.Icon;
import android.os.Build;

public final class RideReminder {
    private static final String CHANNEL_ID = "ride_reminder";
    private static final int NOTIFICATION_ID = 1001;
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
                .setShortCriticalText("乘車中")
                .setVisibility(Notification.VISIBILITY_PUBLIC);

        builder.setStyle(new Notification.BigTextStyle()
                .bigText("抵達目的地前，點一下立即開啟 iPASS MONEY，準備出示乘車碼。"));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            // Android 16 QPR exposes a typed setter. The extra is the documented
            // compatible request mechanism and also works when compiling against API 36.
            builder.getExtras().putBoolean(EXTRA_REQUEST_PROMOTED_ONGOING, true);
        }

        context.getSystemService(NotificationManager.class).notify(
                NOTIFICATION_ID,
                builder.build()
        );
    }

    public static void clear(Context context) {
        context.getSystemService(NotificationManager.class).cancel(NOTIFICATION_ID);
    }
}
