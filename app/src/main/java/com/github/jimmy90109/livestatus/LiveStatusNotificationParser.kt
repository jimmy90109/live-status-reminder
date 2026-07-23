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
        PICKUP_APPROACHING,
        PICKUP_NEARBY,
        ARRIVED,
        ON_TRIP,
        TRIP_ENDED,
    }

    enum class UberRideType {
        STANDARD,
        UBER_TAXI,
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
        val rideType: UberRideType = UberRideType.STANDARD,
        val title: String? = null,
        val officialText: String? = null,
        val pickupEtaMinutes: Int? = null,
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
        notificationTitle: String? = null,
        notificationContentText: String? = null,
    ): UberRideUpdate {
        val lines = notificationText.cleanLines()
        val normalized = lines.joinToString("\n").lowercase(Locale.ROOT)
        val taxiTitle = notificationTitle.cleanText()
            ?: lines.firstOrNull { it in UBER_TAXI_TITLES }
        val taxiContent = notificationContentText.cleanText()
            ?: lines.firstOrNull { line ->
                UBER_TAXI_CONTENT_MARKERS.any(line::contains)
            }

        if (taxiTitle == UBER_TAXI_TRIP_ENDED_TITLE) {
            return UberRideUpdate(
                event = UberRideEvent.TRIP_ENDED,
                rideType = UberRideType.UBER_TAXI,
                title = taxiTitle,
                officialText = notificationContentText.cleanText(),
            )
        }

        if (normalized.contains("rate your trip")) {
            return UberRideUpdate(event = UberRideEvent.TRIP_ENDED)
        }

        parseUberTaxi(taxiTitle, taxiContent, shortCriticalText, notificationText)?.let {
            return it
        }

        val title = lines.firstOrNull { line ->
            line.contains(Regex("""(?i)\bpick up in \d+\s*min\b""")) ||
                line.contains(Regex("""(?i)\bdropoff at \d{1,2}:\d{2}\s*(?:AM|PM)?\b""")) ||
                line.contains(Regex("""(?i)\barrived\b"""))
        }
        val pickupPoint = lines.firstLineStartingWith("Meet at ")
        val dropoffPoint = lines.firstTextAfter("Heading to ")
        val vehicleDetails = lines.firstNotNullOfOrNull(::parseVehicleDetails)
        val pin = exactPin(shortCriticalText) ?: separatedPin(notificationText)
        val hasPickupEta = title?.contains(Regex("""(?i)\bpick up in \d+\s*min\b""")) == true
        val pickupMinutes = title.pickupMinutes()

        val event = when {
            title?.contains(Regex("""(?i)\bdropoff at\b""")) == true ||
                dropoffPoint != null -> UberRideEvent.ON_TRIP
            title?.contains(Regex("""(?i)\barrived\b""")) == true -> UberRideEvent.ARRIVED
            hasPickupEta &&
                pickupMinutes != null &&
                pickupMinutes <= UBER_RIDE_NEARBY_MINUTES &&
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

    private fun parseUberTaxi(
        title: String?,
        contentText: String?,
        shortCriticalText: String?,
        notificationText: String?,
    ): UberRideUpdate? {
        val event = when {
            title == UBER_TAXI_PICKUP_NEARBY_TITLE -> UberRideEvent.PICKUP_NEARBY
            title == UBER_TAXI_PICKUP_APPROACHING_TITLE &&
                contentText?.contains(UBER_TAXI_MEETING_TEXT) == true ->
                UberRideEvent.PICKUP_APPROACHING
            title == UBER_TAXI_PICKUP_EN_ROUTE_TITLE &&
                UBER_TAXI_ETA.find(contentText.orEmpty()) != null ->
                UberRideEvent.PICKUP_EN_ROUTE
            else -> return null
        }
        val etaMinutes = UBER_TAXI_ETA.find(contentText.orEmpty())
            ?.groupValues
            ?.getOrNull(1)
            ?.toIntOrNull()
        val vehicleDetails = UBER_TAXI_VEHICLE.find(contentText.orEmpty())?.let { match ->
            match.groupValues[2] to match.groupValues[1].trim()
        }

        return UberRideUpdate(
            event = event,
            rideType = UberRideType.UBER_TAXI,
            title = title,
            officialText = contentText,
            pickupEtaMinutes = etaMinutes,
            plate = vehicleDetails?.first,
            vehicle = vehicleDetails?.second,
            pin = exactPin(shortCriticalText) ?: separatedPin(notificationText),
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

    private fun String?.cleanText(): String? =
        this?.replace(Regex("""\s+"""), " ")?.trim()?.takeIf { it.isNotEmpty() }

    private fun List<String>.firstTextAfter(prefix: String): String? =
        firstNotNullOfOrNull { line ->
            line.substringAfter(prefix, missingDelimiterValue = "")
                .trim()
                .takeIf { it.isNotEmpty() }
        }

    private fun List<String>.firstLineStartingWith(prefix: String): String? =
        firstOrNull { line ->
            line.startsWith(prefix) && line.length > prefix.length
        }

    private fun parseVehicleDetails(line: String): Pair<String, String>? {
        val parts = line.split("·").map { it.trim() }.filter { it.isNotEmpty() }
        if (parts.size < 2) return null
        val plate = parts.first()
        val vehicle = parts.drop(1).joinToString(" · ")
        val looksLikePlate = plate.any { it.isLetter() } && plate.any { it.isDigit() }
        return if (looksLikePlate && vehicle.any { it.isLetter() }) plate to vehicle else null
    }

    private fun String?.pickupMinutes(): Int? =
        this?.let {
            Regex("""(?i)\bpick up in (\d+)\s*min\b""")
                .find(it)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
        }

    private const val UBER_RIDE_NEARBY_MINUTES = 2
    private const val UBER_TAXI_PICKUP_EN_ROUTE_TITLE = "職業駕駛正在途中"
    private const val UBER_TAXI_PICKUP_APPROACHING_TITLE = "職業駕駛在幾分鐘後就會抵達"
    private const val UBER_TAXI_PICKUP_NEARBY_TITLE = "職業駕駛即將抵達"
    private const val UBER_TAXI_TRIP_ENDED_TITLE = "為您的行程評分"
    private const val UBER_TAXI_MEETING_TEXT = "請準備好與職業駕駛碰面"
    private val UBER_TAXI_TITLES = setOf(
        UBER_TAXI_PICKUP_EN_ROUTE_TITLE,
        UBER_TAXI_PICKUP_APPROACHING_TITLE,
        UBER_TAXI_PICKUP_NEARBY_TITLE,
        UBER_TAXI_TRIP_ENDED_TITLE,
    )
    private val UBER_TAXI_CONTENT_MARKERS = listOf(
        "分鐘內抵達",
        UBER_TAXI_MEETING_TEXT,
        "駕駛車款為",
    )
    private val UBER_TAXI_ETA = Regex("""將在\s*(\d+)\s*分鐘內抵達""")
    private val UBER_TAXI_VEHICLE = Regex(
        """駕駛車款為\s*(.+?)\s*[（(]\s*([\p{L}\p{N}-]+)\s*[）)]""",
    )
}
