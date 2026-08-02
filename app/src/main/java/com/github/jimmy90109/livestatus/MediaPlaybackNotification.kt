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
import android.service.notification.StatusBarNotification

internal data class MediaPlaybackCandidate(
    val sessionKey: String,
    val playbackState: Int,
    val hasSourceNotification: Boolean,
    val sourcePromoted: Boolean,
)

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
                    playbackState == PlaybackState.STATE_CONNECTING
                )
}

internal data class MediaPlaybackUpdate(
    val sessionToken: MediaSession.Token?,
    val packageName: String,
    val appName: String,
    val title: String,
    val artist: String?,
    val smallIcon: Icon? = null,
    val artwork: Bitmap? = null,
    val contentIntent: PendingIntent? = null,
    val visibility: Int = Notification.VISIBILITY_PRIVATE,
    val canPause: Boolean = false,
    val canSkipPrevious: Boolean = false,
    val canSkipNext: Boolean = false,
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
    private var started = false

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
            if (topOngoingController == null) lastPlayingToken = null
            onUpdate(null)
            return
        }

        val controller = controllers.first { it.sessionToken.toString() == selected.sessionKey }
        val source = requireNotNull(matchedSources[controller])
        if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
            lastPlayingToken = controller.sessionToken
        }
        onUpdate(controller.toUpdate(source.notification))
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
        val actions = playbackState?.actions ?: 0L
        return MediaPlaybackUpdate(
            sessionToken = sessionToken,
            packageName = packageName,
            appName = packageName.applicationLabel(context),
            title = title,
            artist = artist,
            smallIcon = source.smallIcon?.takeIf { it.type == Icon.TYPE_RESOURCE },
            artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART),
            contentIntent = source.contentIntent ?: sessionActivity,
            visibility = source.visibility,
            canPause = actions supports PlaybackState.ACTION_PAUSE ||
                actions supports PlaybackState.ACTION_PLAY_PAUSE,
            canSkipPrevious = actions supports PlaybackState.ACTION_SKIP_TO_PREVIOUS,
            canSkipNext = actions supports PlaybackState.ACTION_SKIP_TO_NEXT,
        )
    }

    private infix fun Long.supports(action: Long): Boolean = this and action != 0L

    private fun MediaMetadata.firstText(vararg keys: String): String? =
        keys.firstNotNullOfOrNull { getText(it)?.cleanText() }

    private fun CharSequence.cleanText(): String? =
        toString().trim().takeIf(String::isNotEmpty)

    private fun String.applicationLabel(context: Context): String = runCatching {
        val applicationInfo = context.packageManager.getApplicationInfo(this, 0)
        context.packageManager.getApplicationLabel(applicationInfo).toString()
    }.getOrDefault(substringAfterLast('.').ifBlank {
        context.getString(R.string.media_playback_generic_app_name)
    })
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
            ACTION_PAUSE -> {
                controller.transportControls.pause()
                LiveStatusReminder.clearMediaPlayback(context)
            }
            ACTION_SKIP_PREVIOUS -> controller.transportControls.skipToPrevious()
            ACTION_SKIP_NEXT -> controller.transportControls.skipToNext()
        }
    }

    companion object {
        private const val ACTION_PAUSE =
            "com.github.jimmy90109.livestatus.action.MEDIA_PAUSE"
        private const val ACTION_SKIP_PREVIOUS =
            "com.github.jimmy90109.livestatus.action.MEDIA_SKIP_PREVIOUS"
        private const val ACTION_SKIP_NEXT =
            "com.github.jimmy90109.livestatus.action.MEDIA_SKIP_NEXT"
        private const val EXTRA_MEDIA_SESSION = "media_session"

        fun pausePendingIntent(context: Context, token: MediaSession.Token): PendingIntent =
            pendingIntent(context, ACTION_PAUSE, token)

        fun previousPendingIntent(context: Context, token: MediaSession.Token): PendingIntent =
            pendingIntent(context, ACTION_SKIP_PREVIOUS, token)

        fun nextPendingIntent(context: Context, token: MediaSession.Token): PendingIntent =
            pendingIntent(context, ACTION_SKIP_NEXT, token)

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
