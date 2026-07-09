package com.example.ridecodereminder;

import java.util.Locale;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RideNotificationParser {
    private static final Pattern FOUR_DIGIT_PIN = Pattern.compile("^\\s*(\\d{4})\\s*$");
    private static final Pattern CONTEXTUAL_PIN = Pattern.compile(
            "(?iu)(?:pin|驗證碼|取餐碼|交付碼)\\s*[:：#-]?\\s*(\\d{4})(?!\\d)"
    );

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

    public enum UberEatsEvent {
        NONE,
        ORDER_RECEIVED,
        PREPARING,
        PICKING_UP,
        ON_THE_WAY,
        ARRIVING,
        ORDER_ENDED
    }

    public static final class UberEatsUpdate {
        private final UberEatsEvent event;
        private final String pin;

        UberEatsUpdate(UberEatsEvent event, String pin) {
            this.event = event;
            this.pin = pin;
        }

        public UberEatsEvent getEvent() {
            return event;
        }

        public String getPin() {
            return pin;
        }
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

    public static UberEatsUpdate parseUberEats(
            String notificationText,
            String shortCriticalText
    ) {
        String normalized = notificationText == null
                ? ""
                : notificationText.toLowerCase(Locale.ROOT);

        UberEatsEvent event = UberEatsEvent.NONE;
        if (normalized.contains("訂單已送達")
                || normalized.contains("已送達")
                || normalized.contains("訂單已取消")
                || normalized.contains("已取消")
                || normalized.contains("訂單取消")) {
            event = UberEatsEvent.ORDER_ENDED;
        } else if (normalized.contains("快到了")) {
            event = UberEatsEvent.ARRIVING;
        } else if (normalized.contains("正前往您所在位置")
                || normalized.contains("正在前往您所在位置")
                || normalized.contains("即將抵達")) {
            event = UberEatsEvent.ON_THE_WAY;
        } else if (normalized.contains("正在取餐")) {
            event = UberEatsEvent.PICKING_UP;
        } else if (normalized.contains("正在準備訂單")
                || normalized.contains("準備訂單")) {
            event = UberEatsEvent.PREPARING;
        } else if (normalized.contains("訂單已收到")
                || normalized.contains("已收到您的訂單")) {
            event = UberEatsEvent.ORDER_RECEIVED;
        }

        String pin = exactPin(shortCriticalText);
        if (pin == null) {
            pin = contextualPin(notificationText);
        }
        return new UberEatsUpdate(event, pin);
    }

    private static String exactPin(String value) {
        if (value == null) {
            return null;
        }
        Matcher matcher = FOUR_DIGIT_PIN.matcher(value);
        return matcher.matches() ? matcher.group(1) : null;
    }

    private static String contextualPin(String value) {
        if (value == null) {
            return null;
        }
        Set<String> candidates = new LinkedHashSet<>();
        Matcher matcher = CONTEXTUAL_PIN.matcher(value);
        while (matcher.find()) {
            candidates.add(matcher.group(1));
        }
        return candidates.size() == 1 ? candidates.iterator().next() : null;
    }
}
