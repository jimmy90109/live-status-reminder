package com.example.ridecodereminder;

import java.util.Locale;

public final class RideNotificationParser {
    public enum RideEvent {
        NONE,
        ENTERED,
        EXITED
    }

    private RideNotificationParser() {
    }

    public static RideEvent parse(String notificationText) {
        if (notificationText == null) {
            return RideEvent.NONE;
        }

        String normalized = notificationText.toLowerCase(Locale.ROOT);
        if (!normalized.contains("乘車碼交易")) {
            return RideEvent.NONE;
        }
        if (normalized.contains("出站交易已完成")) {
            return RideEvent.EXITED;
        }
        if (normalized.contains("尚未出站")) {
            return RideEvent.ENTERED;
        }
        return RideEvent.NONE;
    }
}
