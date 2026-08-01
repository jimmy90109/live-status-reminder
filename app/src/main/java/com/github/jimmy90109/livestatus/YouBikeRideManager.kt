package com.github.jimmy90109.livestatus

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.annotation.VisibleForTesting
import java.time.LocalDateTime
import java.time.ZoneId

object YouBikeRideSessionStore {
    private const val PREFERENCES = "you_bike_ride_session"
    private const val KEY_ID = "id"
    private const val KEY_BORROWED_AT = "borrowed_at"
    private const val KEY_STATION = "station"
    private const val KEY_DOCK = "dock"
    private const val KEY_BIKE = "bike"
    private const val KEY_REGION = "region"
    private const val KEY_CANDIDATES = "candidate_regions"
    private const val KEY_MANUAL_REGION = "manual_region"
    private const val KEY_RESOLUTION_ISSUE = "resolution_issue"

    fun load(context: Context): YouBikeRideSession? {
        val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
        val id = preferences.getString(KEY_ID, null) ?: return null
        val station = preferences.getString(KEY_STATION, null) ?: return null
        val bike = preferences.getString(KEY_BIKE, null) ?: return null
        val borrowedAt = preferences.getLong(KEY_BORROWED_AT, -1L).takeIf { it > 0 } ?: return null
        val region = preferences.getString(KEY_REGION, null).toRegion() ?: return null
        return YouBikeRideSession(
            id = id,
            borrowedAtMillis = borrowedAt,
            stationName = station,
            dockNumber = preferences.getString(KEY_DOCK, null),
            bikeNumber = bike,
            region = region,
            candidateRegions = preferences.getStringSet(KEY_CANDIDATES, emptySet()).orEmpty()
                .mapNotNull { it.toRegion() }.toSet(),
            manuallySelectedRegion = preferences.getString(KEY_MANUAL_REGION, null).toRegion(),
            originalResolutionIssue = preferences.getString(KEY_RESOLUTION_ISSUE, null).toResolutionIssue(),
        )
    }

    fun save(context: Context, session: YouBikeRideSession) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
            .putString(KEY_ID, session.id)
            .putLong(KEY_BORROWED_AT, session.borrowedAtMillis)
            .putString(KEY_STATION, session.stationName)
            .putString(KEY_DOCK, session.dockNumber)
            .putString(KEY_BIKE, session.bikeNumber)
            .putString(KEY_REGION, session.region.name)
            .putStringSet(KEY_CANDIDATES, session.candidateRegions.mapTo(mutableSetOf()) { it.name })
            .putString(KEY_MANUAL_REGION, session.manuallySelectedRegion?.name)
            .putString(KEY_RESOLUTION_ISSUE, session.originalResolutionIssue?.name)
            .apply()
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit().clear().apply()
    }

    private fun String?.toRegion(): YouBikeRegion? =
        this?.let { runCatching { YouBikeRegion.valueOf(it) }.getOrNull() }

    private fun String?.toResolutionIssue(): YouBikeStationResolutionIssue? =
        this?.let { runCatching { YouBikeStationResolutionIssue.valueOf(it) }.getOrNull() }
}

object YouBikeRideManager {
    private val taipeiZone = ZoneId.of("Asia/Taipei")

    fun handle(context: Context, update: YouBikeRideUpdate, nowMillis: Long = System.currentTimeMillis()) {
        when (update.event) {
            YouBikeEvent.BORROWED -> start(context, update, nowMillis)
            YouBikeEvent.RETURNED -> finish(context, update)
            YouBikeEvent.NONE -> Unit
        }
    }

    fun restore(context: Context, nowMillis: Long = System.currentTimeMillis()) {
        var session = YouBikeRideSessionStore.load(context) ?: return
        if (session.isExpired(nowMillis) || !AppReminderPreferences.App.YOUBIKE.isEnabled(context)) {
            clear(context)
            return
        }
        session = refreshStoredRegionIfNeeded(context, session)
        refresh(context, session.id, nowMillis)
    }

    fun canScheduleExactAlarms(context: Context): Boolean =
        context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun selectRegion(context: Context, sessionId: String, region: YouBikeRegion): Boolean {
        val session = YouBikeRideSessionStore.load(context) ?: return false
        if (session.id != sessionId || session.isExpired(System.currentTimeMillis())) return false
        val updated = session.copy(region = region, manuallySelectedRegion = region)
        YouBikeRideSessionStore.save(context, updated)
        refresh(context, sessionId)
        return true
    }

    fun refresh(
        context: Context,
        expectedSessionId: String? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val session = YouBikeRideSessionStore.load(context) ?: return
        if (expectedSessionId != null && session.id != expectedSessionId) return
        if (session.isExpired(nowMillis)) {
            clear(context)
            return
        }
        LiveStatusReminder.showYouBike(context, session, nowMillis)
        schedule(context, session, nowMillis)
    }

    fun clear(context: Context) {
        YouBikeRideSessionStore.load(context)?.let { cancelAlarm(context, it.id) }
        YouBikeRideSessionStore.clear(context)
        LiveStatusReminder.clearYouBike(context)
    }

    private fun start(context: Context, update: YouBikeRideUpdate, nowMillis: Long) {
        val occurredAt = update.occurredAt ?: return
        val station = update.stationName ?: return
        val bike = update.bikeNumber ?: return
        val borrowedAt = occurredAt.toEpochMillis()
        if (borrowedAt > nowMillis + 5 * 60_000L || nowMillis - borrowedAt >= 24 * 60 * 60_000L) return
        val current = YouBikeRideSessionStore.load(context)
        if (current?.borrowedAtMillis == borrowedAt && current.bikeNumber == bike) {
            refresh(context, current.id, nowMillis)
            return
        }
        if (!YouBikeSessionPolicy.shouldReplace(current, borrowedAt, bike)) return
        current?.let { cancelAlarm(context, it.id) }
        val resolution = resolveStation(context, station)
        val region = when (resolution) {
            is YouBikeStationResolution.Supported -> resolution.region
            YouBikeStationResolution.Unsupported -> YouBikeRegion.UNSUPPORTED
            else -> YouBikeRegion.UNRESOLVED
        }
        val candidates = when (resolution) {
            is YouBikeStationResolution.Supported -> resolution.candidates
            is YouBikeStationResolution.Ambiguous -> resolution.candidates
            else -> emptySet()
        }
        val session = YouBikeRideSession(
            id = "$borrowedAt-$bike",
            borrowedAtMillis = borrowedAt,
            stationName = station,
            dockNumber = update.dockNumber,
            bikeNumber = bike,
            region = region,
            candidateRegions = candidates,
            originalResolutionIssue = when (resolution) {
                is YouBikeStationResolution.Ambiguous -> YouBikeStationResolutionIssue.AMBIGUOUS
                YouBikeStationResolution.Unknown -> YouBikeStationResolutionIssue.UNKNOWN
                else -> null
            },
        )
        YouBikeRideSessionStore.save(context, session)
        refresh(context, session.id, nowMillis)
    }

    private fun finish(context: Context, update: YouBikeRideUpdate) {
        val current = YouBikeRideSessionStore.load(context) ?: return
        val returnedAt = update.occurredAt?.toEpochMillis() ?: return
        if (!YouBikeSessionPolicy.shouldEnd(current, update.bikeNumber, returnedAt)) return
        val feedbackReport = YouBikeFeedbackPolicy.createReport(
            session = current,
            appVersionName = BuildConfig.VERSION_NAME,
            appVersionCode = BuildConfig.VERSION_CODE.toLong(),
            stationIndexVersion = stationIndexVersion(context),
        )
        clear(context)
        feedbackReport?.let { YouBikeFeedbackNotifier.show(context, it) }
    }

    private fun stationIndexVersion(context: Context): String =
        context.resources.openRawResource(R.raw.youbike_stations).bufferedReader().use { reader ->
            YouBikeStationIndexMetadata.parseVersion(reader.lineSequence())
        } ?: "app-${BuildConfig.VERSION_NAME}-${BuildConfig.VERSION_CODE}"

    private fun refreshStoredRegionIfNeeded(
        context: Context,
        session: YouBikeRideSession,
    ): YouBikeRideSession {
        if (
            session.manuallySelectedRegion != null ||
            session.region !in setOf(YouBikeRegion.UNSUPPORTED, YouBikeRegion.UNRESOLVED)
        ) {
            return session
        }
        val resolution = resolveStation(context, session.stationName)
        val updated = YouBikeSessionPolicy.withResolution(session, resolution)
        if (updated != session) YouBikeRideSessionStore.save(context, updated)
        return updated
    }

    @VisibleForTesting
    internal fun resolveStation(context: Context, stationName: String): YouBikeStationResolution =
        context.resources.openRawResource(R.raw.youbike_stations).bufferedReader().use { reader ->
            YouBikeStationResolver.resolve(stationName, reader.lineSequence())
        }

    private fun schedule(context: Context, session: YouBikeRideSession, nowMillis: Long) {
        cancelAlarm(context, session.id)
        val nextAt = YouBikeFarePolicy.estimate(
            session.borrowedAtMillis,
            nowMillis,
            session.region,
            session.vehicleType,
        )?.nextBoundaryMillis ?: return
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        // The previous half-hour remains valid at the exact boundary; schedule one
        // millisecond after it so the refreshed estimate enters the new interval.
        val triggerAtMillis = YouBikeAlarmPolicy.triggerAtMillis(nextAt, nowMillis)
        val operation = alarmIntent(context, session.id)
        YouBikeAlarmPolicy.schedule(
            canScheduleExactAlarms = alarmManager.canScheduleExactAlarms(),
            scheduleExact = {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    operation,
                )
            },
            scheduleInexact = {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    operation,
                )
            },
        )
    }

    private fun cancelAlarm(context: Context, sessionId: String) {
        context.getSystemService(AlarmManager::class.java).cancel(alarmIntent(context, sessionId))
    }

    private fun alarmIntent(context: Context, sessionId: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            sessionId.hashCode(),
            Intent(context, YouBikeFareAlarmReceiver::class.java)
                .setAction(ACTION_REFRESH)
                .putExtra(EXTRA_SESSION_ID, sessionId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun LocalDateTime.toEpochMillis(): Long =
        atZone(taipeiZone).toInstant().toEpochMilli()

    internal const val ACTION_REFRESH = "com.github.jimmy90109.livestatus.action.REFRESH_YOUBIKE"
    internal const val EXTRA_SESSION_ID = "you_bike_session_id"
}

internal object YouBikeSessionPolicy {
    fun withResolution(
        session: YouBikeRideSession,
        resolution: YouBikeStationResolution,
    ): YouBikeRideSession = when (resolution) {
        is YouBikeStationResolution.Supported -> session.copy(
            region = resolution.region,
            candidateRegions = resolution.candidates,
        )
        is YouBikeStationResolution.Ambiguous -> session.copy(
            region = YouBikeRegion.UNRESOLVED,
            candidateRegions = resolution.candidates,
        )
        YouBikeStationResolution.Unsupported -> session.copy(
            region = YouBikeRegion.UNSUPPORTED,
            candidateRegions = emptySet(),
        )
        YouBikeStationResolution.Unknown -> session.copy(
            region = YouBikeRegion.UNRESOLVED,
            candidateRegions = emptySet(),
        )
    }

    fun shouldReplace(
        current: YouBikeRideSession?,
        borrowedAtMillis: Long,
        bikeNumber: String,
    ): Boolean = current == null ||
        borrowedAtMillis > current.borrowedAtMillis ||
        (borrowedAtMillis == current.borrowedAtMillis && bikeNumber != current.bikeNumber)

    fun shouldEnd(
        current: YouBikeRideSession,
        returnedBikeNumber: String?,
        returnedAtMillis: Long,
    ): Boolean = returnedBikeNumber == current.bikeNumber &&
        returnedAtMillis >= current.borrowedAtMillis
}

internal enum class YouBikeAlarmMode {
    EXACT,
    INEXACT,
}

internal object YouBikeAlarmPolicy {
    fun mode(canScheduleExactAlarms: Boolean): YouBikeAlarmMode =
        if (canScheduleExactAlarms) YouBikeAlarmMode.EXACT else YouBikeAlarmMode.INEXACT

    fun triggerAtMillis(nextBoundaryMillis: Long, nowMillis: Long): Long =
        (nextBoundaryMillis + 1L).coerceAtLeast(nowMillis + 1_000L)

    fun schedule(
        canScheduleExactAlarms: Boolean,
        scheduleExact: () -> Unit,
        scheduleInexact: () -> Unit,
    ): YouBikeAlarmMode {
        if (mode(canScheduleExactAlarms) == YouBikeAlarmMode.EXACT) {
            try {
                scheduleExact()
                return YouBikeAlarmMode.EXACT
            } catch (_: SecurityException) {
                // Permission can be revoked between the capability check and scheduling.
            }
        }
        scheduleInexact()
        return YouBikeAlarmMode.INEXACT
    }
}

class YouBikeFareAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED) {
            if (YouBikeRideManager.canScheduleExactAlarms(context)) {
                YouBikeRideManager.restore(context)
            }
            return
        }
        if (intent.action != YouBikeRideManager.ACTION_REFRESH) return
        if (!AppReminderPreferences.App.YOUBIKE.isEnabled(context)) {
            YouBikeRideManager.clear(context)
            return
        }
        val sessionId = intent.getStringExtra(YouBikeRideManager.EXTRA_SESSION_ID) ?: return
        YouBikeRideManager.refresh(context, sessionId)
    }
}
