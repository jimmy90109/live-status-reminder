package com.github.jimmy90109.livestatus

import java.util.Locale

object LiveStatusNotificationParser {
    private val fourDigitPin = Regex("""^\s*(\d{4})\s*$""")

    enum class RideEvent {
        NONE,
        ENTERED,
        EXITED,
    }

    enum class FoodpandaEvent {
        NONE,
        COURIER_ON_THE_WAY,
        COURIER_ARRIVING,
        ORDER_ENDED,
    }

    enum class UberEatsEvent {
        NONE,
        ORDER_RECEIVED,
        PREPARING,
        PICKING_UP,
        ON_THE_WAY,
        ARRIVING,
        ORDER_ENDED,
    }

    data class UberEatsUpdate(
        val event: UberEatsEvent,
        val pin: String?,
    )

    @JvmStatic
    fun parse(notificationText: String?): RideEvent {
        if (notificationText == null) return RideEvent.NONE

        val normalized = notificationText.lowercase(Locale.ROOT)
        if (!normalized.contains("乘車碼交易")) return RideEvent.NONE
        if (normalized.contains("出站交易已完成")) return RideEvent.EXITED
        if (normalized.contains("尚未出站")) return RideEvent.ENTERED
        return RideEvent.NONE
    }

    @JvmStatic
    fun parseFoodpanda(notificationText: String?): FoodpandaEvent {
        if (notificationText == null) return FoodpandaEvent.NONE

        val normalized = notificationText.lowercase(Locale.ROOT)
        return when {
            normalized.contains("外送夥伴即將抵達") -> FoodpandaEvent.COURIER_ARRIVING
            normalized.contains("外送夥伴在路上") -> FoodpandaEvent.COURIER_ON_THE_WAY
            normalized.contains("已送達") ||
                normalized.contains("訂單完成") ||
                normalized.contains("已取消") ||
                normalized.contains("訂單取消") -> FoodpandaEvent.ORDER_ENDED
            else -> FoodpandaEvent.NONE
        }
    }

    @JvmStatic
    fun parseUberEats(
        notificationText: String?,
        shortCriticalText: String?,
    ): UberEatsUpdate {
        val normalized = notificationText?.lowercase(Locale.ROOT).orEmpty()
        val event = when {
            normalized.contains("訂單已送達") ||
                normalized.contains("已送達") ||
                normalized.contains("訂單已取消") ||
                normalized.contains("已取消") ||
                normalized.contains("訂單取消") -> UberEatsEvent.ORDER_ENDED
            normalized.contains("快到了") -> UberEatsEvent.ARRIVING
            normalized.contains("正前往您所在位置") ||
                normalized.contains("正在前往您所在位置") ||
                normalized.contains("即將抵達") -> UberEatsEvent.ON_THE_WAY
            normalized.contains("正在取餐") -> UberEatsEvent.PICKING_UP
            normalized.contains("正在準備訂單") ||
                normalized.contains("準備訂單") -> UberEatsEvent.PREPARING
            normalized.contains("訂單已收到") ||
                normalized.contains("已收到您的訂單") -> UberEatsEvent.ORDER_RECEIVED
            else -> UberEatsEvent.NONE
        }

        return UberEatsUpdate(
            event = event,
            pin = exactPin(shortCriticalText),
        )
    }

    private fun exactPin(value: String?): String? =
        value?.let { fourDigitPin.matchEntire(it)?.groupValues?.get(1) }
}
