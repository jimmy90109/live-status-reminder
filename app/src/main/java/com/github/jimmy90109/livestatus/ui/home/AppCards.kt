package com.github.jimmy90109.livestatus.ui.home

import android.app.Notification
import android.app.PendingIntent
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.SystemClock
import android.provider.Settings
import androidx.core.net.toUri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.livestatus.BuildConfig
import com.github.jimmy90109.livestatus.ClockTimerNotificationExtractor
import com.github.jimmy90109.livestatus.ClockTimerSource
import com.github.jimmy90109.livestatus.ClockTimerState
import com.github.jimmy90109.livestatus.ClockTimerUpdate
import com.github.jimmy90109.livestatus.DiscordVoiceUpdate
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.HevyWorkoutPhase
import com.github.jimmy90109.livestatus.HevyWorkoutNotificationParser
import com.github.jimmy90109.livestatus.HevyWorkoutUpdate
import com.github.jimmy90109.livestatus.GoogleRecorderNotificationParser
import com.github.jimmy90109.livestatus.RecorderState
import com.github.jimmy90109.livestatus.RecorderUpdate
import com.github.jimmy90109.livestatus.StravaRecordingState
import com.github.jimmy90109.livestatus.StravaRecordingLanguage
import com.github.jimmy90109.livestatus.StravaRecordingUpdate
import com.github.jimmy90109.livestatus.TeamsCallUpdate
import com.github.jimmy90109.livestatus.TeamsCallLanguage
import com.github.jimmy90109.livestatus.YouBikeNotificationParser
import com.github.jimmy90109.livestatus.YouBikeEvent
import com.github.jimmy90109.livestatus.YouBikeRideManager
import com.github.jimmy90109.livestatus.YouBikeRideSessionStore
import com.github.jimmy90109.livestatus.YouBikeRideUpdate
import com.github.jimmy90109.livestatus.YptStudyNotificationParser
import com.github.jimmy90109.livestatus.YptStudyUpdate
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

@Composable
internal fun GoogleRecorderCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = stringResource(R.string.google_recorder_card_app_name),
        appPackageName = GoogleRecorderNotificationParser.PACKAGE_NAME,
        fallbackIconRes = R.drawable.ic_microphone_notification,
        title = stringResource(R.string.google_recorder_card_title),
        description = stringResource(R.string.google_recorder_card_description),
        supportedLanguages = listOf(
            stringResource(R.string.google_recorder_language),
        ),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.recorderContainer,
        labelColor = colors.recorderSecondaryContainer,
        foregroundColor = colors.recorderText,
        actionColor = colors.recorderPrimary,
    ) {
        AppActionDivider(colors.recorderText)
        AppCardActionButton(
            stringResource(R.string.google_recorder_simulate_running),
            colors.recorderPrimary,
            colors.recorderText,
            supportingText = stringResource(R.string.monitoring_google_recorder_running),
            enabled = enabled,
        ) {
            val now = System.currentTimeMillis()
            LiveStatusReminder.showGoogleRecorder(
                context,
                RecorderUpdate(
                    sourceKey = "debug-google-recorder",
                    state = RecorderState.RUNNING,
                    elapsedMillis = 7_000L,
                    startedAtEpochMillis = now - 7_000L,
                    postedAtEpochMillis = now,
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.google_recorder_simulate_paused),
            colors.recorderPrimary,
            colors.recorderText,
            supportingText = stringResource(R.string.monitoring_google_recorder_paused),
            enabled = enabled,
        ) {
            LiveStatusReminder.showGoogleRecorder(
                context,
                RecorderUpdate(
                    sourceKey = "debug-google-recorder",
                    state = RecorderState.PAUSED,
                    elapsedMillis = 7_000L,
                    startedAtEpochMillis = null,
                    postedAtEpochMillis = System.currentTimeMillis(),
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.google_recorder_simulate_clear),
            colors.recorderPrimary,
            colors.recorderText,
            supportingText = stringResource(R.string.monitoring_google_recorder_stopped),
        ) {
            LiveStatusReminder.clearGoogleRecorder(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.google_recorder_debug_open_payload),
                colors.recorderPrimary,
                colors.recorderText,
                enabled = installed,
                onClick = onOpenDebug,
            )
        }
    }
}

@Composable
internal fun MediaPlaybackCard(
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = stringResource(R.string.media_playback_card_app_name),
        appPackageName = context.packageName,
        fallbackIconRes = R.drawable.ic_music_notification,
        title = stringResource(R.string.media_playback_card_title),
        description = stringResource(R.string.media_playback_card_description),
        supportedLanguages = listOf(stringResource(R.string.media_playback_language_independent)),
        installed = true,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        usePackageIcon = false,
        cardColor = colors.commonContainer,
        labelColor = colors.commonSurface,
        foregroundColor = colors.onSurface,
        notices = {
            AppWarningNotice(
                title = stringResource(R.string.media_playback_limitation_title),
                description = stringResource(R.string.media_playback_limitation_description),
            )
        },
    )
}

@Composable
internal fun DiscordVoiceCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "Discord",
        appPackageName = DISCORD_PACKAGE,
        fallbackIconRes = R.drawable.ic_voice_notification,
        title = stringResource(R.string.discord_voice_card_title),
        description = stringResource(R.string.discord_voice_card_description),
        supportedLanguages = listOf(stringResource(R.string.media_playback_language_independent)),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.discordContainer,
        labelColor = colors.discordSecondaryContainer,
        foregroundColor = colors.discordText,
        actionColor = colors.discordPrimary,
    ) {
        AppActionDivider(colors.discordText)
        AppCardActionButton(
            stringResource(R.string.discord_voice_simulate_connected),
            colors.discordPrimary,
            colors.discordText,
            supportingText = stringResource(R.string.discord_voice_monitoring_connected),
            enabled = enabled,
        ) {
            LiveStatusReminder.showDiscordVoice(
                context,
                DiscordVoiceUpdate(
                    sourceKey = "debug-discord-voice",
                    postedAtEpochMillis = System.currentTimeMillis(),
                    sourceTitle = "語音已連線 — 點選即可回到通話",
                    sourceContentText = "[測試] 語音頻道",
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.discord_voice_simulate_clear),
            colors.discordPrimary,
            colors.discordText,
            supportingText = stringResource(R.string.discord_voice_monitoring_disconnected),
        ) {
            LiveStatusReminder.clearDiscordVoice(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.discord_debug_open_payload),
                colors.discordPrimary,
                colors.discordText,
                enabled = installed,
                onClick = onOpenDebug,
            )
        }
    }
}

@Composable
internal fun TeamsCallCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val openTeams = PendingIntent.getActivity(
        context,
        12,
        HomeScreenHostActivity.createOpenTeamsIntent(context),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
    fun teamsAction(title: String): Notification.Action = Notification.Action.Builder(
        Icon.createWithResource(context, R.drawable.ic_voice_notification),
        title,
        openTeams,
    ).build()
    AppCard(
        appName = stringResource(R.string.teams_card_app_name),
        appPackageName = TEAMS_PACKAGE,
        fallbackIconRes = R.drawable.ic_voice_notification,
        title = stringResource(R.string.teams_card_title),
        description = stringResource(R.string.teams_card_description),
        supportedLanguages = listOf(
            stringResource(R.string.media_playback_language_independent),
        ),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.teamsContainer,
        labelColor = colors.teamsSecondaryContainer,
        foregroundColor = colors.teamsText,
        actionColor = colors.teamsPrimary,
    ) {
        AppActionDivider(colors.teamsText)
        AppCardActionButton(
            stringResource(R.string.teams_simulate_active),
            colors.teamsPrimary,
            colors.teamsText,
            supportingText = stringResource(R.string.teams_monitoring_active),
            enabled = enabled,
        ) {
            LiveStatusReminder.showTeamsCall(
                context,
                TeamsCallUpdate(
                    sourceKey = "debug-teams-call",
                    startedAtEpochMillis = System.currentTimeMillis() - 90_000L,
                    sourceTitle = "Meeting with Demo User",
                    participantName = "Demo User",
                    language = TeamsCallLanguage.ENGLISH,
                    contentIntent = openTeams,
                    sourceActions = listOf(teamsAction("靜音"), teamsAction("掛斷")),
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.teams_simulate_muted),
            colors.teamsPrimary,
            colors.teamsText,
            supportingText = stringResource(R.string.teams_monitoring_muted),
            enabled = enabled,
        ) {
            LiveStatusReminder.showTeamsCall(
                context,
                TeamsCallUpdate(
                    sourceKey = "debug-teams-call-muted",
                    startedAtEpochMillis = System.currentTimeMillis() - 90_000L,
                    sourceTitle = "Meeting with Demo User",
                    participantName = "Demo User",
                    language = TeamsCallLanguage.ENGLISH,
                    contentIntent = openTeams,
                    sourceActions = listOf(teamsAction("啟用通知"), teamsAction("掛斷")),
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.teams_simulate_clear),
            colors.teamsPrimary,
            colors.teamsText,
            supportingText = stringResource(R.string.teams_monitoring_ended),
        ) {
            LiveStatusReminder.clearTeamsCall(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.teams_debug_open_payload),
                colors.teamsPrimary,
                colors.teamsText,
                enabled = installed,
                onClick = onOpenDebug,
            )
        }
    }
}

@Composable
internal fun YptCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = stringResource(R.string.ypt_card_app_name),
        appPackageName = YPT_PACKAGE,
        fallbackIconRes = R.drawable.ic_timer_notification,
        title = stringResource(R.string.ypt_card_title),
        description = stringResource(R.string.ypt_card_description),
        supportedLanguages = listOf(stringResource(R.string.media_playback_language_independent)),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.yptContainer,
        labelColor = colors.yptSecondaryContainer,
        foregroundColor = colors.yptText,
        actionColor = colors.yptPrimary,
    ) {
        AppActionDivider(colors.yptText)
        AppCardActionButton(
            stringResource(R.string.ypt_simulate_start),
            colors.yptPrimary,
            colors.yptText,
            supportingText = stringResource(R.string.monitoring_ypt_running),
            enabled = enabled,
        ) {
            LiveStatusReminder.showYptStudy(
                context,
                YptStudyUpdate(
                    sourceKey = "debug-ypt-study",
                    startedAtEpochMillis = System.currentTimeMillis() - 10 * 60_000L,
                    sourceContentText = "YPT - Study Group",
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.ypt_simulate_stop),
            colors.yptPrimary,
            colors.yptText,
            supportingText = stringResource(R.string.monitoring_ypt_stopped),
        ) {
            LiveStatusReminder.clearYptStudy(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.ypt_debug_open_payload),
                colors.yptPrimary,
                colors.yptText,
                onClick = onOpenDebug,
            )
        }
    }
}

@Composable
internal fun HevyCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = stringResource(R.string.hevy_card_app_name),
        appPackageName = HEVY_PACKAGE,
        fallbackIconRes = R.drawable.ic_fitness_notification,
        title = stringResource(R.string.hevy_card_title),
        description = stringResource(R.string.hevy_card_description),
        supportedLanguages = listOf(stringResource(R.string.media_playback_language_independent)),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.commonContainer,
        labelColor = colors.commonSurface,
        foregroundColor = colors.onSurface,
        actionColor = colors.commonPrimary,
    ) {
        AppActionDivider(colors.onSurface)
        AppCardActionButton(
            stringResource(R.string.hevy_simulate_active_set),
            colors.commonPrimary,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_hevy_active_set),
            enabled = enabled,
        ) {
            LiveStatusReminder.showHevyWorkout(
                context,
                HevyWorkoutUpdate(
                    sourceKey = "debug-hevy-workout",
                    startedAtEpochMillis = System.currentTimeMillis() - 3 * 60_000L,
                    exerciseName = "肩推（啞鈴）",
                    phase = HevyWorkoutPhase.ACTIVE_SET,
                    setNumber = 4,
                    totalSets = 4,
                    setDetail = "7.5 kg × 12 次",
                    sourceContentText = "第 4/4 組 - 7.5 kg x 12 次",
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.hevy_simulate_rest),
            colors.commonPrimary,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_hevy_rest),
            enabled = enabled,
        ) {
            LiveStatusReminder.showHevyWorkout(
                context,
                HevyWorkoutUpdate(
                    sourceKey = "debug-hevy-workout",
                    startedAtEpochMillis = System.currentTimeMillis() - 4 * 60_000L,
                    exerciseName = "俯身飛鳥（啞鈴）",
                    phase = HevyWorkoutPhase.REST,
                    setNumber = 1,
                    totalSets = 4,
                    setDetail = "2.5 kg × 12 次",
                    sourceContentText = "下一個: 第1 組（共 4組） (2.5 kg x 12 次)\n休息 0:45",
                    restRemainingSeconds = 45,
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.hevy_simulate_finish),
            colors.commonPrimary,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_hevy_finished),
        ) {
            LiveStatusReminder.clearHevyWorkout(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.hevy_debug_open_payload),
                colors.commonPrimary,
                colors.onSurface,
                onClick = onOpenDebug,
            )
        }
    }
}

@Composable
internal fun StravaCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = stringResource(R.string.strava_card_app_name),
        appPackageName = STRAVA_PACKAGE,
        fallbackIconRes = R.drawable.ic_running_notification,
        title = stringResource(R.string.strava_card_title),
        description = stringResource(R.string.strava_card_description),
        supportedLanguages = listOf("繁中", "English"),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.stravaContainer,
        labelColor = colors.stravaSecondaryContainer,
        foregroundColor = colors.stravaText,
        actionColor = colors.stravaPrimary,
    ) {
        AppActionDivider(colors.stravaText)
        AppCardActionButton(
            stringResource(R.string.strava_simulate_recording),
            colors.stravaPrimary,
            colors.stravaText,
            supportingText = stringResource(R.string.monitoring_strava_recording),
            enabled = enabled,
        ) {
            LiveStatusReminder.showStravaRecording(
                context,
                StravaRecordingUpdate(
                    sourceKey = "debug-strava-recording",
                    state = StravaRecordingState.RECORDING,
                    language = StravaRecordingLanguage.TRADITIONAL_CHINESE,
                    officialTitle = "跑步 · 0:53 · 0 公里",
                    officialText = null,
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.strava_simulate_paused),
            colors.stravaPrimary,
            colors.stravaText,
            supportingText = stringResource(R.string.monitoring_strava_paused),
            enabled = enabled,
        ) {
            LiveStatusReminder.showStravaRecording(
                context,
                StravaRecordingUpdate(
                    sourceKey = "debug-strava-recording",
                    state = StravaRecordingState.PAUSED,
                    language = StravaRecordingLanguage.TRADITIONAL_CHINESE,
                    officialTitle = "跑步 · 0:53 · 0 公里",
                    officialText = "已停止",
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.strava_simulate_finish),
            colors.stravaPrimary,
            colors.stravaText,
            supportingText = stringResource(R.string.monitoring_strava_finished),
        ) {
            LiveStatusReminder.clearStravaRecording(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.strava_debug_open_payload),
                colors.stravaPrimary,
                colors.stravaText,
                onClick = onOpenDebug,
            )
        }
    }
}


@Composable
internal fun IpassCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "iPASS MONEY",
        appPackageName = IPASS_PACKAGE,
        fallbackIconRes = R.drawable.ic_notification,
        title = stringResource(R.string.transit_code_card_title),
        description = stringResource(R.string.transit_code_status_description),
        supportedLanguages = listOf("繁中"),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.ipassContainer,
        labelColor = colors.ipassSecondaryContainer,
        foregroundColor = colors.onSurface,
        actionColor = colors.ipassPrimary,
    ) {
        Spacer(Modifier.height(4.dp))
        AppCardActionButton(
            "模擬上車，顯示提醒",
            colors.ipassPrimary,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_ipass_entered),
            enabled = enabled,
        ) {
            LiveStatusReminder.show(context)
        }
        AppCardActionButton(
            "模擬下車，移除提醒",
            colors.ipassPrimary,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_ipass_exited),
        ) {
            LiveStatusReminder.clear(context)
        }
    }
}

@Composable
internal fun TaiwanPayCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "台灣 Pay",
        appPackageName = TAIWAN_PAY_PACKAGE,
        fallbackIconRes = R.drawable.ic_notification,
        title = stringResource(R.string.transit_code_card_title),
        description = stringResource(R.string.transit_code_status_description),
        supportedLanguages = listOf("繁中"),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.taiwanPayContainer,
        labelColor = colors.taiwanPaySecondaryContainer,
        foregroundColor = colors.onSurface,
        actionColor = colors.taiwanPayPrimary,
    ) {
        Spacer(Modifier.height(4.dp))
        AppCardActionButton(
            stringResource(R.string.taiwan_pay_simulate_boarding),
            colors.taiwanPayPrimary,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_taiwan_pay_boarding),
            enabled = enabled,
        ) {
            LiveStatusReminder.showTaiwanPay(context)
        }
        AppCardActionButton(
            stringResource(R.string.taiwan_pay_simulate_alighting),
            colors.taiwanPayPrimary,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_taiwan_pay_alighting),
        ) {
            LiveStatusReminder.clearTaiwanPay(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.taiwan_pay_debug_open_payload),
                colors.taiwanPayPrimary,
                colors.onSurface,
            ) {
                onOpenDebug()
            }
        }
    }
}

@Composable
internal fun ClockCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "Clock",
        appPackageName = CLOCK_PACKAGE,
        fallbackIconRes = R.drawable.ic_timer_notification,
        title = "倒數計時",
        description = "Google 時鐘原生即時通知未生效時，將主要倒數同步成即時通知；暫停時固定顯示剩餘時間。",
        supportedLanguages = listOf("不依賴語言"),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.clockContainer,
        labelColor = colors.clockSurface,
        foregroundColor = colors.clockText,
        actionColor = colors.clockPrimary,
        notices = {
            AppWarningNotice(
                title = stringResource(R.string.clock_aod_update_warning_title),
                description = stringResource(R.string.clock_aod_update_warning_description),
            )
        },
    ) {
        AppActionDivider(colors.clockText)
        AppCardActionButton(
            "模擬 22 分鐘倒數",
            colors.clockPrimary,
            colors.clockText,
            supportingText = stringResource(R.string.monitoring_clock_running),
            enabled = enabled,
        ) {
            LiveStatusReminder.showClockTimer(
                context,
                ClockTimerUpdate(
                    sourceKey = "debug-clock-running",
                    state = ClockTimerState.RUNNING,
                    endElapsedRealtimeMillis = SystemClock.elapsedRealtime() + 22 * 60_000L,
                    source = ClockTimerSource.METRIC_STYLE,
                ),
            )
        }
        AppCardActionButton(
            "模擬暫停於 12:34",
            colors.clockPrimary,
            colors.clockText,
            supportingText = stringResource(R.string.monitoring_clock_paused),
            enabled = enabled,
        ) {
            LiveStatusReminder.showClockTimer(
                context,
                ClockTimerUpdate(
                    sourceKey = "debug-clock-paused",
                    state = ClockTimerState.PAUSED,
                    remainingMillis = 12 * 60_000L + 34_000L,
                    source = ClockTimerSource.METRIC_STYLE,
                ),
            )
        }
        AppCardActionButton(
            "清除 Clock 倒數",
            colors.clockPrimary,
            colors.clockText,
            supportingText = stringResource(R.string.monitoring_clock_ended),
        ) {
            LiveStatusReminder.clearClockTimer(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton("查看通知 payload", colors.clockPrimary, colors.clockText) {
                onOpenDebug()
            }
        }
    }
}

@Composable
internal fun YouBikeCard(
    installed: Boolean,
    enabled: Boolean,
    exactAlarmAllowed: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "YouBike",
        appPackageName = YOU_BIKE_PACKAGE,
        fallbackIconRes = R.drawable.ic_bicycle_notification,
        title = stringResource(R.string.you_bike_tracking_title),
        description = stringResource(R.string.you_bike_tracking_description),
        supportedLanguages = listOf("繁中"),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.youBikeContainer,
        labelColor = colors.youBikeSecondaryContainer,
        foregroundColor = colors.youBikeText,
        actionColor = colors.youBikePrimary,
        notices = if (enabled && !exactAlarmAllowed) {
            {
                AppWarningNotice(
                    title = stringResource(R.string.you_bike_exact_alarm_disabled_title),
                    description = stringResource(R.string.you_bike_exact_alarm_disabled_description),
                    outlineColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.error,
                    action = {
                        ActionButton(
                            label = stringResource(R.string.you_bike_exact_alarm_allow_action),
                            background = MaterialTheme.colorScheme.error,
                            foreground = MaterialTheme.colorScheme.onError,
                        ) {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    "package:${context.packageName}".toUri(),
                                ),
                            )
                        }
                    },
                )
            }
        } else {
            null
        }
    ) {
        AppActionDivider(colors.youBikeText)
        AppCardActionButton(
            stringResource(R.string.you_bike_simulate_borrow),
            colors.youBikePrimary,
            colors.youBikeText,
            supportingText = stringResource(R.string.monitoring_you_bike_borrowed),
            enabled = enabled,
        ) {
            YouBikeRideManager.handle(
                context,
                YouBikeNotificationParser.parse(
                    "借車成功！您於${youBikeTestTimestamp()}在龍江錦州街口 09車柱,使用掃碼-信用卡(20) 0000,租借車號0102751。",
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.you_bike_simulate_electric_borrow),
            colors.youBikePrimary,
            colors.youBikeText,
            supportingText = stringResource(R.string.monitoring_you_bike_borrowed),
            enabled = enabled,
        ) {
            YouBikeRideManager.handle(
                context,
                YouBikeNotificationParser.parse(
                    "借車成功！您於${youBikeTestTimestamp()}在龍江錦州街口 09車柱,使用掃碼-信用卡(20) 0000,租借車號0162898。",
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.you_bike_simulate_unknown_borrow),
            colors.youBikePrimary,
            colors.youBikeText,
            supportingText = stringResource(R.string.monitoring_you_bike_borrowed),
            enabled = enabled,
        ) {
            YouBikeRideManager.handle(
                context,
                YouBikeNotificationParser.parse(
                    "借車成功！您於${youBikeTestTimestamp()}在測試未知站 01車柱,使用掃碼 0000,租借車號TEST200。",
                ),
            )
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.you_bike_simulate_near_boundary),
                colors.youBikePrimary,
                colors.youBikeText,
                supportingText = stringResource(R.string.you_bike_simulate_near_boundary_description),
                enabled = enabled,
            ) {
                YouBikeRideManager.handle(
                    context,
                    YouBikeNotificationParser.parse(
                        "借車成功！您於${youBikeTestTimestamp(29 * 60L + 50L)}在龍江錦州街口 09車柱,使用掃碼 0000,租借車號TEST030。",
                    ),
                )
            }
        }
        AppCardActionButton(
            stringResource(R.string.you_bike_simulate_return),
            colors.youBikePrimary,
            colors.youBikeText,
            supportingText = stringResource(R.string.monitoring_you_bike_returned),
        ) {
            val session = YouBikeRideSessionStore.load(context)
            if (session == null) {
                YouBikeRideManager.clear(context)
            } else {
                YouBikeRideManager.handle(
                    context,
                    YouBikeRideUpdate(
                        event = YouBikeEvent.RETURNED,
                        occurredAt = java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Taipei")),
                        bikeNumber = session.bikeNumber,
                    ),
                )
            }
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.you_bike_debug_open_payload),
                colors.youBikePrimary,
                colors.youBikeText,
                onClick = onOpenDebug,
            )
        }
    }
}


private const val IPASS_PACKAGE = "com.ipass.ipassmoney"
private const val TAIWAN_PAY_PACKAGE = "tw.com.twmp.twhcewallet"
private const val CLOCK_PACKAGE = ClockTimerNotificationExtractor.CLOCK_PACKAGE
private const val YOU_BIKE_PACKAGE = "tw.com.youbike.plus"
private const val YPT_PACKAGE = YptStudyNotificationParser.PACKAGE_NAME
private const val HEVY_PACKAGE = HevyWorkoutNotificationParser.PACKAGE_NAME
private const val STRAVA_PACKAGE = "com.strava"
private const val DISCORD_PACKAGE = "com.discord"
private const val TEAMS_PACKAGE = "com.microsoft.teams"

private fun youBikeTestTimestamp(secondsAgo: Long = 0): String =
    java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Taipei"))
        .minusSeconds(secondsAgo)
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
