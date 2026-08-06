package com.github.jimmy90109.livestatus.ui.home

import android.content.Intent
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
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.YouBikeNotificationParser
import com.github.jimmy90109.livestatus.YouBikeEvent
import com.github.jimmy90109.livestatus.YouBikeRideManager
import com.github.jimmy90109.livestatus.YouBikeRideSessionStore
import com.github.jimmy90109.livestatus.YouBikeRideUpdate
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors


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

private fun youBikeTestTimestamp(secondsAgo: Long = 0): String =
    java.time.LocalDateTime.now(java.time.ZoneId.of("Asia/Taipei"))
        .minusSeconds(secondsAgo)
        .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
