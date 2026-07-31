package com.github.jimmy90109.livestatus.ui.home

import android.os.SystemClock
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
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
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors


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
        title = null,
        description = null,
        supportedLanguages = listOf("繁中"),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.ipassContainer,
        labelColor = colors.ipassSecondaryContainer,
        foregroundColor = colors.onSurface,
    ) {
        Spacer(Modifier.height(4.dp))
        AppCardActionButton(
            "模擬上車，顯示提醒  ↑",
            colors.ipassPrimary,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_ipass_entered),
            enabled = enabled,
        ) {
            LiveStatusReminder.show(context)
        }
        AppCardActionButton(
            "模擬下車，移除提醒  ✓",
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
        title = null,
        description = null,
        supportedLanguages = listOf("繁中"),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.taiwanPayContainer,
        labelColor = colors.taiwanPaySecondaryContainer,
        foregroundColor = colors.onSurface,
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
    ) {
        AppWarningNotice(
            title = stringResource(R.string.clock_aod_update_warning_title),
            description = stringResource(R.string.clock_aod_update_warning_description),
        )
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
            "清除 Clock 倒數  ✓",
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


private const val IPASS_PACKAGE = "com.ipass.ipassmoney"
private const val TAIWAN_PAY_PACKAGE = "tw.com.twmp.twhcewallet"
private const val CLOCK_PACKAGE = ClockTimerNotificationExtractor.CLOCK_PACKAGE
