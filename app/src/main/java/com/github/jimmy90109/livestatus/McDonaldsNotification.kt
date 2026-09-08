package com.github.jimmy90109.livestatus

internal sealed interface McDonaldsDecision {
    data class Show(
        val update: LiveStatusNotificationParser.McDonaldsUpdate,
    ) : McDonaldsDecision

    data object Clear : McDonaldsDecision

    data object None : McDonaldsDecision
}

internal class McDonaldsTracker {
    private var activeSourceKey: String? = null

    fun onPosted(
        sourceKey: String,
        update: LiveStatusNotificationParser.McDonaldsUpdate,
    ): McDonaldsDecision {
        if (
            update.event != LiveStatusNotificationParser.McDonaldsEvent.READY_FOR_PICKUP ||
            update.orderNumber.isNullOrBlank()
        ) {
            return McDonaldsDecision.None
        }

        activeSourceKey = sourceKey
        return McDonaldsDecision.Show(update)
    }

    fun onRemoved(sourceKey: String): McDonaldsDecision {
        if (sourceKey != activeSourceKey) return McDonaldsDecision.None
        reset()
        return McDonaldsDecision.Clear
    }

    fun reset() {
        activeSourceKey = null
    }
}
