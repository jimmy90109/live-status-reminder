package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.StatusBarNotification
import java.util.Locale

internal data class CitymapperNavigationText(val title: String, val contentText: String)

internal enum class CitymapperNavigationStage {
    UNKNOWN,
    WALKING,
    WAITING,
    RIDING,
    TRAIN_DEPARTURE,
}

internal enum class CitymapperTransitMode {
    UNKNOWN,
    BUS,
    TRAIN,
    METRO,
}

internal sealed interface CitymapperTransitTiming {
    data class CountdownMinutes(val minutes: Int) : CitymapperTransitTiming
    data class ScheduledTime(val time: String) : CitymapperTransitTiming
}

internal data class CitymapperNavigationPresentation(
    val stage: CitymapperNavigationStage = CitymapperNavigationStage.UNKNOWN,
    val transitMode: CitymapperTransitMode = CitymapperTransitMode.UNKNOWN,
    val walkingMinutes: Int? = null,
    val transitTiming: CitymapperTransitTiming? = null,
    val stops: Int? = null,
)

internal data class CitymapperNavigationMapping(
    val text: CitymapperNavigationText,
    val presentation: CitymapperNavigationPresentation,
)

internal object CitymapperNavigationMapper {
    const val PACKAGE_NAME = "com.citymapper.app.release"
    const val CHANNEL_ID = "trip-progress"
    private val inlineWhitespace = Regex("[^\\S\\r\\n]+")

    private val shareLabels = setOf("Share ETA", "分享預計抵達時間", "Share trip", "分享行程")
    private val chineseArrival = Regex(
        """^((?:上午|下午)?\s*(?:[01]?\d|2[0-3]):[0-5]\d)\s*(?:[（(]\s*\d+\s*分鐘\s*[）)]\s*)?到達$""",
    )
    private val englishArrival = Regex(
        """^Arrive\s+((?:0?[1-9]|1[0-2]):[0-5]\d\s+(?:AM|PM))\s*(?:\(\s*\d+\s*min\s*\))?$""",
        RegexOption.IGNORE_CASE,
    )
    private val chineseWalkingMinutes = Regex("""^\(距離\s*(\d+)\s*分鐘\)$""")
    private val englishWalkingMinutes = Regex("""^\(\s*(\d+)\s*min\s+away\s*\)$""", RegexOption.IGNORE_CASE)
    private val chineseWaiting = Regex("""^等候\s+(.+?)(?:\s+\(|$)""")
    private val englishWaiting = Regex("""^Wait\s+for\s+(.+?)(?:\s+\(|$)""", RegexOption.IGNORE_CASE)
    private val taipeiMetroLineCodes = setOf("BR", "R", "G", "O", "BL", "Y")
    private val chineseWaitingMinutes = Regex("""^(\d+)(?:\s*[,，]\s*\d+)*\s*分鐘$""")
    private val englishWaitingMinutes = Regex("""^(\d+)(?:\s*[,，]\s*\d+)*\s*min$""", RegexOption.IGNORE_CASE)
    private const val sourceClockTime =
        """(?:(?:上午|下午)\s*(?:0?[1-9]|1[0-2]):[0-5]\d|(?:[01]?\d|2[0-3]):[0-5]\d|(?:0?[1-9]|1[0-2]):[0-5]\d\s*(?:AM|PM))"""
    private val waitingScheduledTimes = Regex(
        """^($sourceClockTime)(?:\s*[,，]\s*$sourceClockTime)*$""",
        RegexOption.IGNORE_CASE,
    )
    private val chineseRiding = Regex("""^乘坐\s+(\d+)\s+站$""")
    private val englishRiding = Regex("""^Ride\s+(\d+)\s+stops?\s+to$""", RegexOption.IGNORE_CASE)
    private val trainDeparture = Regex("""^((?:[01]?\d|2[0-3]):[0-5]\d)\s+\d{3,4}\s+\S.+$""")

    fun isShareLabel(title: String?): Boolean = shareLabels.any {
        it.equals(normalizeLine(title.orEmpty()), ignoreCase = true)
    }

    fun isEligible(
        packageName: String?,
        channelId: String?,
        isOngoing: Boolean,
        isGroupSummary: Boolean,
    ): Boolean = packageName == PACKAGE_NAME && channelId == CHANNEL_ID &&
        isOngoing && !isGroupSummary

    fun map(
        notificationText: String?,
        actionTitles: List<String> = emptyList(),
        arrivalTimeFormat: String,
    ): CitymapperNavigationText? = mapping(notificationText, actionTitles, arrivalTimeFormat)?.text

    fun mapping(
        notificationText: String?,
        actionTitles: List<String> = emptyList(),
        arrivalTimeFormat: String,
    ): CitymapperNavigationMapping? = parse(notificationText, actionTitles, arrivalTimeFormat)

    fun presentation(
        notificationText: String?,
        actionTitles: List<String> = emptyList(),
    ): CitymapperNavigationPresentation? = normalizedLines(notificationText, actionTitles)
        .takeIf { it.isNotEmpty() }
        ?.let(::classify)

    private fun parse(
        notificationText: String?,
        actionTitles: List<String>,
        arrivalTimeFormat: String,
    ): CitymapperNavigationMapping? {
        val lines = normalizedLines(notificationText, actionTitles)
        val title = lines.firstOrNull() ?: return null
        val body = lines.drop(1).mapIndexed { index, line ->
            if (index == lines.size - 2) formatArrival(line, arrivalTimeFormat) else line
        }
        return CitymapperNavigationMapping(
            text = CitymapperNavigationText(title, body.joinToString("\n")),
            presentation = classify(lines),
        )
    }

    private fun normalizedLines(notificationText: String?, actionTitles: List<String>): List<String> {
        val actionLines = actionTitles.map(::normalizeLine).toSet()
        return notificationText.orEmpty().lineSequence()
            .map(::normalizeLine)
            .filter { it.isNotEmpty() && it !in actionLines && !isShareLabel(it) }
            .distinct()
            .toList()
    }

    private fun classify(lines: List<String>): CitymapperNavigationPresentation {
        val title = lines.first()
        val body = lines.drop(1)

        if (title.startsWith("步行至") || title.startsWith("Walk to", ignoreCase = true)) {
            val minutes = body.firstNotNullOfOrNull(::walkingMinutes)
            return CitymapperNavigationPresentation(
                CitymapperNavigationStage.WALKING,
                walkingMinutes = minutes,
            )
        }

        val waitingRoute = chineseWaiting.find(title)?.groupValues?.get(1)
            ?: englishWaiting.find(title)?.groupValues?.get(1)
        if (waitingRoute != null) {
            val mode = when {
                waitingRoute.uppercase(Locale.ROOT) in taipeiMetroLineCodes -> CitymapperTransitMode.METRO
                waitingRoute.any(Char::isDigit) -> CitymapperTransitMode.BUS
                else -> CitymapperTransitMode.UNKNOWN
            }
            return CitymapperNavigationPresentation(
                stage = CitymapperNavigationStage.WAITING,
                transitMode = mode,
                transitTiming = body.firstNotNullOfOrNull(::transitTiming),
            )
        }

        val stops = (chineseRiding.matchEntire(title) ?: englishRiding.matchEntire(title))
            ?.groupValues?.get(1)?.toIntOrNull()
        if (stops != null) {
            return CitymapperNavigationPresentation(CitymapperNavigationStage.RIDING, stops = stops)
        }

        val departureTime = trainDeparture.matchEntire(title)?.groupValues?.get(1)
        if (departureTime != null) {
            return CitymapperNavigationPresentation(
                stage = CitymapperNavigationStage.TRAIN_DEPARTURE,
                transitMode = CitymapperTransitMode.TRAIN,
                transitTiming = CitymapperTransitTiming.ScheduledTime(departureTime),
            )
        }

        return CitymapperNavigationPresentation()
    }

    private fun walkingMinutes(line: String): Int? =
        (chineseWalkingMinutes.matchEntire(line) ?: englishWalkingMinutes.matchEntire(line))
            ?.groupValues?.get(1)?.toIntOrNull()

    private fun transitTiming(line: String): CitymapperTransitTiming? {
        val minutes = (chineseWaitingMinutes.matchEntire(line) ?: englishWaitingMinutes.matchEntire(line))
            ?.groupValues?.get(1)?.toIntOrNull()
        if (minutes != null) return CitymapperTransitTiming.CountdownMinutes(minutes)
        val time = waitingScheduledTimes.matchEntire(line)?.groupValues?.get(1) ?: return null
        return CitymapperTransitTiming.ScheduledTime(time)
    }

    private fun formatArrival(line: String, format: String): String {
        val time = (chineseArrival.matchEntire(line) ?: englishArrival.matchEntire(line))
            ?.groupValues?.get(1) ?: return line
        return String.format(Locale.ROOT, format, time)
    }

    private fun normalizeLine(line: String): String = line.replace(inlineWhitespace, " ").trim()
}

internal data class CitymapperNavigationUpdate(
    val sourceKey: String,
    val postTime: Long,
    val text: CitymapperNavigationText,
    val presentation: CitymapperNavigationPresentation = CitymapperNavigationPresentation(),
    val visibility: Int = Notification.VISIBILITY_PRIVATE,
    val contentIntent: PendingIntent? = null,
    val sourceActions: List<Notification.Action> = emptyList(),
)

internal object CitymapperNotificationExtractor {
    fun extract(
        context: Context,
        source: StatusBarNotification,
        notificationText: String? = null,
    ): CitymapperNavigationUpdate? {
        val notification = source.notification
        if (!CitymapperNavigationMapper.isEligible(
                source.packageName,
                notification.channelId,
                source.isOngoing,
                notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            )
        ) return null
        val actions = notification.actions.orEmpty().toList()
        val sourceText = notificationText ?: LiveStatusNotificationListenerService.readCitymapperNotificationText(
            context, source.packageName, notification,
        )
        val mapping = CitymapperNavigationMapper.mapping(
            sourceText,
            actions.map { it.title?.toString().orEmpty() },
            context.getString(R.string.citymapper_arrival_time_format),
        ) ?: return null
        return CitymapperNavigationUpdate(
            sourceKey = source.key,
            postTime = source.postTime,
            text = mapping.text,
            presentation = mapping.presentation,
            visibility = notification.visibility,
            contentIntent = notification.contentIntent,
            sourceActions = actions.filter(::isCitymapperNavigationAction),
        )
    }
}

internal fun isCitymapperNavigationAction(action: Notification.Action): Boolean =
    action.actionIntent != null &&
        !CitymapperNavigationMapper.isShareLabel(action.title?.toString())

internal sealed interface CitymapperNavigationDecision {
    data class Show(val update: CitymapperNavigationUpdate) : CitymapperNavigationDecision
    data object Clear : CitymapperNavigationDecision
    data object None : CitymapperNavigationDecision
}

internal class CitymapperNavigationTracker {
    private var activeSourceKey: String? = null
    private var activeTransitMode = CitymapperTransitMode.UNKNOWN

    fun onPosted(sourceKey: String, update: CitymapperNavigationUpdate?): CitymapperNavigationDecision {
        if (update != null) {
            if (sourceKey != activeSourceKey) activeTransitMode = CitymapperTransitMode.UNKNOWN
            activeSourceKey = sourceKey
            val presentation = resolvePresentation(update.presentation)
            return CitymapperNavigationDecision.Show(update.copy(presentation = presentation))
        }
        return onRemoved(sourceKey)
    }

    fun onRemoved(sourceKey: String): CitymapperNavigationDecision {
        if (sourceKey != activeSourceKey) return CitymapperNavigationDecision.None
        return reset()
    }

    fun restore(updates: List<CitymapperNavigationUpdate>): CitymapperNavigationDecision {
        val latest = updates.maxByOrNull { it.postTime } ?: return reset()
        activeSourceKey = latest.sourceKey
        activeTransitMode = CitymapperTransitMode.UNKNOWN
        val presentation = resolvePresentation(latest.presentation)
        return CitymapperNavigationDecision.Show(latest.copy(presentation = presentation))
    }

    fun reset(): CitymapperNavigationDecision {
        activeSourceKey = null
        activeTransitMode = CitymapperTransitMode.UNKNOWN
        return CitymapperNavigationDecision.Clear
    }

    private fun resolvePresentation(
        presentation: CitymapperNavigationPresentation,
    ): CitymapperNavigationPresentation {
        if (
            presentation.stage == CitymapperNavigationStage.WALKING ||
            presentation.stage == CitymapperNavigationStage.WAITING &&
            presentation.transitMode == CitymapperTransitMode.UNKNOWN
        ) {
            activeTransitMode = CitymapperTransitMode.UNKNOWN
            return presentation
        }
        if (presentation.transitMode != CitymapperTransitMode.UNKNOWN) {
            activeTransitMode = presentation.transitMode
            return presentation
        }
        return if (
            presentation.stage == CitymapperNavigationStage.RIDING &&
            activeTransitMode != CitymapperTransitMode.UNKNOWN
        ) {
            presentation.copy(transitMode = activeTransitMode)
        } else {
            presentation
        }
    }
}
