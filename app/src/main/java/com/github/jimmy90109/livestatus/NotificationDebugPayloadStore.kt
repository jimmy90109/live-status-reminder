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
    private val _boltPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _taiwanTaxiPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _foodpandaPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _uberEatsPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _clockPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _taiwanPayPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _youBikePayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _yptPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _hevyPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _discordPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _teamsPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _googleRecorderPayloads =
        MutableStateFlow<List<NotificationDebugPayload>>(emptyList())
    private val _stravaPayloads = MutableStateFlow<List<NotificationDebugPayload>>(emptyList())

    val uberPayloads: StateFlow<List<NotificationDebugPayload>> = _uberPayloads
    val boltPayloads: StateFlow<List<NotificationDebugPayload>> = _boltPayloads
    val taiwanTaxiPayloads: StateFlow<List<NotificationDebugPayload>> = _taiwanTaxiPayloads
    val foodpandaPayloads: StateFlow<List<NotificationDebugPayload>> = _foodpandaPayloads
    val uberEatsPayloads: StateFlow<List<NotificationDebugPayload>> = _uberEatsPayloads
    val clockPayloads: StateFlow<List<NotificationDebugPayload>> = _clockPayloads
    val taiwanPayPayloads: StateFlow<List<NotificationDebugPayload>> = _taiwanPayPayloads
    val youBikePayloads: StateFlow<List<NotificationDebugPayload>> = _youBikePayloads
    val yptPayloads: StateFlow<List<NotificationDebugPayload>> = _yptPayloads
    val hevyPayloads: StateFlow<List<NotificationDebugPayload>> = _hevyPayloads
    val discordPayloads: StateFlow<List<NotificationDebugPayload>> = _discordPayloads
    val teamsPayloads: StateFlow<List<NotificationDebugPayload>> = _teamsPayloads
    val googleRecorderPayloads: StateFlow<List<NotificationDebugPayload>> =
        _googleRecorderPayloads
    val stravaPayloads: StateFlow<List<NotificationDebugPayload>> = _stravaPayloads

    internal fun recordStrava(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        lifecycle: String,
        update: StravaRecordingUpdate? = null,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = if (lifecycle == "REMOVED") lifecycle else update?.state?.name ?: "NONE",
            parsedPin = null,
            parsedDetails = linkedMapOf(
                "lifecycle" to lifecycle,
                "language" to update?.language?.name.orEmpty(),
                "officialTitle" to update?.officialTitle.orEmpty(),
                "officialText" to update?.officialText.orEmpty(),
            ),
        )
        _stravaPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    internal fun recordGoogleRecorder(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        lifecycle: String,
        extraction: RecorderExtraction? = null,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = if (lifecycle == "REMOVED") {
                lifecycle
            } else {
                extraction?.event?.name ?: lifecycle
            },
            parsedPin = null,
            parsedDetails = linkedMapOf("lifecycle" to lifecycle) +
                extraction?.diagnostics.orEmpty(),
        )
        _googleRecorderPayloads.update { current ->
            (listOf(payload) + current).take(MAX_ITEMS)
        }
    }

    internal fun recordDiscord(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        lifecycle: String,
        extraction: DiscordVoiceExtraction,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = if (lifecycle == "REMOVED") {
                lifecycle
            } else if (extraction.update == null) {
                "NONE"
            } else {
                "VOICE_ACTIVE"
            },
            parsedPin = null,
            parsedDetails = linkedMapOf("lifecycle" to lifecycle) + extraction.diagnostics,
        )
        _discordPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    internal fun recordTeams(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        lifecycle: String,
        extraction: TeamsCallExtraction? = null,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = if (lifecycle == "REMOVED") {
                lifecycle
            } else if (extraction?.update == null) {
                "NONE"
            } else {
                "CALL_ACTIVE"
            },
            parsedPin = null,
            parsedDetails = linkedMapOf("lifecycle" to lifecycle) +
                extraction?.diagnostics.orEmpty(),
        )
        _teamsPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    internal fun recordHevy(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        lifecycle: String,
        update: HevyWorkoutUpdate?,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = if (lifecycle == "REMOVED") lifecycle else update?.phase?.name ?: "NONE",
            parsedPin = null,
            parsedDetails = linkedMapOf(
                "lifecycle" to lifecycle,
                "exerciseName" to update?.exerciseName.orEmpty(),
                "setNumber" to update?.setNumber?.toString().orEmpty(),
                "totalSets" to update?.totalSets?.toString().orEmpty(),
                "setDetail" to update?.setDetail.orEmpty(),
                "sourceContentText" to update?.sourceContentText.orEmpty(),
                "restRemainingSeconds" to update?.restRemainingSeconds?.toString().orEmpty(),
                "startedAtEpochMillis" to update?.startedAtEpochMillis?.toString().orEmpty(),
            ),
        )
        _hevyPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    internal fun recordYpt(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        extraction: YptStudyExtraction,
    ) {
        val update = extraction.update
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = if (update == null) "NONE" else "RUNNING",
            parsedPin = null,
            parsedDetails = linkedMapOf(
                "sourceKey" to update?.sourceKey.orEmpty(),
                "startedAtEpochMillis" to update?.startedAtEpochMillis?.toString().orEmpty(),
                "sourceContentText" to update?.sourceContentText.orEmpty(),
            ) + extraction.diagnostics,
        )
        _yptPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    internal fun recordYouBike(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = "RAW",
            parsedPin = null,
            parsedDetails = emptyMap(),
        )
        _youBikePayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    internal fun recordTaiwanPay(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        lifecycle: String,
        update: LiveStatusNotificationParser.TaiwanPayRideUpdate,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = if (lifecycle == "REMOVED") lifecycle else update.event.name,
            parsedPin = null,
            parsedDetails = linkedMapOf(
                "lifecycle" to lifecycle,
                "parsedStationName" to update.stationName.orEmpty(),
            ),
        )
        _taiwanPayPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    internal fun recordClock(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        extraction: ClockTimerExtraction,
    ) {
        val update = extraction.update
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = update?.state?.name ?: "NONE",
            parsedPin = null,
            parsedDetails = linkedMapOf(
                "sourceKey" to update?.sourceKey.orEmpty(),
                "source" to update?.source?.name.orEmpty(),
                "endElapsedRealtimeMillis" to
                    update?.endElapsedRealtimeMillis?.toString().orEmpty(),
                "remainingMillis" to update?.remainingMillis?.toString().orEmpty(),
            ) + extraction.diagnostics,
        )
        _clockPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

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
                "parsedRideType" to update.rideType.name,
                "parsedTitle" to update.title.orEmpty(),
                "parsedOfficialText" to update.officialText.orEmpty(),
                "parsedPickupEtaMinutes" to update.pickupEtaMinutes?.toString().orEmpty(),
                "parsedPickupPoint" to update.pickupPoint.orEmpty(),
                "parsedDropoffPoint" to update.dropoffPoint.orEmpty(),
                "parsedPlate" to update.plate.orEmpty(),
                "parsedVehicle" to update.vehicle.orEmpty(),
            ),
        )
        _uberPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    fun recordBolt(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        lifecycle: String,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = lifecycle,
            parsedPin = null,
            parsedDetails = linkedMapOf("lifecycle" to lifecycle),
        )
        _boltPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
    }

    fun recordTaiwanTaxi(
        context: Context,
        statusBarNotification: StatusBarNotification,
        notificationText: String,
        notificationTitle: String?,
        notificationContentText: String?,
        update: LiveStatusNotificationParser.TaiwanTaxiUpdate,
    ) {
        val payload = createPayload(
            context = context,
            statusBarNotification = statusBarNotification,
            notificationText = notificationText,
            shortCriticalText = null,
            notificationTitle = notificationTitle,
            notificationContentText = notificationContentText,
            parsedEvent = update.event.name,
            parsedPin = null,
            parsedDetails = linkedMapOf("parsedPlate" to update.plate.orEmpty()),
        )
        _taiwanTaxiPayloads.update { current -> (listOf(payload) + current).take(MAX_ITEMS) }
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

    fun clearBolt() {
        _boltPayloads.value = emptyList()
    }

    fun clearTaiwanTaxi() {
        _taiwanTaxiPayloads.value = emptyList()
    }

    fun clearFoodpanda() {
        _foodpandaPayloads.value = emptyList()
    }

    fun clearUberEats() {
        _uberEatsPayloads.value = emptyList()
    }

    fun clearClock() {
        _clockPayloads.value = emptyList()
    }

    fun clearTaiwanPay() {
        _taiwanPayPayloads.value = emptyList()
    }

    fun clearYouBike() {
        _youBikePayloads.value = emptyList()
    }

    fun clearYpt() {
        _yptPayloads.value = emptyList()
    }

    fun clearHevy() {
        _hevyPayloads.value = emptyList()
    }

    fun clearDiscord() {
        _discordPayloads.value = emptyList()
    }

    fun clearTeams() {
        _teamsPayloads.value = emptyList()
    }

    fun clearGoogleRecorder() {
        _googleRecorderPayloads.value = emptyList()
    }

    fun clearStrava() {
        _stravaPayloads.value = emptyList()
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
                context,
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
        context: Context,
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
        "isOngoing" to
            ((notification.flags and Notification.FLAG_ONGOING_EVENT) != 0).toString(),
        "isForegroundService" to
            ((notification.flags and Notification.FLAG_FOREGROUND_SERVICE) != 0).toString(),
        "isNoClear" to
            ((notification.flags and Notification.FLAG_NO_CLEAR) != 0).toString(),
        "isGroupSummary" to
            ((notification.flags and Notification.FLAG_GROUP_SUMMARY) != 0).toString(),
        "isPromotedOngoing" to
            ((notification.flags and Notification.FLAG_PROMOTED_ONGOING) != 0).toString(),
        "when" to timeFormatter.format(Date(notification.`when`)),
        "showsChronometer" to
            notification.extras.getBoolean(Notification.EXTRA_SHOW_CHRONOMETER, false).toString(),
        "chronometerCountsDown" to
            notification.extras.getBoolean(
                Notification.EXTRA_CHRONOMETER_COUNT_DOWN,
                false,
            ).toString(),
        "style" to notification.styleName(context, statusBarNotification.packageName),
        "number" to notification.number.toString(),
        "hasContentIntent" to (notification.contentIntent != null).toString(),
        "hasDeleteIntent" to (notification.deleteIntent != null).toString(),
        "hasFullScreenIntent" to (notification.fullScreenIntent != null).toString(),
        "shortCriticalText" to shortCriticalText.orEmpty(),
        "title" to notificationTitle.orEmpty(),
        "contentText" to notificationContentText.orEmpty(),
        "joinedText" to notificationText,
        "actions" to notification.actions.orEmpty().mapIndexed { index, action ->
            buildString {
                append("#")
                append(index)
                append(" title=")
                append(action.title.toString().replace("\n", " "))
                append(" semanticAction=")
                append(action.semanticAction)
                append(" hasPendingIntent=")
                append(action.actionIntent != null)
            }
        }.joinToString(" | "),
    )

    private fun Notification.styleName(context: Context, packageName: String): String {
        val packageContext = runCatching { context.createPackageContext(packageName, 0) }
            .getOrDefault(context)
        return runCatching {
            Notification.Builder.recoverBuilder(packageContext, this).style?.javaClass?.name
        }.getOrNull().orEmpty()
    }

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
