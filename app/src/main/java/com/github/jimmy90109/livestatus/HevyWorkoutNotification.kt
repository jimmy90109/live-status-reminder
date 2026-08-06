package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent

internal enum class HevyWorkoutPhase {
    ACTIVE_SET,
    REST,
}

internal data class HevyWorkoutUpdate(
    val sourceKey: String,
    val startedAtEpochMillis: Long,
    val exerciseName: String,
    val phase: HevyWorkoutPhase,
    val setNumber: Int,
    val totalSets: Int,
    val setDetail: String,
    val sourceContentText: String,
    val restRemainingSeconds: Int? = null,
    val contentIntent: PendingIntent? = null,
    val sourceActions: List<Notification.Action> = emptyList(),
) {
    init {
        require(setNumber in 1..totalSets) { "Set number must be within the exercise set count." }
        require(
            (phase == HevyWorkoutPhase.ACTIVE_SET && restRemainingSeconds == null) ||
                (phase == HevyWorkoutPhase.REST && restRemainingSeconds != null &&
                    restRemainingSeconds >= 0),
        ) { "Only rest updates may contain a non-negative rest duration." }
    }

    val progressPercent: Int
        get() = ((setNumber.toDouble() / totalSets) * 100).toInt().coerceIn(0, 100)

    val hasActiveRestCountdown: Boolean
        get() = phase == HevyWorkoutPhase.REST && requireNotNull(restRemainingSeconds) > 0
}

internal object HevyWorkoutNotificationParser {
    const val PACKAGE_NAME = "com.hevy"
    const val WORKOUT_CHANNEL_ID = "FOREGROUND_SERVICE_NOTIFICATION_CHANNEL_ID"
    const val WORKOUT_CATEGORY = "workout"

    private val activeSetPattern = Regex(
        """^第\s*(\d+)\s*/\s*(\d+)\s*組\s*[-–—]\s*(.+?)\s*$""",
    )
    private val nextSetPattern = Regex(
        """^下一個\s*[:：]\s*第\s*(\d+)\s*組\s*[（(]\s*共\s*(\d+)\s*組\s*[）)]\s*[（(]\s*(.+?)\s*[）)]\s*$""",
    )
    private val restPattern = Regex("""^休息\s*(\d+):(\d{2})\s*$""")

    fun parse(
        sourceKey: String,
        channelId: String?,
        category: String?,
        startedAtEpochMillis: Long,
        notificationText: String?,
        nowEpochMillis: Long,
        contentIntent: PendingIntent? = null,
        sourceActions: List<Notification.Action> = emptyList(),
    ): HevyWorkoutUpdate? {
        if (channelId != WORKOUT_CHANNEL_ID || category != WORKOUT_CATEGORY) return null
        if (startedAtEpochMillis !in 1..nowEpochMillis) return null

        val lines = notificationText
            ?.lineSequence()
            ?.map(String::trim)
            ?.filter(String::isNotEmpty)
            ?.toList()
            .orEmpty()
        if (lines.size < 2) return null

        val exerciseName = lines.first()
        val activeSet = activeSetPattern.matchEntire(lines[1])
        if (activeSet != null) {
            return createUpdate(
                sourceKey = sourceKey,
                startedAtEpochMillis = startedAtEpochMillis,
                exerciseName = exerciseName,
                phase = HevyWorkoutPhase.ACTIVE_SET,
                setNumberText = activeSet.groupValues[1],
                totalSetsText = activeSet.groupValues[2],
                setDetail = activeSet.groupValues[3],
                sourceContentText = lines[1],
                contentIntent = contentIntent,
                sourceActions = sourceActions,
            )
        }

        val nextSet = nextSetPattern.matchEntire(lines[1]) ?: return null
        val rest = lines.drop(2).firstNotNullOfOrNull(restPattern::matchEntire) ?: return null
        val restMinutes = rest.groupValues[1].toIntOrNull() ?: return null
        val restSeconds = rest.groupValues[2].toIntOrNull()?.takeIf { it in 0..59 } ?: return null
        val restRemainingSeconds = restMinutes.toLong() * 60 + restSeconds
        if (restRemainingSeconds > Int.MAX_VALUE) return null
        return createUpdate(
            sourceKey = sourceKey,
            startedAtEpochMillis = startedAtEpochMillis,
            exerciseName = exerciseName,
            phase = HevyWorkoutPhase.REST,
            setNumberText = nextSet.groupValues[1],
            totalSetsText = nextSet.groupValues[2],
            setDetail = nextSet.groupValues[3],
            sourceContentText = lines.drop(1).joinToString("\n"),
            restRemainingSeconds = restRemainingSeconds.toInt(),
            contentIntent = contentIntent,
            sourceActions = sourceActions,
        )
    }

    private fun createUpdate(
        sourceKey: String,
        startedAtEpochMillis: Long,
        exerciseName: String,
        phase: HevyWorkoutPhase,
        setNumberText: String,
        totalSetsText: String,
        setDetail: String,
        sourceContentText: String,
        restRemainingSeconds: Int? = null,
        contentIntent: PendingIntent?,
        sourceActions: List<Notification.Action>,
    ): HevyWorkoutUpdate? {
        val setNumber = setNumberText.toIntOrNull() ?: return null
        val totalSets = totalSetsText.toIntOrNull() ?: return null
        if (setNumber !in 1..totalSets || totalSets <= 0) return null
        val normalizedDetail = setDetail
            .trim()
            .replace(Regex("""\s+[xX×]\s+"""), " × ")
            .takeIf(String::isNotEmpty)
            ?: return null
        return HevyWorkoutUpdate(
            sourceKey = sourceKey,
            startedAtEpochMillis = startedAtEpochMillis,
            exerciseName = exerciseName,
            phase = phase,
            setNumber = setNumber,
            totalSets = totalSets,
            setDetail = normalizedDetail,
            sourceContentText = sourceContentText,
            restRemainingSeconds = restRemainingSeconds,
            contentIntent = contentIntent,
            sourceActions = sourceActions,
        )
    }
}

internal sealed interface HevyWorkoutDecision {
    data class Show(val update: HevyWorkoutUpdate) : HevyWorkoutDecision
    data object Clear : HevyWorkoutDecision
    data object None : HevyWorkoutDecision
}

internal class HevyWorkoutTracker {
    private var activeSourceKey: String? = null

    fun onPosted(sourceKey: String, update: HevyWorkoutUpdate?): HevyWorkoutDecision {
        if (update != null) {
            activeSourceKey = sourceKey
            return HevyWorkoutDecision.Show(update)
        }
        if (activeSourceKey == sourceKey) {
            activeSourceKey = null
            return HevyWorkoutDecision.Clear
        }
        return HevyWorkoutDecision.None
    }

    fun onRemoved(sourceKey: String): HevyWorkoutDecision {
        if (activeSourceKey != sourceKey) return HevyWorkoutDecision.None
        activeSourceKey = null
        return HevyWorkoutDecision.Clear
    }

    fun restore(updates: List<HevyWorkoutUpdate>): HevyWorkoutDecision {
        val update = updates.maxByOrNull(HevyWorkoutUpdate::startedAtEpochMillis)
        activeSourceKey = update?.sourceKey
        return if (update == null) HevyWorkoutDecision.Clear else HevyWorkoutDecision.Show(update)
    }

    fun reset() {
        activeSourceKey = null
    }
}
