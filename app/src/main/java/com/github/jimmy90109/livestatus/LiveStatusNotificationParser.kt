package com.github.jimmy90109.livestatus

import java.util.Locale

object LiveStatusNotificationParser {
    private val fourDigitPin = Regex("""^\s*(\d{4})\s*$""")
    private val separatedPinDigits = Regex(
        """(?m)(?:^|\n)\s*(\d)\s*\n\s*(\d)\s*\n\s*(\d)\s*\n\s*(\d)\s*(?:\n|$)""",
    )

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

    enum class UberRideEvent {
        NONE,
        PICKUP_EN_ROUTE,
        PICKUP_NEARBY,
        ARRIVED,
        ON_TRIP,
        TRIP_ENDED,
    }

    enum class PikminEvent {
        NONE,
        FLOWER_PLANTING,
    }

    data class UberEatsUpdate(
        val event: UberEatsEvent,
        val pin: String?,
    )

    data class UberRideUpdate(
        val event: UberRideEvent,
        val title: String? = null,
        val pickupPoint: String? = null,
        val dropoffPoint: String? = null,
        val plate: String? = null,
        val vehicle: String? = null,
        val pin: String? = null,
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
            pin = exactPin(shortCriticalText) ?: separatedPin(notificationText),
        )
    }

    @JvmStatic
    fun parseUberRide(
        notificationText: String?,
        shortCriticalText: String?,
    ): UberRideUpdate {
        val lines = notificationText.cleanLines()
        val normalized = lines.joinToString("\n").lowercase(Locale.ROOT)

        if (normalized.contains("rate your trip")) {
            return UberRideUpdate(event = UberRideEvent.TRIP_ENDED)
        }

        val title = lines.firstOrNull { line ->
            line.contains(Regex("""(?i)\bpick up in \d+\s*min\b""")) ||
                line.contains(Regex("""(?i)\bdropoff at \d{1,2}:\d{2}\s*(?:AM|PM)?\b""")) ||
                line.contains(Regex("""(?i)\barrived\b"""))
        }
        val pickupPoint = lines.firstTextAfter("Meet at ")
        val dropoffPoint = lines.firstTextAfter("Heading to ")
        val vehicleDetails = lines.firstNotNullOfOrNull(::parseVehicleDetails)
        val pin = exactPin(shortCriticalText) ?: separatedPin(notificationText)
        val hasPickupEta = title?.contains(Regex("""(?i)\bpick up in \d+\s*min\b""")) == true

        val event = when {
            title?.contains(Regex("""(?i)\bdropoff at\b""")) == true ||
                dropoffPoint != null -> UberRideEvent.ON_TRIP
            title?.contains(Regex("""(?i)\barrived\b""")) == true -> UberRideEvent.ARRIVED
            hasPickupEta &&
                pickupPoint == null &&
                (vehicleDetails != null || pin != null) -> UberRideEvent.PICKUP_NEARBY
            hasPickupEta && pickupPoint != null -> UberRideEvent.PICKUP_EN_ROUTE
            else -> UberRideEvent.NONE
        }

        return UberRideUpdate(
            event = event,
            title = title,
            pickupPoint = pickupPoint,
            dropoffPoint = dropoffPoint,
            plate = vehicleDetails?.first,
            vehicle = vehicleDetails?.second,
            pin = pin,
        )
    }

    @JvmStatic
    fun parsePikminBloom(notificationText: String?): PikminEvent {
        if (notificationText == null) return PikminEvent.NONE

        val normalized = notificationText.lowercase(Locale.ROOT)
        return if (
            normalized.contains("正在背景執行時種花") ||
            normalized.contains("正在背景执行时种花")
        ) {
            PikminEvent.FLOWER_PLANTING
        } else {
            PikminEvent.NONE
        }
    }

    private fun exactPin(value: String?): String? =
        value?.let { fourDigitPin.matchEntire(it)?.groupValues?.get(1) }

    private fun separatedPin(value: String?): String? =
        value?.let { text ->
            separatedPinDigits.find(text)?.destructured?.let { (first, second, third, fourth) ->
                first + second + third + fourth
            }
        }

    private fun String?.cleanLines(): List<String> =
        orEmpty()
            .lineSequence()
            .map { it.replace(Regex("""\s+"""), " ").trim() }
            .filter { it.isNotEmpty() }
            .toList()

    private fun List<String>.firstTextAfter(prefix: String): String? =
        firstNotNullOfOrNull { line ->
            line.substringAfter(prefix, missingDelimiterValue = "")
                .trim()
                .takeIf { it.isNotEmpty() }
        }

    private fun parseVehicleDetails(line: String): Pair<String, String>? {
        val parts = line.split("·").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        val plate = parts.first()
        val vehicle = parts.drop(1).joinToString(" · ")
        val looksLikePlate = plate.any { it.isLetter() } && plate.any { it.isDigit() }
        return if (looksLikePlate && vehicle.any { it.isLetter() }) plate to vehicle else null
    }

}
