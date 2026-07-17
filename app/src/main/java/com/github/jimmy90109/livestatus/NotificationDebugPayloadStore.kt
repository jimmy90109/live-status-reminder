package com.github.jimmy90109.livestatus

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object NotificationDebugPayloadStore {
    private const val MAX_ITEMS = 30
    private val fourDigitCandidate = Regex("""(?<!\d)\d{4}(?!\d)""")
    private val timeFormatter = SimpleDateFormat("MM/dd HH:mm:ss", Locale.TAIWAN)
    private val _uberPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _foodpandaPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _uberEatsPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())

    val uberPayloads: StateFlow<List<NotificationDebugPayload>> = _uberPayloads
    val foodpandaPayloads: StateFlow<List<NotificationDebugPayload>> = _foodpandaPayloads
    val uberEatsPayloads: StateFlow<List<NotificationDebugPayload>> = _uberEatsPayloads

    fun recordUber(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        shortCriticalText: String?,
        notificationTitle: String?,
        notificationContentText: String?,
        update: LiveStatusNotificationParser.UberRideUpdate,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = shortCriticalText,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = update.event.name,
            parsedPin = update.pin,
            parsedDetails = linkedMapOf(
                "parsedTitle" to update.title.orEmpty(),
                "parsedPickupPoint" to update.pickupPoint.orEmpty(),
                "parsedDropoffPoint" to update.dropoffPoint.orEmpty(),
                "parsedPlate" to update.plate.orEmpty(),
                "parsedVehicle" to update.vehicle.orEmpty(),
            ),
        )
        _uberPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    fun recordFoodpanda(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        event: LiveStatusNotificationParser.FoodpandaEvent,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = event.name,
            parsedPin = null,
            parsedDetails = emptyMap(),
        )
        _foodpandaPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    fun recordUberEats(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        shortCriticalText: String?,
        notificationTitle: String?,
        notificationContentText: String?,
        update: LiveStatusNotificationParser.UberEatsUpdate,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = shortCriticalText,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = update.event.name,
            parsedPin = update.pin,
            parsedDetails = emptyMap(),
        )
        _uberEatsPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    fun clearUber() {
        _uberPayloads.value = emptyList()
    }

    fun clearFoodpanda() {
        _foodpandaPayloads.value = emptyList()
    }

    fun clearUberEats() {
        _uberEatsPayloads.value = emptyList()
    }

    private fun createPayload(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        shortCriticalText: String?,
        notificationTitle: String?,
        notificationContentText: String?,
        parsedEvent: String,
        parsedPin: String?,
        parsedDetails: Map<String, String>,
    ): NotificationDebugPayload {
        val notification = statusBarNotification.notification
        return NotificationDebugPayload(
            capturedAt = timeFormatter.format(Date()),
            key = statusBarNotification.key,
            id = statusBarNotification.id,
            tag = statusBarNotification.tag,
            postTime = timeFormatter.format(Date(statusBarNotification.postTime)),
            appLabel = statusBarNotification.packageName.toAppLabel(context),
            parsedEvent = parsedEvent,
            parsedPin = parsedPin,
            parsedDetails = parsedDetails,
            pinCandidates = pinCandidates(notification, notificationText, shortCriticalText),
            fields = notificationFields(
                statusBarNotification,
                notification,
                notificationText,
                shortCriticalText,
                notificationTitle,
                notificationContentText,
            ),
            extras = notification.extras.toDebugMap(),
        )
    }

    private fun notificationFields(
        statusBarNotification: StatusBarNotification,
        notification: Notification,
        notificationText: String,
        shortCriticalText: String?,
        notificationTitle: String?,
        notificationContentText: String?,
    ): Map<String, String> = linkedMapOf(
        "packageName" to statusBarNotification.packageName,
        "key" to statusBarNotification.key,
        "id" to statusBarNotification.id.toString(),
        "tag" to statusBarNotification.tag.orEmpty(),
        "postTime" to timeFormatter.format(Date(statusBarNotification.postTime)),
        "channelId" to notification.channelId.orEmpty(),
        "category" to notification.category.orEmpty(),
        "group" to notification.group.orEmpty(),
        "sortKey" to notification.sortKey.orEmpty(),
        "priority" to notification.priority.toString(),
        "flags" to notification.flags.toString(),
        "isGroupSummary" to
            ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0).toString(),
        "when" to timeFormatter.format(Date(notification.`when`)),
        "number" to notification.number.toString(),
        "shortCriticalText" to shortCriticalText.orEmpty(),
        "title" to notificationTitle.orEmpty(),
        "contentText" to notificationContentText.orEmpty(),
        "joinedText" to notificationText,
    )

    private fun pinCandidates(
        notification: Notification,
        notificationText: String,
        shortCriticalText: String?,
    ): List<String> {
        val source = buildString {
            appendLine(shortCriticalText.orEmpty())
            appendLine(notificationText)
            notification.extras.keySet().sorted().forEach { key ->
                appendLine(notification.extras.get(key).toDebugString())
            }
        }
        return fourDigitCandidate.findAll(source).map { it.value }.distinct().toList()
    }

    private fun Bundle.toDebugMap(): Map<String, String> =
        keySet().sorted().associateWith { key -> get(key).toDebugString() }

    private fun String.toAppLabel(context: Context): String =
        runCatching {
            val packageManager = context.packageManager
            val applicationInfo = packageManager.getApplicationInfo(this, 0)
            packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrDefault(this)

    private fun Any?.toDebugString(): String = when (this) {
        null -> ""
        is CharSequence -> this.toString()
        is Array<*> -> this.joinToString(prefix = "[", postfix = "]") { it.toDebugString() }
        is IntArray -> this.joinToString(prefix = "[", postfix = "]")
        is LongArray -> this.joinToString(prefix = "[", postfix = "]")
        is FloatArray -> this.joinToString(prefix = "[", postfix = "]")
        is DoubleArray -> this.joinToString(prefix = "[", postfix = "]")
        is BooleanArray -> this.joinToString(prefix = "[", postfix = "]")
        is Bundle -> this.toDebugMap().entries.joinToString(prefix = "{", postfix = "}") {
            "${it.key}=${it.value}"
        }
        else -> this.toString()
    }
}

data class NotificationDebugPayload(
    val capturedAt: String,
    val key: String,
    val id: Int,
    val tag: String?,
    val postTime: String,
    val appLabel: String,
    val parsedEvent: String,
    val parsedPin: String?,
    val parsedDetails: Map<String, String>,
    val pinCandidates: List<String>,
    val fields: Map<String, String>,
    val extras: Map<String, String>,
)
