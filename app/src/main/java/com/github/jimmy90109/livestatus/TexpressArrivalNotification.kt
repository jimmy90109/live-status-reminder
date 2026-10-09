package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification

internal data class TexpressArrival(
    val trainNumber: String,
    val minutes: Int,
    val station: String,
)

internal object TexpressArrivalNotificationParser {
    const val PACKAGE_NAME = "tw.com.thsrc.texpress"

    private val inlineWhitespace = Regex("\\s+")
    private val arrivalTitle = Regex(
        """^T\s*[- ]?\s*Express\s*列車到站提醒\s*[！!]?$""",
        RegexOption.IGNORE_CASE,
    )
    private val arrivalText = Regex(
        """(?:您\s*搭乘\s*)?台灣高鐵\s*(\d{1,4})\s*車次\s*即將於\s*(\d{1,3})\s*分鐘後\s*抵達\s*([^。.!！\n]+?站)\s*[。.!！]?$""",
    )

    fun isEligible(packageName: String?, isGroupSummary: Boolean): Boolean =
        packageName == PACKAGE_NAME && !isGroupSummary

    fun parse(
        notificationTitle: String?,
        notificationContentText: String?,
        notificationText: String? = null,
    ): TexpressArrival? {
        if (!arrivalTitle.matches(normalize(notificationTitle))) return null
        val candidates = sequenceOf(notificationContentText, notificationText)
            .filterNotNull()
            .flatMap { it.lineSequence() }
            .map(::normalize)
            .filter(String::isNotEmpty)
        val match = candidates.firstNotNullOfOrNull(arrivalText::find) ?: return null
        val minutes = match.groupValues[2].toIntOrNull()?.takeIf { it > 0 } ?: return null
        return TexpressArrival(
            trainNumber = match.groupValues[1],
            minutes = minutes,
            station = normalize(match.groupValues[3]),
        )
    }

    private fun normalize(value: String?): String =
        value.orEmpty().replace(inlineWhitespace, " ").trim()
}

internal data class TexpressArrivalUpdate(
    val sourceKey: String,
    val postTime: Long,
    val arrival: TexpressArrival,
    val picture: Bitmap? = null,
    val contentIntent: PendingIntent? = null,
)

internal object TexpressArrivalNotificationExtractor {
    fun extract(
        context: Context,
        source: StatusBarNotification,
        notificationText: String? = null,
    ): TexpressArrivalUpdate? {
        val notification = source.notification
        if (!TexpressArrivalNotificationParser.isEligible(
                packageName = source.packageName,
                isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
            )
        ) return null
        val arrival = TexpressArrivalNotificationParser.parse(
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
        return TexpressArrivalUpdate(
            sourceKey = source.key,
            postTime = source.postTime,
            arrival = arrival,
            picture = readStandardPicture(context, notification),
            contentIntent = notification.contentIntent,
        )
    }

    private fun readStandardPicture(context: Context, notification: Notification): Bitmap? {
        val pictureIcon = notification.extras.getParcelable(
            Notification.EXTRA_PICTURE_ICON,
            Icon::class.java,
        )
        val iconBitmap = pictureIcon?.let { icon ->
            runCatching { icon.loadDrawable(context) }
                .getOrNull()
                ?.let { drawable ->
                    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: return@let null
                    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: return@let null
                    Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
                        val canvas = Canvas(bitmap)
                        drawable.setBounds(0, 0, canvas.width, canvas.height)
                        drawable.draw(canvas)
                    }
                }
        }
        if (iconBitmap != null) return iconBitmap
        return notification.extras.getParcelable(
            Notification.EXTRA_PICTURE,
            Bitmap::class.java,
        )
    }
}

internal sealed interface TexpressArrivalDecision {
    data class Show(val update: TexpressArrivalUpdate) : TexpressArrivalDecision
    data object Clear : TexpressArrivalDecision
    data object None : TexpressArrivalDecision
}

internal class TexpressArrivalTracker {
    private val updates = mutableMapOf<String, TexpressArrivalUpdate>()
    private var activeSourceKey: String? = null

    fun onPosted(sourceKey: String, update: TexpressArrivalUpdate?): TexpressArrivalDecision {
        if (update == null) return onRemoved(sourceKey)
        updates[sourceKey] = update
        val latest = latest() ?: return TexpressArrivalDecision.Clear
        activeSourceKey = latest.sourceKey
        return TexpressArrivalDecision.Show(latest)
    }

    fun onRemoved(sourceKey: String): TexpressArrivalDecision {
        if (updates.remove(sourceKey) == null) return TexpressArrivalDecision.None
        if (sourceKey != activeSourceKey) return TexpressArrivalDecision.None
        val latest = latest()
        activeSourceKey = latest?.sourceKey
        return latest?.let(TexpressArrivalDecision::Show) ?: TexpressArrivalDecision.Clear
    }

    fun restore(restoredUpdates: List<TexpressArrivalUpdate>): TexpressArrivalDecision {
        updates.clear()
        restoredUpdates.forEach { updates[it.sourceKey] = it }
        val latest = latest()
        activeSourceKey = latest?.sourceKey
        return latest?.let(TexpressArrivalDecision::Show) ?: TexpressArrivalDecision.Clear
    }

    fun reset(): TexpressArrivalDecision {
        updates.clear()
        activeSourceKey = null
        return TexpressArrivalDecision.Clear
    }

    private fun latest(): TexpressArrivalUpdate? = updates.values.maxByOrNull { it.postTime }
}
