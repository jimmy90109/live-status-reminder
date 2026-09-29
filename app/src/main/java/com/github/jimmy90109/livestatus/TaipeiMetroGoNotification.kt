package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.service.notification.StatusBarNotification

internal data class TaipeiMetroGoRoute(
    val origin: String,
    val destination: String,
    val currentStation: String,
)

internal object TaipeiMetroGoNotificationParser {
    const val PACKAGE_NAME = "tw.com.trtc.is.android05"
    const val CHANNEL_ID = "MetroBeaconChannel"
    const val RUNNING_TITLE = "台北捷運 Go - 運行中"

    private val inlineWhitespace = Regex("\\s+")
    private val currentStationPattern = Regex("^(?:🔔\\s*)?行經[：:]\\s*(.+)$")
    private val routePattern = Regex("^(.+?)\\s+[-–—]\\s+(.+)$")

    fun isEligible(
        packageName: String?,
        channelId: String?,
        isOngoing: Boolean,
        isForegroundService: Boolean,
        isGroupSummary: Boolean,
    ): Boolean = packageName == PACKAGE_NAME &&
        channelId == CHANNEL_ID &&
        isOngoing &&
        isForegroundService &&
        !isGroupSummary

    fun parse(
        notificationTitle: String?,
        notificationContentText: String?,
        notificationText: String?,
    ): TaipeiMetroGoRoute? {
        if (normalizeLine(notificationTitle.orEmpty()) != RUNNING_TITLE) return null
        val lines = notificationText.orEmpty().lineSequence()
            .map(::normalizeLine)
            .filter { it.isNotEmpty() && it != RUNNING_TITLE }
            .toList()
        val currentStation = sequenceOf(normalizeLine(notificationContentText.orEmpty()))
            .plus(lines.asSequence())
            .mapNotNull(::currentStation)
            .firstOrNull()
            ?: return null
        val route = lines.firstNotNullOfOrNull(::route) ?: return null
        return TaipeiMetroGoRoute(
            origin = route.first,
            destination = route.second,
            currentStation = currentStation,
        )
    }

    fun isEndAction(action: Notification.Action): Boolean =
        action.actionIntent != null && normalizeLine(action.title?.toString().orEmpty()) == "結束"

    private fun currentStation(line: String): String? = currentStationPattern.matchEntire(line)
        ?.groupValues?.get(1)
        ?.let(::normalizeLine)
        ?.takeIf(String::isNotEmpty)

    private fun route(line: String): Pair<String, String>? {
        val match = routePattern.matchEntire(line) ?: return null
        val origin = normalizeLine(match.groupValues[1])
        val destination = normalizeLine(match.groupValues[2])
        return if (origin.isNotEmpty() && destination.isNotEmpty()) {
            origin to destination
        } else {
            null
        }
    }

    private fun normalizeLine(value: String): String =
        value.replace(inlineWhitespace, " ").trim()
}

internal data class TaipeiMetroGoUpdate(
    val sourceKey: String,
    val postTime: Long,
    val origin: String,
    val destination: String,
    val currentStation: String,
    val visibility: Int = Notification.VISIBILITY_PRIVATE,
    val contentIntent: PendingIntent? = null,
    val sourceActions: List<Notification.Action> = emptyList(),
)

internal object TaipeiMetroGoNotificationExtractor {
    fun extract(
        context: Context,
        source: StatusBarNotification,
        notificationText: String? = null,
    ): TaipeiMetroGoUpdate? {
        val notification = source.notification
        if (!TaipeiMetroGoNotificationParser.isEligible(
                packageName = source.packageName,
                channelId = notification.channelId,
                isOngoing = source.isOngoing,
                isForegroundService =
                    notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
                isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            )
        ) return null
        val route = TaipeiMetroGoNotificationParser.parse(
            notificationTitle =
                LiveStatusNotificationListenerService.readNotificationTitle(notification),
            notificationContentText =
                LiveStatusNotificationListenerService.readNotificationContentText(notification),
            notificationText = notificationText
                ?: LiveStatusNotificationListenerService.readNotificationText(
                    context,
                    source.packageName,
                    notification,
                ),
        ) ?: return null
        return TaipeiMetroGoUpdate(
            sourceKey = source.key,
            postTime = source.postTime,
            origin = route.origin,
            destination = route.destination,
            currentStation = route.currentStation,
            visibility = notification.visibility,
            contentIntent = notification.contentIntent,
            sourceActions = notification.actions.orEmpty()
                .filter(TaipeiMetroGoNotificationParser::isEndAction),
        )
    }
}

internal sealed interface TaipeiMetroGoDecision {
    data class Show(val update: TaipeiMetroGoUpdate) : TaipeiMetroGoDecision
    data object Clear : TaipeiMetroGoDecision
    data object None : TaipeiMetroGoDecision
}

internal class TaipeiMetroGoTracker {
    private var activeSourceKey: String? = null

    fun onPosted(sourceKey: String, update: TaipeiMetroGoUpdate?): TaipeiMetroGoDecision {
        if (update != null) {
            activeSourceKey = sourceKey
            return TaipeiMetroGoDecision.Show(update)
        }
        return onRemoved(sourceKey)
    }

    fun onRemoved(sourceKey: String): TaipeiMetroGoDecision {
        if (sourceKey != activeSourceKey) return TaipeiMetroGoDecision.None
        return reset()
    }

    fun restore(updates: List<TaipeiMetroGoUpdate>): TaipeiMetroGoDecision {
        val latest = updates.maxByOrNull(TaipeiMetroGoUpdate::postTime) ?: return reset()
        activeSourceKey = latest.sourceKey
        return TaipeiMetroGoDecision.Show(latest)
    }

    fun reset(): TaipeiMetroGoDecision {
        activeSourceKey = null
        return TaipeiMetroGoDecision.Clear
    }
}
