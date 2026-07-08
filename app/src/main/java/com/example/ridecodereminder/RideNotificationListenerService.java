package com.example.ridecodereminder;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class RideNotificationListenerService extends NotificationListenerService {
    private static final String IPASS_PACKAGE = "com.ipass.ipassmoney";
    private static final String FOODPANDA_PACKAGE = "com.global.foodpanda.android";

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        String notificationText = readNotificationText(statusBarNotification.getNotification());
        String packageName = statusBarNotification.getPackageName();
        if (IPASS_PACKAGE.equals(packageName)) {
            handleRideNotification(notificationText);
        } else if (FOODPANDA_PACKAGE.equals(packageName)) {
            handleFoodpandaNotification(notificationText);
        }
    }

    private void handleRideNotification(String notificationText) {
        RideNotificationParser.RideEvent event = RideNotificationParser.parse(notificationText);
        if (event == RideNotificationParser.RideEvent.ENTERED) {
            RideReminder.show(this);
        } else if (event == RideNotificationParser.RideEvent.EXITED) {
            RideReminder.clear(this);
        }
    }

    private void handleFoodpandaNotification(String notificationText) {
        RideNotificationParser.FoodpandaEvent event =
                RideNotificationParser.parseFoodpanda(notificationText);
        if (event == RideNotificationParser.FoodpandaEvent.COURIER_ON_THE_WAY
                || event == RideNotificationParser.FoodpandaEvent.COURIER_ARRIVING) {
            RideReminder.showFoodpanda(this, event);
        } else if (event == RideNotificationParser.FoodpandaEvent.ORDER_ENDED) {
            RideReminder.clearFoodpanda(this);
        }
    }

    static String readNotificationText(Notification notification) {
        Bundle extras = notification.extras;
        return join(
                extras.getCharSequence(Notification.EXTRA_TITLE),
                extras.getCharSequence(Notification.EXTRA_TEXT),
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
                extras.getCharSequence(Notification.EXTRA_SUB_TEXT)
        );
    }

    private static String join(CharSequence... values) {
        StringBuilder result = new StringBuilder();
        for (CharSequence value : values) {
            if (value != null) {
                result.append(value).append('\n');
            }
        }
        return result.toString();
    }
}
