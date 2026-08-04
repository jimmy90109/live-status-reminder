package com.github.jimmy90109.livestatus

object TaiwanTaxiRideManager {
    private var lastUpdate = LiveStatusNotificationParser.TaiwanTaxiUpdate(
        LiveStatusNotificationParser.TaiwanTaxiEvent.NONE,
    )

    @Synchronized
    fun handle(
        context: android.content.Context,
        update: LiveStatusNotificationParser.TaiwanTaxiUpdate,
    ) {
        when (update.event) {
            LiveStatusNotificationParser.TaiwanTaxiEvent.TRIP_ENDED -> clear(context)
            LiveStatusNotificationParser.TaiwanTaxiEvent.NONE -> Unit
            else -> {
                lastUpdate = merge(lastUpdate, update)
                LiveStatusReminder.showTaiwanTaxi(context, lastUpdate)
            }
        }
    }

    @Synchronized
    fun clear(context: android.content.Context) {
        lastUpdate = LiveStatusNotificationParser.TaiwanTaxiUpdate(
            LiveStatusNotificationParser.TaiwanTaxiEvent.NONE,
        )
        LiveStatusReminder.clearTaiwanTaxi(context)
    }

    internal fun merge(
        previous: LiveStatusNotificationParser.TaiwanTaxiUpdate,
        update: LiveStatusNotificationParser.TaiwanTaxiUpdate,
    ): LiveStatusNotificationParser.TaiwanTaxiUpdate {
        val event = if (eventRank(update.event) >= eventRank(previous.event)) {
            update.event
        } else {
            previous.event
        }
        return LiveStatusNotificationParser.TaiwanTaxiUpdate(
            event = event,
            plate = update.plate ?: previous.plate,
        )
    }

    private fun eventRank(event: LiveStatusNotificationParser.TaiwanTaxiEvent): Int =
        when (event) {
            LiveStatusNotificationParser.TaiwanTaxiEvent.DRIVER_FOUND -> 1
            LiveStatusNotificationParser.TaiwanTaxiEvent.VEHICLE_ARRIVED -> 2
            LiveStatusNotificationParser.TaiwanTaxiEvent.TRIP_ENDED -> 3
            LiveStatusNotificationParser.TaiwanTaxiEvent.NONE -> 0
        }
}
