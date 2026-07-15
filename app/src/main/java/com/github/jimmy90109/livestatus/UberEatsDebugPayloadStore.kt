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

object UberEatsDebugPayloadStore {
    private const val MAX_ITEMS = 30
    private val fourDigitCandidate = Regex("""(?<!\d)\d{4}(?!\d)""")
    private val timeFormatter = SimpleDateFormat("MM/dd HH:mm:ss", Locale.TAIWAN)
    private val _payloads = MutableStateFlow<List<UberEatsDebugPayload>>(emptyList())

    val payloads: StateFlow<List<UberEatsDebugPayload>> = _payloads

    fun record(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        shortCriticalText: String?,
        notificationTitle: String?,
        notificationContentText: String?,
        update: LiveStatusNotificationParser.UberEatsUpdate,
    ) {
        val notification = statusBarNotification.notification
        val payload = UberEatsDebugPayload(
            capturedAt = timeFormatter.format(Date()),
            key = statusBarNotification.key,
            id = statusBarNotification.id,
            tag = statusBarNotification.tag,
            postTime = timeFormatter.format(Date(statusBarNotification.postTime)),
            appLabel = statusBarNotification.packageName.toAppLabel(context),
            parsedEvent = update.event.name,
            parsedPin = update.pin,
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
        _payloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    fun clear() {
        _payloads.value = emptyList()
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

data class UberEatsDebugPayload(
    val capturedAt: String,
    val key: String,
    val id: Int,
    val tag: String?,
    val postTime: String,
    val appLabel: String,
    val parsedEvent: String,
    val parsedPin: String?,
    val pinCandidates: List<String>,
    val fields: Map<String, String>,
    val extras: Map<String, String>,
)
