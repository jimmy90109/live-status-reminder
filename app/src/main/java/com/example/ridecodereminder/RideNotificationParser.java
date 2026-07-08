package com.example.ridecodereminder;

import java.util.Locale;

public final class RideNotificationParser {
    public enum RideEvent {
        NONE,
        ENTERED,
        EXITED
    }

    public enum FoodpandaEvent {
        NONE,
        COURIER_ON_THE_WAY,
        COURIER_ARRIVING,
        ORDER_ENDED
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

    public static FoodpandaEvent parseFoodpanda(String notificationText) {
        if (notificationText == null) {
            return FoodpandaEvent.NONE;
        }

        String normalized = notificationText.toLowerCase(Locale.ROOT);
        if (normalized.contains("外送夥伴即將抵達")) {
            return FoodpandaEvent.COURIER_ARRIVING;
        }
        if (normalized.contains("外送夥伴在路上")) {
            return FoodpandaEvent.COURIER_ON_THE_WAY;
        }
        if (normalized.contains("已送達")
                || normalized.contains("訂單完成")
                || normalized.contains("已取消")
                || normalized.contains("訂單取消")) {
            return FoodpandaEvent.ORDER_ENDED;
        }
        return FoodpandaEvent.NONE;
    }
}
