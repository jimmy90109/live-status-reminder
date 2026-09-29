package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.service.notification.StatusBarNotification
import java.nio.charset.StandardCharsets
import java.security.MessageDigest

internal data class GenericProgressSignals(
    val sourcePackageName: String,
    val ownPackageName: String,
    val progress: Int,
    val progressMax: Int,
    val progressIndeterminate: Boolean,
    val isGroupSummary: Boolean,
    val isPromotedOngoing: Boolean,
    val isMediaNotification: Boolean,
    val hasDedicatedIntegration: Boolean,
)

internal object GenericProgressPolicy {
    fun progressPercent(signals: GenericProgressSignals): Int? {
        if (signals.sourcePackageName == signals.ownPackageName) return null
        if (signals.progressMax <= 0 || signals.progressIndeterminate) return null
        if (signals.isGroupSummary || signals.isPromotedOngoing) return null
        if (signals.isMediaNotification || signals.hasDedicatedIntegration) return null

        val boundedProgress = signals.progress.coerceIn(0, signals.progressMax)
        return ((boundedProgress.toLong() * 100L) / signals.progressMax).toInt()
    }
}

internal object GenericProgressSourcePolicy {
    private val dedicatedPackages = setOf(
        "com.citymapper.app.release",
        "tw.com.trtc.is.android05",
        "com.google.android.deskclock",
        "com.ipass.ipassmoney",
        "tw.com.twmp.twhcewallet",
        "tw.com.youbike.plus",
        "com.global.foodpanda.android",
        "com.mcdonalds.mobileapp",
        "dbx.taiwantaxi",
        "com.ubercab",
        "com.ubercab.eats",
        "com.nianticlabs.pikmin",
        "com.pallo.passiontimerscoped",
        "com.hevy",
        "com.strava",
        "com.discord",
        "com.microsoft.teams",
        "com.google.android.apps.recorder",
    )

    fun hasDedicatedIntegration(packageName: String): Boolean =
        packageName in dedicatedPackages
}

internal object GenericProgressActionPolicy {
    fun <T> select(actions: List<T>, isActionable: (T) -> Boolean): List<T> =
        actions.asSequence().filter(isActionable).take(MAX_SOURCE_ACTIONS).toList()

    private const val MAX_SOURCE_ACTIONS = 3
}

internal data class GenericProgressUpdate(
    val sourceKey: String,
    val sourcePackageName: String,
    val sourceAppName: String,
    val title: String,
    val contentText: String?,
    val progressPercent: Int,
    val smallIcon: Icon?,
    val largeIcon: Icon?,
    val contentIntent: PendingIntent?,
    val visibility: Int,
    val sourceActions: List<Notification.Action>,
)

internal object GenericProgressNotificationExtractor {
    fun extract(context: Context, statusBarNotification: StatusBarNotification): GenericProgressUpdate? {
        val notification = statusBarNotification.notification
        val packageName = statusBarNotification.packageName
        val sourceProgress = readSourceProgress(context, packageName, notification)
        val progressPercent = GenericProgressPolicy.progressPercent(
            GenericProgressSignals(
                sourcePackageName = packageName,
                ownPackageName = context.packageName,
                progress = sourceProgress.progress,
                progressMax = sourceProgress.progressMax,
                progressIndeterminate = sourceProgress.indeterminate,
                isGroupSummary = notification.flags and Notification.FLAG_GROUP_SUMMARY != 0,
                isPromotedOngoing = notification.isPromotedOngoing(),
                isMediaNotification = notification.isMediaNotification(),
                hasDedicatedIntegration =
                    GenericProgressSourcePolicy.hasDedicatedIntegration(packageName),
            ),
        ) ?: return null
        val appName = applicationLabel(context, packageName)
        val title = notification.extras
            .getCharSequence(Notification.EXTRA_TITLE)
            .normalizedText()
            ?: appName
        val contentText = notification.extras
            .getCharSequence(Notification.EXTRA_TEXT)
            .normalizedText()
            ?: notification.extras
                .getCharSequence(Notification.EXTRA_BIG_TEXT)
                .normalizedText()

        return GenericProgressUpdate(
            sourceKey = statusBarNotification.key,
            sourcePackageName = packageName,
            sourceAppName = appName,
            title = title,
            contentText = contentText,
            progressPercent = progressPercent,
            smallIcon = notification.smallIcon?.takeIf { it.type == Icon.TYPE_RESOURCE },
            largeIcon = notification.getLargeIcon(),
            contentIntent = notification.contentIntent,
            visibility = notification.visibility,
            sourceActions = GenericProgressActionPolicy.select(
                notification.actions.orEmpty().toList(),
            ) { it.actionIntent != null },
        )
    }

    private fun readSourceProgress(
        context: Context,
        packageName: String,
        notification: Notification,
    ): SourceProgress {
        val progressStyle = runCatching {
            val packageContext = context.createPackageContext(packageName, 0)
            Notification.Builder.recoverBuilder(packageContext, notification).style
                as? Notification.ProgressStyle
        }.getOrNull()
        if (progressStyle != null) {
            return SourceProgress(
                progress = progressStyle.progress,
                progressMax = progressStyle.progressMax,
                indeterminate = progressStyle.isProgressIndeterminate,
            )
        }

        return SourceProgress(
            progress = notification.extras.getInt(Notification.EXTRA_PROGRESS, 0),
            progressMax = notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0),
            indeterminate = notification.extras.getBoolean(
                Notification.EXTRA_PROGRESS_INDETERMINATE,
                false,
            ),
        )
    }

    private fun applicationLabel(context: Context, packageName: String): String =
        runCatching {
            val applicationInfo = context.packageManager.getApplicationInfo(
                packageName,
                PackageManager.ApplicationInfoFlags.of(0),
            )
            context.packageManager.getApplicationLabel(applicationInfo).toString()
        }.getOrNull().normalizedText() ?: packageName

    private fun CharSequence?.normalizedText(): String? =
        this?.toString()?.trim()?.takeIf(String::isNotEmpty)

    private data class SourceProgress(
        val progress: Int,
        val progressMax: Int,
        val indeterminate: Boolean,
    )
}

internal sealed interface GenericProgressDecision {
    data class Show(val update: GenericProgressUpdate) : GenericProgressDecision
    data class Clear(val sourceKey: String) : GenericProgressDecision
}

internal class GenericProgressTracker {
    private val sourceKeys = linkedSetOf<String>()

    fun onPosted(
        sourceKey: String,
        update: GenericProgressUpdate?,
    ): GenericProgressDecision = if (update == null) {
        sourceKeys.remove(sourceKey)
        GenericProgressDecision.Clear(sourceKey)
    } else {
        sourceKeys += sourceKey
        GenericProgressDecision.Show(update)
    }

    fun onRemoved(sourceKey: String): GenericProgressDecision {
        sourceKeys.remove(sourceKey)
        return GenericProgressDecision.Clear(sourceKey)
    }

    fun restore(updates: List<GenericProgressUpdate>): List<GenericProgressDecision.Show> {
        sourceKeys.clear()
        sourceKeys += updates.map(GenericProgressUpdate::sourceKey)
        return updates.map { GenericProgressDecision.Show(it) }
    }

    fun reset(): List<String> = sourceKeys.toList().also { sourceKeys.clear() }
}

internal object GenericProgressNotificationIdentity {
    private const val TAG_PREFIX = "generic_progress:"

    fun tag(sourceKey: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(sourceKey.toByteArray(StandardCharsets.UTF_8))
            .joinToString(separator = "") { "%02x".format(it) }
        return TAG_PREFIX + digest
    }

    fun isGenericProgressTag(tag: String?): Boolean = tag?.startsWith(TAG_PREFIX) == true
}
