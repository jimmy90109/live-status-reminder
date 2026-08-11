package com.github.jimmy90109.livestatus

internal object UberEatsNotificationSourcePolicy {
    const val DECORATED_CUSTOM_VIEW_TEMPLATE =
        "android.app.Notification\$DecoratedCustomViewStyle"

    fun supports(template: String?, isGroupSummary: Boolean): Boolean =
        !isGroupSummary && template == DECORATED_CUSTOM_VIEW_TEMPLATE
}

internal data class UberEatsDisplayUpdate(
    val event: LiveStatusNotificationParser.UberEatsEvent,
    val language: LiveStatusNotificationParser.UberEatsLanguage,
    val pin: String?,
    val officialTitle: String?,
    val officialText: String,
)

internal sealed interface UberEatsDecision {
    data class Show(val update: UberEatsDisplayUpdate) : UberEatsDecision

    data object Clear : UberEatsDecision

    data object None : UberEatsDecision
}

internal class UberEatsTracker {
    private var activeSourceKey: String? = null
    private var current: UberEatsDisplayUpdate? = null

    fun onPosted(
        sourceKey: String,
        update: LiveStatusNotificationParser.UberEatsUpdate,
        officialTitle: String?,
        officialText: String,
    ): UberEatsDecision {
        if (update.event == LiveStatusNotificationParser.UberEatsEvent.ORDER_ENDED) {
            reset()
            return UberEatsDecision.Clear
        }
        if (update.event == LiveStatusNotificationParser.UberEatsEvent.NONE) {
            return UberEatsDecision.None
        }

        val previous = current
        if (
            update.event != LiveStatusNotificationParser.UberEatsEvent.ORDER_RECEIVED &&
            previous != null &&
            eventRank(update.event) < eventRank(previous.event)
        ) {
            return UberEatsDecision.None
        }

        val next = if (update.event == LiveStatusNotificationParser.UberEatsEvent.ORDER_RECEIVED) {
            UberEatsDisplayUpdate(
                event = update.event,
                language = update.language,
                pin = update.pin,
                officialTitle = officialTitle,
                officialText = officialText,
            )
        } else {
            UberEatsDisplayUpdate(
                event = update.event,
                language = update.language,
                pin = update.pin ?: previous?.pin,
                officialTitle = officialTitle ?: previous?.officialTitle,
                officialText = officialText.takeIf(String::isNotBlank)
                    ?: previous?.officialText.orEmpty(),
            )
        }
        activeSourceKey = sourceKey
        current = next
        return UberEatsDecision.Show(next)
    }

    fun onRemoved(sourceKey: String): UberEatsDecision {
        if (sourceKey != activeSourceKey) return UberEatsDecision.None
        reset()
        return UberEatsDecision.Clear
    }

    fun reset() {
        activeSourceKey = null
        current = null
    }

    private fun eventRank(event: LiveStatusNotificationParser.UberEatsEvent): Int = when (event) {
        LiveStatusNotificationParser.UberEatsEvent.ORDER_RECEIVED -> 1
        LiveStatusNotificationParser.UberEatsEvent.PREPARING -> 2
        LiveStatusNotificationParser.UberEatsEvent.PICKING_UP -> 3
        LiveStatusNotificationParser.UberEatsEvent.ON_THE_WAY -> 4
        LiveStatusNotificationParser.UberEatsEvent.ARRIVING -> 5
        else -> 0
    }
}
