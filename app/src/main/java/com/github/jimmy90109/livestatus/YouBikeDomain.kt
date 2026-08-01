package com.github.jimmy90109.livestatus

import java.text.Normalizer
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

enum class YouBikeEvent {
    NONE,
    BORROWED,
    RETURNED,
}

enum class YouBikeRegion {
    TAIPEI,
    NEW_TAIPEI,
    TAOYUAN,
    HSINCHU_COUNTY,
    HSINCHU_CITY,
    HSINCHU_SCIENCE_PARK,
    MIAOLI,
    TAICHUNG,
    CHIAYI_CITY,
    CHIAYI_COUNTY,
    TAINAN,
    KAOHSIUNG,
    PINGTUNG,
    TAITUNG,
    UNSUPPORTED,
    UNRESOLVED,
}

internal val YouBikeRegion.isFareSupported: Boolean
    get() = fareRule(YouBikeVehicleType.STANDARD_2_0) != null

enum class YouBikeVehicleType(val displayName: String) {
    STANDARD_2_0("YouBike 2.0"),
    ELECTRIC_2_0E("YouBike 2.0E"),
}

enum class YouBikeStationResolutionIssue {
    UNKNOWN,
    AMBIGUOUS,
}

object YouBikeVehicleClassifier {
    fun classify(bikeNumber: String?): YouBikeVehicleType {
        val normalized = bikeNumber?.trim().orEmpty()
        return if (
            normalized.length == 7 &&
            normalized.all(Char::isDigit) &&
            normalized[2] in setOf('6', '9')
        ) {
            YouBikeVehicleType.ELECTRIC_2_0E
        } else {
            YouBikeVehicleType.STANDARD_2_0
        }
    }
}

data class YouBikeRideUpdate(
    val event: YouBikeEvent,
    val occurredAt: LocalDateTime? = null,
    val stationName: String? = null,
    val dockNumber: String? = null,
    val bikeNumber: String? = null,
    val chargedAmount: Int? = null,
)

data class YouBikeRideSession(
    val id: String,
    val borrowedAtMillis: Long,
    val stationName: String,
    val dockNumber: String?,
    val bikeNumber: String,
    val region: YouBikeRegion,
    val candidateRegions: Set<YouBikeRegion> = emptySet(),
    val manuallySelectedRegion: YouBikeRegion? = null,
    val originalResolutionIssue: YouBikeStationResolutionIssue? = null,
) {
    val vehicleType: YouBikeVehicleType
        get() = YouBikeVehicleClassifier.classify(bikeNumber)
}

object YouBikeNotificationParser {
    private val dateTimePattern = Regex("""(\d{4})-(\d{2})-(\d{2})\s+(\d{2}):(\d{2}):(\d{2})""")
    private val borrowPattern = Regex(
        """借車成功\s*[！!]?\s*您於\s*(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})\s*在\s*(.+?)\s+(\d+)\s*車柱\s*[,，].*?租借車號\s*([A-Za-z0-9-]+)""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )
    private val returnPattern = Regex(
        """還車扣款成功\s*[！!]?\s*您於\s*(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2}:\d{2})\s*在\s*(.+?)\s+(\d+)\s*車柱\s*[,，].*?歸還車號\s*([A-Za-z0-9-]+).*?並已扣款\s*(\d+)\s*元""",
        setOf(RegexOption.DOT_MATCHES_ALL),
    )

    fun parse(primaryText: String?, fallbackText: String? = null): YouBikeRideUpdate {
        val candidates = sequenceOf(primaryText, fallbackText)
            .filterNotNull()
            .map(::normalizeNotificationText)
            .filter(String::isNotBlank)
            .distinct()
        candidates.forEach { text ->
            returnPattern.find(text)?.let { match ->
                return match.toUpdate(YouBikeEvent.RETURNED, match.groupValues[5].toIntOrNull())
            }
        }
        sequenceOf(primaryText, fallbackText)
            .filterNotNull()
            .map(::normalizeNotificationText)
            .filter(String::isNotBlank)
            .distinct()
            .forEach { text ->
                borrowPattern.find(text)?.let { match ->
                    return match.toUpdate(YouBikeEvent.BORROWED)
                }
            }
        return YouBikeRideUpdate(YouBikeEvent.NONE)
    }

    private fun MatchResult.toUpdate(event: YouBikeEvent, amount: Int? = null): YouBikeRideUpdate {
        val occurredAt = parseDateTime(groupValues[1])
            ?: return YouBikeRideUpdate(YouBikeEvent.NONE)
        val stationName = groupValues[2].trim().takeIf(String::isNotEmpty)
            ?: return YouBikeRideUpdate(YouBikeEvent.NONE)
        val bikeNumber = groupValues[4].trim().takeIf(String::isNotEmpty)
            ?: return YouBikeRideUpdate(YouBikeEvent.NONE)
        return YouBikeRideUpdate(
            event = event,
            occurredAt = occurredAt,
            stationName = stationName,
            dockNumber = groupValues[3].trim().takeIf(String::isNotEmpty),
            bikeNumber = bikeNumber,
            chargedAmount = amount,
        )
    }

    private fun parseDateTime(value: String): LocalDateTime? {
        val parts = dateTimePattern.matchEntire(value.trim())?.groupValues ?: return null
        return runCatching {
            LocalDateTime.of(
                parts[1].toInt(), parts[2].toInt(), parts[3].toInt(),
                parts[4].toInt(), parts[5].toInt(), parts[6].toInt(),
            )
        }.getOrNull()
    }

    private fun normalizeNotificationText(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replace(Regex("""[\r\n]+"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
}

data class YouBikeFareEstimate(
    val amount: Int,
    val nextBoundaryMillis: Long,
    val nextAmount: Int,
)

private const val YOU_BIKE_HALF_HOUR_MILLIS = 30 * 60_000L
private const val YOU_BIKE_TWO_HOURS_MILLIS = 2 * 60 * 60_000L
private const val YOU_BIKE_FOUR_HOURS_MILLIS = 4 * 60 * 60_000L
private const val YOU_BIKE_EIGHT_HOURS_MILLIS = 8 * 60 * 60_000L

object YouBikeFarePolicy {
    fun estimate(
        borrowedAtMillis: Long,
        nowMillis: Long,
        region: YouBikeRegion,
        vehicleType: YouBikeVehicleType = YouBikeVehicleType.STANDARD_2_0,
    ): YouBikeFareEstimate? {
        val rule = region.fareRule(vehicleType, borrowedAtMillis) ?: return null
        val elapsed = (nowMillis - borrowedAtMillis).coerceAtLeast(0L)
        val amount = amountForElapsed(elapsed, rule)
        var nextInterval = elapsed / YOU_BIKE_HALF_HOUR_MILLIS + 1
        var nextAmount: Int
        do {
            nextAmount = amountForElapsed(nextInterval * YOU_BIKE_HALF_HOUR_MILLIS + 1L, rule)
            if (nextAmount == amount) nextInterval++
        } while (nextAmount == amount)
        return YouBikeFareEstimate(
            amount = amount,
            nextBoundaryMillis = borrowedAtMillis + nextInterval * YOU_BIKE_HALF_HOUR_MILLIS,
            nextAmount = nextAmount,
        )
    }

    private fun amountForElapsed(elapsedMillis: Long, rule: YouBikeFareRule): Int {
        val intervalCount =
            ((elapsedMillis + YOU_BIKE_HALF_HOUR_MILLIS - 1) / YOU_BIKE_HALF_HOUR_MILLIS)
                .toInt().coerceAtLeast(1)
        var amount = 0
        repeat(intervalCount) { index ->
            val intervalStart = index * YOU_BIKE_HALF_HOUR_MILLIS
            amount += rule.tiers.first { tier ->
                tier.untilMillisExclusive == null || intervalStart < tier.untilMillisExclusive
            }.pricePerHalfHour
        }
        return (amount - rule.initialSubsidyAmount).coerceAtLeast(0)
    }
}

internal data class YouBikeFareRule(
    val tiers: List<YouBikeFareTier>,
    val initialSubsidyAmount: Int,
    val subsidyUntilExclusiveMillis: Long? = null,
)

internal data class YouBikeFareTier(
    val untilMillisExclusive: Long?,
    val pricePerHalfHour: Int,
)

private val standardFareWithoutSubsidy = YouBikeFareRule(
    tiers = listOf(
        YouBikeFareTier(YOU_BIKE_FOUR_HOURS_MILLIS, 10),
        YouBikeFareTier(YOU_BIKE_EIGHT_HOURS_MILLIS, 20),
        YouBikeFareTier(null, 40),
    ),
    initialSubsidyAmount = 0,
)

private val electricFareWithoutSubsidy = YouBikeFareRule(
    tiers = listOf(
        YouBikeFareTier(YOU_BIKE_TWO_HOURS_MILLIS, 20),
        YouBikeFareTier(null, 40),
    ),
    initialSubsidyAmount = 0,
)

private val taitungStandardFareWithoutSubsidy = YouBikeFareRule(
    tiers = listOf(
        YouBikeFareTier(YOU_BIKE_FOUR_HOURS_MILLIS, 12),
        YouBikeFareTier(YOU_BIKE_EIGHT_HOURS_MILLIS, 24),
        YouBikeFareTier(null, 48),
    ),
    initialSubsidyAmount = 0,
)

private val taitungElectricFareWithoutSubsidy = YouBikeFareRule(
    tiers = listOf(
        YouBikeFareTier(YOU_BIKE_TWO_HOURS_MILLIS, 25),
        YouBikeFareTier(null, 50),
    ),
    initialSubsidyAmount = 0,
)

private val chiayiSubsidyUntilExclusiveMillis = LocalDate.of(2027, 1, 1)
    .atStartOfDay(ZoneId.of("Asia/Taipei"))
    .toInstant()
    .toEpochMilli()

private fun YouBikeRegion.fareRule(
    vehicleType: YouBikeVehicleType,
    borrowedAtMillis: Long = 0L,
): YouBikeFareRule? {
    if (this == YouBikeRegion.UNSUPPORTED || this == YouBikeRegion.UNRESOLVED) return null
    val rule = when (vehicleType) {
        YouBikeVehicleType.STANDARD_2_0 -> when (this) {
            YouBikeRegion.TAIPEI,
            YouBikeRegion.NEW_TAIPEI,
            YouBikeRegion.HSINCHU_CITY,
            YouBikeRegion.MIAOLI,
            YouBikeRegion.TAICHUNG,
            -> standardFareWithoutSubsidy.copy(initialSubsidyAmount = 10)
            YouBikeRegion.TAOYUAN -> standardFareWithoutSubsidy.copy(initialSubsidyAmount = 20)
            YouBikeRegion.HSINCHU_COUNTY,
            YouBikeRegion.HSINCHU_SCIENCE_PARK,
            YouBikeRegion.TAINAN,
            -> standardFareWithoutSubsidy
            YouBikeRegion.CHIAYI_CITY,
            YouBikeRegion.CHIAYI_COUNTY,
            -> standardFareWithoutSubsidy.copy(
                initialSubsidyAmount = 10,
                subsidyUntilExclusiveMillis = chiayiSubsidyUntilExclusiveMillis,
            )
            YouBikeRegion.KAOHSIUNG -> standardFareWithoutSubsidy.copy(initialSubsidyAmount = 5)
            YouBikeRegion.PINGTUNG -> standardFareWithoutSubsidy.copy(initialSubsidyAmount = 10)
            YouBikeRegion.TAITUNG -> taitungStandardFareWithoutSubsidy.copy(initialSubsidyAmount = 12)
            YouBikeRegion.UNSUPPORTED,
            YouBikeRegion.UNRESOLVED,
            -> null
        }
        YouBikeVehicleType.ELECTRIC_2_0E -> when (this) {
            YouBikeRegion.HSINCHU_CITY,
            YouBikeRegion.MIAOLI,
            YouBikeRegion.TAICHUNG,
            -> electricFareWithoutSubsidy.copy(initialSubsidyAmount = 10)
            YouBikeRegion.TAIPEI,
            YouBikeRegion.NEW_TAIPEI,
            YouBikeRegion.TAOYUAN,
            YouBikeRegion.HSINCHU_COUNTY,
            YouBikeRegion.HSINCHU_SCIENCE_PARK,
            YouBikeRegion.TAINAN,
            YouBikeRegion.PINGTUNG,
            -> electricFareWithoutSubsidy
            YouBikeRegion.CHIAYI_CITY,
            YouBikeRegion.CHIAYI_COUNTY,
            -> electricFareWithoutSubsidy.copy(
                initialSubsidyAmount = 10,
                subsidyUntilExclusiveMillis = chiayiSubsidyUntilExclusiveMillis,
            )
            YouBikeRegion.KAOHSIUNG -> electricFareWithoutSubsidy.copy(initialSubsidyAmount = 10)
            YouBikeRegion.TAITUNG -> taitungElectricFareWithoutSubsidy.copy(initialSubsidyAmount = 12)
            YouBikeRegion.UNSUPPORTED,
            YouBikeRegion.UNRESOLVED,
            -> null
        }
    }
    return rule?.let {
        if (it.subsidyUntilExclusiveMillis?.let { until -> borrowedAtMillis >= until } == true) {
            it.copy(initialSubsidyAmount = 0)
        } else {
            it
        }
    }
}

sealed interface YouBikeStationResolution {
    data class Supported(
        val region: YouBikeRegion,
        val candidates: Set<YouBikeRegion> = setOf(region),
    ) : YouBikeStationResolution

    data class Ambiguous(val candidates: Set<YouBikeRegion>) : YouBikeStationResolution
    data object Unsupported : YouBikeStationResolution
    data object Unknown : YouBikeStationResolution
}

object YouBikeStationResolver {
    fun normalizeStationName(value: String): String =
        Normalizer.normalize(value, Normalizer.Form.NFKC)
            .trim()
            .replace(Regex("""\s+"""), " ")
            .removePrefix("YouBike2.0_")
            .trim()

    fun resolve(stationName: String, rows: Sequence<String>): YouBikeStationResolution {
        val target = normalizeStationName(stationName)
        val regions = rows.mapNotNull { row ->
            val columns = row.split('\t')
            if (columns.size < 2 || columns[0].startsWith("#")) return@mapNotNull null
            if (normalizeStationName(columns[0]) != target) return@mapNotNull null
            columns[1].toRegion()
        }.toSet()
        if (regions.isEmpty()) return YouBikeStationResolution.Unknown
        val supported = regions.filterTo(linkedSetOf()) { it.isFareSupported }
        return when {
            supported.size == 1 && regions.size == 1 ->
                YouBikeStationResolution.Supported(supported.first())
            supported.isNotEmpty() -> YouBikeStationResolution.Ambiguous(supported)
            else -> YouBikeStationResolution.Unsupported
        }
    }

    private fun String.toRegion(): YouBikeRegion? = when (this) {
        "Taipei" -> YouBikeRegion.TAIPEI
        "NewTaipei" -> YouBikeRegion.NEW_TAIPEI
        "Taoyuan" -> YouBikeRegion.TAOYUAN
        "HsinchuCounty" -> YouBikeRegion.HSINCHU_COUNTY
        "HsinchuCity" -> YouBikeRegion.HSINCHU_CITY
        "HsinchuSciencePark" -> YouBikeRegion.HSINCHU_SCIENCE_PARK
        "Miaoli" -> YouBikeRegion.MIAOLI
        "Taichung" -> YouBikeRegion.TAICHUNG
        "ChiayiCity" -> YouBikeRegion.CHIAYI_CITY
        "ChiayiCounty" -> YouBikeRegion.CHIAYI_COUNTY
        "Tainan" -> YouBikeRegion.TAINAN
        "Kaohsiung" -> YouBikeRegion.KAOHSIUNG
        "Pingtung" -> YouBikeRegion.PINGTUNG
        "Taitung" -> YouBikeRegion.TAITUNG
        "Unsupported" -> YouBikeRegion.UNSUPPORTED
        else -> null
    }
}

internal fun YouBikeRideSession.isExpired(nowMillis: Long): Boolean =
    Duration.ofMillis((nowMillis - borrowedAtMillis).coerceAtLeast(0)).toHours() >= 24
