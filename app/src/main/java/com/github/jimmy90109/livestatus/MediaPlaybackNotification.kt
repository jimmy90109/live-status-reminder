package com.github.jimmy90109.livestatus

import android.app.Notification
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Icon
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSession
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.SystemClock
import android.service.notification.StatusBarNotification

internal data class MediaPlaybackCandidate(
    val sessionKey: String,
    val playbackState: Int,
    val hasSourceNotification: Boolean,
    val sourcePromoted: Boolean,
)

internal enum class MediaPlaybackControl {
    SKIP_PREVIOUS,
    SEEK_BACK_15_SECONDS,
    REWIND,
    SKIP_NEXT,
    SEEK_FORWARD_15_SECONDS,
    FAST_FORWARD,
}

internal data class MediaPlaybackSideControls(
    val left: MediaPlaybackControl?,
    val right: MediaPlaybackControl?,
)

internal object MediaPlaybackControlPolicy {
    fun select(actions: Long): MediaPlaybackSideControls {
        val canSeek = actions supports PlaybackState.ACTION_SEEK_TO
        val canRewind = actions supports PlaybackState.ACTION_REWIND
        val canFastForward = actions supports PlaybackState.ACTION_FAST_FORWARD
        val canSkipPrevious = actions supports PlaybackState.ACTION_SKIP_TO_PREVIOUS
        val canSkipNext = actions supports PlaybackState.ACTION_SKIP_TO_NEXT
        val hasTimeControlSignal = canRewind || canFastForward

        if (hasTimeControlSignal && canSeek) {
            return MediaPlaybackSideControls(
                left = MediaPlaybackControl.SEEK_BACK_15_SECONDS,
                right = MediaPlaybackControl.SEEK_FORWARD_15_SECONDS,
            )
        }
        if (hasTimeControlSignal) {
            return MediaPlaybackSideControls(
                left = when {
                    canRewind -> MediaPlaybackControl.REWIND
                    canSkipPrevious -> MediaPlaybackControl.SKIP_PREVIOUS
                    else -> null
                },
                right = when {
                    canFastForward -> MediaPlaybackControl.FAST_FORWARD
                    canSkipNext -> MediaPlaybackControl.SKIP_NEXT
                    else -> null
                },
            )
        }
        if (canSeek && !canSkipPrevious && !canSkipNext) {
            return MediaPlaybackSideControls(
                left = MediaPlaybackControl.SEEK_BACK_15_SECONDS,
                right = MediaPlaybackControl.SEEK_FORWARD_15_SECONDS,
            )
        }
        return MediaPlaybackSideControls(
            left = MediaPlaybackControl.SKIP_PREVIOUS.takeIf { canSkipPrevious },
            right = MediaPlaybackControl.SKIP_NEXT.takeIf { canSkipNext },
        )
    }

    private infix fun Long.supports(action: Long): Boolean = this and action != 0L
}

internal object MediaPlaybackSeekPosition {
    fun calculate(
        positionMillis: Long,
        lastPositionUpdateElapsedMillis: Long,
        playbackSpeed: Float,
        playbackState: Int,
        nowElapsedMillis: Long,
        offsetMillis: Long,
        durationMillis: Long?,
    ): Long? {
        if (positionMillis == PlaybackState.PLAYBACK_POSITION_UNKNOWN || positionMillis < 0L) {
            return null
        }
        val elapsedMillis = (nowElapsedMillis - lastPositionUpdateElapsedMillis).coerceAtLeast(0L)
        val estimatedPosition = if (
            playbackState == PlaybackState.STATE_PLAYING &&
            lastPositionUpdateElapsedMillis > 0L &&
            playbackSpeed.isFinite()
        ) {
            positionMillis.toDouble() + elapsedMillis.toDouble() * playbackSpeed.toDouble()
        } else {
            positionMillis.toDouble()
        }
        val upperBound = durationMillis?.takeIf { it > 0L }?.toDouble()
            ?: Long.MAX_VALUE.toDouble()
        return (estimatedPosition + offsetMillis.toDouble())
            .coerceIn(0.0, upperBound)
            .toLong()
    }
}

internal object MediaPlaybackProgress {
    fun percent(positionMillis: Long?, durationMillis: Long?): Int? {
        if (positionMillis == null || durationMillis == null || durationMillis <= 0L) return null
        return (positionMillis.toDouble() / durationMillis.toDouble() * 100.0)
            .coerceIn(0.0, 100.0)
            .toInt()
    }
}

internal object MediaPlaybackText {
    fun artistAndAlbum(artist: String?, album: String?): String? {
        val cleanArtist = artist?.trim()?.takeIf(String::isNotEmpty)
        val cleanAlbum = album?.trim()?.takeIf(String::isNotEmpty)
        return listOfNotNull(
            cleanArtist,
            cleanAlbum?.takeUnless { it.equals(cleanArtist, ignoreCase = true) },
        ).joinToString(" - ").ifBlank { null }
    }
}

internal object MediaPlaybackPolicy {
    fun select(
        candidates: List<MediaPlaybackCandidate>,
        lastPlayingSessionKey: String?,
    ): MediaPlaybackCandidate? {
        val candidate = candidates.firstOrNull {
            it.hasSourceNotification && isOngoingPlayback(
                playbackState = it.playbackState,
                sameSessionWasPlaying = it.sessionKey == lastPlayingSessionKey,
            )
        }
        return candidate?.takeUnless { it.sourcePromoted }
    }

    fun isOngoingPlayback(playbackState: Int, sameSessionWasPlaying: Boolean): Boolean =
        playbackState == PlaybackState.STATE_PLAYING ||
            sameSessionWasPlaying && (
                playbackState == PlaybackState.STATE_BUFFERING ||
                    playbackState == PlaybackState.STATE_CONNECTING ||
                    playbackState == PlaybackState.STATE_FAST_FORWARDING ||
                    playbackState == PlaybackState.STATE_REWINDING ||
                    playbackState == PlaybackState.STATE_SKIPPING_TO_PREVIOUS ||
                    playbackState == PlaybackState.STATE_SKIPPING_TO_NEXT ||
                    playbackState == PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM
                )

    fun canRetainPaused(playbackState: Int, sameSessionWasPlaying: Boolean): Boolean =
        sameSessionWasPlaying && playbackState == PlaybackState.STATE_PAUSED
}

internal class MediaPauseGracePeriod(
    private val durationMillis: Long,
) {
    private var sessionKey: String? = null
    private var expiresAtMillis: Long = 0L

    fun remainingMillis(sessionKey: String, nowMillis: Long): Long {
        if (this.sessionKey != sessionKey) {
            this.sessionKey = sessionKey
            expiresAtMillis = nowMillis + durationMillis
        }
        return (expiresAtMillis - nowMillis).coerceAtLeast(0L)
    }

    fun clear() {
        sessionKey = null
        expiresAtMillis = 0L
    }
}

internal data class MediaPlaybackUpdate(
    val sessionToken: MediaSession.Token?,
    val title: String,
    val artist: String?,
    val album: String?,
    val smallIcon: Icon? = null,
    val artwork: Bitmap? = null,
    val progressPercent: Int? = null,
    val contentIntent: PendingIntent? = null,
    val visibility: Int = Notification.VISIBILITY_PRIVATE,
    val canPlay: Boolean = false,
    val canPause: Boolean = false,
    val leftControl: MediaPlaybackControl? = null,
    val rightControl: MediaPlaybackControl? = null,
)

internal class MediaPlaybackMonitor(
    private val context: Context,
    private val listenerComponent: ComponentName,
    private val handler: Handler,
    private val onUpdate: (MediaPlaybackUpdate?) -> Unit,
) {
    private val mediaSessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val sourceNotifications = linkedMapOf<String, StatusBarNotification>()
    private val controllerCallbacks = linkedMapOf<MediaSession.Token, MediaController.Callback>()
    private var controllers: List<MediaController> = emptyList()
    private var lastPlayingToken: MediaSession.Token? = null
    private var pausedToken: MediaSession.Token? = null
    private val pauseGracePeriod = MediaPauseGracePeriod(PAUSE_GRACE_PERIOD_MILLIS)
    private var progressRefreshScheduled = false
    private var started = false

    private val pauseRemovalRunnable = Runnable {
        pausedToken = null
        reconcile()
    }
    private val progressRefreshRunnable = Runnable {
        progressRefreshScheduled = false
        if (started) reconcile()
    }

    private val activeSessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { activeControllers ->
            updateControllers(activeControllers.orEmpty())
            reconcile()
        }

    fun start(activeNotifications: Array<StatusBarNotification>) {
        if (started) return
        started = true
        sourceNotifications.clear()
        activeNotifications.forEach(::trackSourceNotification)
        runCatching {
            mediaSessionManager.addOnActiveSessionsChangedListener(
                activeSessionsChangedListener,
                listenerComponent,
                handler,
            )
            updateControllers(mediaSessionManager.getActiveSessions(listenerComponent))
        }.onFailure {
            updateControllers(emptyList())
        }
        reconcile()
    }

    fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        trackSourceNotification(statusBarNotification)
        if (started) reconcile()
    }

    fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        if (sourceNotifications.remove(statusBarNotification.key) != null && started) {
            reconcile()
        }
    }

    fun refresh() {
        if (started) reconcile()
    }

    fun stop() {
        if (started) {
            runCatching {
                mediaSessionManager.removeOnActiveSessionsChangedListener(
                    activeSessionsChangedListener,
                )
            }
        }
        started = false
        cancelPauseGracePeriod()
        cancelProgressRefresh()
        updateControllers(emptyList())
        sourceNotifications.clear()
        lastPlayingToken = null
        onUpdate(null)
    }

    private fun trackSourceNotification(statusBarNotification: StatusBarNotification) {
        if (
            statusBarNotification.packageName == context.packageName ||
            !statusBarNotification.notification.isMediaNotification()
        ) {
            return
        }
        sourceNotifications[statusBarNotification.key] = statusBarNotification
    }

    private fun updateControllers(activeControllers: List<MediaController>) {
        val activeTokens = activeControllers.mapTo(mutableSetOf()) { it.sessionToken }
        controllerCallbacks.keys.filterNot(activeTokens::contains).forEach { token ->
            val controller = controllers.firstOrNull { it.sessionToken == token }
            val callback = controllerCallbacks.remove(token)
            if (controller != null && callback != null) controller.unregisterCallback(callback)
        }
        activeControllers.forEach { controller ->
            if (controllerCallbacks.containsKey(controller.sessionToken)) return@forEach
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = reconcile()

                override fun onMetadataChanged(metadata: MediaMetadata?) = reconcile()

                override fun onSessionDestroyed() {
                    updateControllers(
                        controllers.filterNot { it.sessionToken == controller.sessionToken },
                    )
                    reconcile()
                }
            }
            controller.registerCallback(callback, handler)
            controllerCallbacks[controller.sessionToken] = callback
        }
        controllers = activeControllers
    }

    private fun reconcile() {
        if (!AppReminderPreferences.App.MEDIA_PLAYBACK.isEnabled(context)) {
            cancelPauseGracePeriod()
            cancelProgressRefresh()
            lastPlayingToken = null
            onUpdate(null)
            return
        }

        val matchedSources = controllers.associateWith(::findSourceNotification)
        val selected = MediaPlaybackPolicy.select(
            candidates = controllers.map { controller ->
                val source = matchedSources[controller]
                MediaPlaybackCandidate(
                    sessionKey = controller.sessionToken.toString(),
                    playbackState = controller.playbackState?.state ?: PlaybackState.STATE_NONE,
                    hasSourceNotification = source != null,
                    sourcePromoted = source?.notification?.isPromotedOngoing() == true,
                )
            },
            lastPlayingSessionKey = lastPlayingToken?.toString(),
        )
        if (selected == null) {
            val topOngoingController = controllers.firstOrNull { controller ->
                val source = matchedSources[controller]
                source != null && MediaPlaybackPolicy.isOngoingPlayback(
                    controller.playbackState?.state ?: PlaybackState.STATE_NONE,
                    controller.sessionToken == lastPlayingToken,
                )
            }
            if (topOngoingController != null) {
                cancelPauseGracePeriod()
                cancelProgressRefresh()
                if (topOngoingController.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    lastPlayingToken = topOngoingController.sessionToken
                }
                onUpdate(null)
                return
            }

            val pausedController = controllers.firstOrNull { controller ->
                val source = matchedSources[controller]
                source != null &&
                    !source.notification.isPromotedOngoing() &&
                    MediaPlaybackPolicy.canRetainPaused(
                        playbackState = controller.playbackState?.state
                            ?: PlaybackState.STATE_NONE,
                        sameSessionWasPlaying = controller.sessionToken == lastPlayingToken,
                    )
            }
            if (pausedController != null) {
                val remainingMillis = pauseGracePeriod.remainingMillis(
                    sessionKey = pausedController.sessionToken.toString(),
                    nowMillis = SystemClock.elapsedRealtime(),
                )
                if (remainingMillis > 0L) {
                    cancelProgressRefresh()
                    schedulePauseRemoval(pausedController.sessionToken, remainingMillis)
                    val source = requireNotNull(matchedSources[pausedController])
                    onUpdate(pausedController.toUpdate(source.notification))
                    return
                }
            }

            cancelPauseGracePeriod()
            cancelProgressRefresh()
            lastPlayingToken = null
            onUpdate(null)
            return
        }

        val controller = controllers.first { it.sessionToken.toString() == selected.sessionKey }
        val source = requireNotNull(matchedSources[controller])
        cancelPauseGracePeriod()
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            lastPlayingToken = controller.sessionToken
        }
        val update = controller.toUpdate(source.notification)
        scheduleProgressRefresh(
            controller.playbackState?.state == PlaybackState.STATE_PLAYING &&
                update.progressPercent != null,
        )
        onUpdate(update)
    }

    private fun findSourceNotification(controller: MediaController): StatusBarNotification? {
        val samePackage = sourceNotifications.values.filter {
            it.packageName == controller.packageName
        }
        return samePackage.firstOrNull {
            it.notification.mediaSessionToken() == controller.sessionToken
        } ?: samePackage.firstOrNull {
            it.notification.category == Notification.CATEGORY_TRANSPORT
        }
    }

    private fun MediaController.toUpdate(source: Notification): MediaPlaybackUpdate {
        val metadata = metadata
        val sourceTitle = source.extras.getCharSequence(Notification.EXTRA_TITLE)?.cleanText()
        val sourceText = source.extras.getCharSequence(Notification.EXTRA_TEXT)?.cleanText()
        val title = metadata?.firstText(
            MediaMetadata.METADATA_KEY_DISPLAY_TITLE,
            MediaMetadata.METADATA_KEY_TITLE,
        ) ?: sourceTitle ?: context.getString(R.string.media_playback_unknown_title)
        val artist = metadata?.firstText(
            MediaMetadata.METADATA_KEY_ARTIST,
            MediaMetadata.METADATA_KEY_ALBUM_ARTIST,
            MediaMetadata.METADATA_KEY_AUTHOR,
        ) ?: sourceText?.takeUnless { it == title }
        val album = metadata?.firstText(MediaMetadata.METADATA_KEY_ALBUM)
        val actions = playbackState?.actions ?: 0L
        val paused = playbackState?.state == PlaybackState.STATE_PAUSED
        val sideControls = MediaPlaybackControlPolicy.select(actions)
        val durationMillis = metadata
            ?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { it > 0L }
        val estimatedPositionMillis = playbackState?.let { state ->
            MediaPlaybackSeekPosition.calculate(
                positionMillis = state.position,
                lastPositionUpdateElapsedMillis = state.lastPositionUpdateTime,
                playbackSpeed = state.playbackSpeed,
                playbackState = state.state,
                nowElapsedMillis = SystemClock.elapsedRealtime(),
                offsetMillis = 0L,
                durationMillis = durationMillis,
            )
        }
        return MediaPlaybackUpdate(
            sessionToken = sessionToken,
            title = title,
            artist = artist,
            album = album,
            smallIcon = source.smallIcon?.takeIf { it.type == Icon.TYPE_RESOURCE },
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            progressPercent = MediaPlaybackProgress.percent(
                estimatedPositionMillis,
                durationMillis,
            ),
            contentIntent = source.contentIntent ?: sessionActivity,
            visibility = source.visibility,
            canPlay = paused && (
                actions supports PlaybackState.ACTION_PLAY ||
                    actions supports PlaybackState.ACTION_PLAY_PAUSE
                ),
            canPause = !paused && (
                actions supports PlaybackState.ACTION_PAUSE ||
                    actions supports PlaybackState.ACTION_PLAY_PAUSE
                ),
            leftControl = sideControls.left,
            rightControl = sideControls.right,
        )
    }

    private infix fun Long.supports(action: Long): Boolean = this and action != 0L

    private fun MediaMetadata.firstText(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { getText(it)?.cleanText() }

    private fun CharSequence.cleanText(): String? =
        toString().trim().takeIf(String::isNotEmpty)

    private fun schedulePauseRemoval(token: MediaSession.Token, delayMillis: Long) {
        if (pausedToken == token) return
        handler.removeCallbacks(pauseRemovalRunnable)
        pausedToken = token
        handler.postDelayed(pauseRemovalRunnable, delayMillis)
    }

    private fun cancelPauseGracePeriod() {
        handler.removeCallbacks(pauseRemovalRunnable)
        pausedToken = null
        pauseGracePeriod.clear()
    }

    private fun scheduleProgressRefresh(shouldSchedule: Boolean) {
        if (!shouldSchedule) {
            cancelProgressRefresh()
            return
        }
        if (progressRefreshScheduled) return
        progressRefreshScheduled = true
        handler.postDelayed(progressRefreshRunnable, PROGRESS_REFRESH_INTERVAL_MILLIS)
    }

    private fun cancelProgressRefresh() {
        handler.removeCallbacks(progressRefreshRunnable)
        progressRefreshScheduled = false
    }

    private companion object {
        const val PAUSE_GRACE_PERIOD_MILLIS = 60_000L
        const val PROGRESS_REFRESH_INTERVAL_MILLIS = 15_000L
    }
}

internal fun Notification.isMediaNotification(): Boolean =
    mediaSessionToken() != null || category == Notification.CATEGORY_TRANSPORT

internal fun Notification.mediaSessionToken(): MediaSession.Token? =
    extras.getParcelable(Notification.EXTRA_MEDIA_SESSION, MediaSession.Token::class.java)

internal fun Notification.isPromotedOngoing(): Boolean =
    flags and Notification.FLAG_PROMOTED_ONGOING != 0

class MediaPlaybackActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!AppReminderPreferences.App.MEDIA_PLAYBACK.isEnabled(context)) return
        val token = intent.getParcelableExtra(
            EXTRA_MEDIA_SESSION,
            MediaSession.Token::class.java,
        ) ?: return
        val controller = runCatching { MediaController(context, token) }.getOrNull() ?: return
        when (intent.action) {
            ACTION_PLAY -> controller.transportControls.play()
            ACTION_PAUSE -> controller.transportControls.pause()
            ACTION_SKIP_PREVIOUS -> controller.transportControls.skipToPrevious()
            ACTION_SKIP_NEXT -> controller.transportControls.skipToNext()
            ACTION_SEEK_BACK_15_SECONDS -> seekBy(controller, -SEEK_INTERVAL_MILLIS)
            ACTION_SEEK_FORWARD_15_SECONDS -> seekBy(controller, SEEK_INTERVAL_MILLIS)
            ACTION_REWIND -> controller.transportControls.rewind()
            ACTION_FAST_FORWARD -> controller.transportControls.fastForward()
        }
    }

    private fun seekBy(controller: MediaController, offsetMillis: Long) {
        val playbackState = controller.playbackState ?: return
        val durationMillis = controller.metadata
            ?.getLong(MediaMetadata.METADATA_KEY_DURATION)
            ?.takeIf { it > 0L }
        val targetPosition = MediaPlaybackSeekPosition.calculate(
            positionMillis = playbackState.position,
            lastPositionUpdateElapsedMillis = playbackState.lastPositionUpdateTime,
            playbackSpeed = playbackState.playbackSpeed,
            playbackState = playbackState.state,
            nowElapsedMillis = SystemClock.elapsedRealtime(),
            offsetMillis = offsetMillis,
            durationMillis = durationMillis,
        ) ?: return
        controller.transportControls.seekTo(targetPosition)
    }

    companion object {
        private const val ACTION_PLAY =
            "com.github.jimmy90109.livestatus.action.MEDIA_PLAY"
        private const val ACTION_PAUSE =
            "com.github.jimmy90109.livestatus.action.MEDIA_PAUSE"
        private const val ACTION_SKIP_PREVIOUS =
            "com.github.jimmy90109.livestatus.action.MEDIA_SKIP_PREVIOUS"
        private const val ACTION_SKIP_NEXT =
            "com.github.jimmy90109.livestatus.action.MEDIA_SKIP_NEXT"
        private const val ACTION_SEEK_BACK_15_SECONDS =
            "com.github.jimmy90109.livestatus.action.MEDIA_SEEK_BACK_15_SECONDS"
        private const val ACTION_SEEK_FORWARD_15_SECONDS =
            "com.github.jimmy90109.livestatus.action.MEDIA_SEEK_FORWARD_15_SECONDS"
        private const val ACTION_REWIND =
            "com.github.jimmy90109.livestatus.action.MEDIA_REWIND"
        private const val ACTION_FAST_FORWARD =
            "com.github.jimmy90109.livestatus.action.MEDIA_FAST_FORWARD"
        private const val EXTRA_MEDIA_SESSION = "media_session"
        private const val SEEK_INTERVAL_MILLIS = 15_000L

        fun playPendingIntent(context: Context, token: MediaSession.Token): PendingIntent =
            pendingIntent(context, ACTION_PLAY, token)

        fun pausePendingIntent(context: Context, token: MediaSession.Token): PendingIntent =
            pendingIntent(context, ACTION_PAUSE, token)

        internal fun controlPendingIntent(
            context: Context,
            token: MediaSession.Token,
            control: MediaPlaybackControl,
        ): PendingIntent = pendingIntent(
            context = context,
            action = when (control) {
                MediaPlaybackControl.SKIP_PREVIOUS -> ACTION_SKIP_PREVIOUS
                MediaPlaybackControl.SEEK_BACK_15_SECONDS -> ACTION_SEEK_BACK_15_SECONDS
                MediaPlaybackControl.REWIND -> ACTION_REWIND
                MediaPlaybackControl.SKIP_NEXT -> ACTION_SKIP_NEXT
                MediaPlaybackControl.SEEK_FORWARD_15_SECONDS -> ACTION_SEEK_FORWARD_15_SECONDS
                MediaPlaybackControl.FAST_FORWARD -> ACTION_FAST_FORWARD
            },
            token = token,
        )

        private fun pendingIntent(
            context: Context,
            action: String,
            token: MediaSession.Token,
        ): PendingIntent = PendingIntent.getBroadcast(
            context,
            action.hashCode(),
            Intent(context, MediaPlaybackActionReceiver::class.java)
                .setAction(action)
                .putExtra(EXTRA_MEDIA_SESSION, token),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }
}
