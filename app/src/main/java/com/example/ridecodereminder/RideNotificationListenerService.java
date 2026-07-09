package com.example.ridecodereminder;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public final class RideNotificationListenerService extends NotificationListenerService {
    private static final String IPASS_PACKAGE = "com.ipass.ipassmoney";
    private static final String FOODPANDA_PACKAGE = "com.global.foodpanda.android";
    private static final String UBER_EATS_PACKAGE = "com.ubercab.eats";

    private RideNotificationParser.UberEatsEvent lastUberEatsEvent =
            RideNotificationParser.UberEatsEvent.NONE;
    private String lastUberEatsPin;

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        Notification notification = statusBarNotification.getNotification();
        String notificationText = readNotificationText(notification);
        String packageName = statusBarNotification.getPackageName();
        if (IPASS_PACKAGE.equals(packageName)) {
            handleRideNotification(notificationText);
        } else if (FOODPANDA_PACKAGE.equals(packageName)) {
            handleFoodpandaNotification(notificationText);
        } else if (UBER_EATS_PACKAGE.equals(packageName)) {
            handleUberEatsNotification(notificationText, readShortCriticalText(notification));
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

    private void handleUberEatsNotification(
            String notificationText,
            String shortCriticalText
    ) {
        RideNotificationParser.UberEatsUpdate update =
                RideNotificationParser.parseUberEats(notificationText, shortCriticalText);
        RideNotificationParser.UberEatsEvent event = update.getEvent();

        if (event == RideNotificationParser.UberEatsEvent.ORDER_ENDED) {
            lastUberEatsEvent = RideNotificationParser.UberEatsEvent.NONE;
            lastUberEatsPin = null;
            RideReminder.clearUberEats(this);
            return;
        }

        if (event == RideNotificationParser.UberEatsEvent.ORDER_RECEIVED) {
            lastUberEatsEvent = event;
            lastUberEatsPin = update.getPin();
        } else {
            if (update.getPin() != null) {
                lastUberEatsPin = update.getPin();
            }
            if (event != RideNotificationParser.UberEatsEvent.NONE
                    && eventRank(event) >= eventRank(lastUberEatsEvent)) {
                lastUberEatsEvent = event;
            }
        }

        if (lastUberEatsEvent != RideNotificationParser.UberEatsEvent.NONE
                && (event != RideNotificationParser.UberEatsEvent.NONE
                || update.getPin() != null)) {
            RideReminder.showUberEats(this, lastUberEatsEvent, lastUberEatsPin);
        }
    }

    private static int eventRank(RideNotificationParser.UberEatsEvent event) {
        switch (event) {
            case ORDER_RECEIVED:
                return 1;
            case PREPARING:
                return 2;
            case PICKING_UP:
                return 3;
            case ON_THE_WAY:
                return 4;
            case ARRIVING:
                return 5;
            default:
                return 0;
        }
    }

    static String readNotificationText(Notification notification) {
        Bundle extras = notification.extras;
        String text = join(
                extras.getCharSequence(Notification.EXTRA_TITLE),
                extras.getCharSequence(Notification.EXTRA_TEXT),
                extras.getCharSequence(Notification.EXTRA_BIG_TEXT),
                extras.getCharSequence(Notification.EXTRA_SUB_TEXT),
                extras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT)
        );
        CharSequence[] lines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES);
        return lines == null ? text : text + join(lines);
    }

    static String readShortCriticalText(Notification notification) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.BAKLAVA) {
            return notification.getShortCriticalText();
        }
        return null;
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
