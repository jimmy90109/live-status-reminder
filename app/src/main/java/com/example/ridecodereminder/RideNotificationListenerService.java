package com.example.ridecodereminder;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class RideNotificationListenerService extends NotificationListenerService {
    private static final String IPASS_PACKAGE = "com.ipass.ipassmoney";

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        if (!IPASS_PACKAGE.equals(statusBarNotification.getPackageName())) {
            return;
        }

        RideNotificationParser.RideEvent event = RideNotificationParser.parse(
                readNotificationText(statusBarNotification.getNotification())
        );
        if (event == RideNotificationParser.RideEvent.ENTERED) {
            RideReminder.show(this);
        } else if (event == RideNotificationParser.RideEvent.EXITED) {
            RideReminder.clear(this);
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
