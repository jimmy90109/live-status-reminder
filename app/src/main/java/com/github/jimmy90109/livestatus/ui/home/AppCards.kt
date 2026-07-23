package com.github.jimmy90109.livestatus.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.livestatus.AppReminderPreferences
import com.github.jimmy90109.livestatus.BuildConfig
import com.github.jimmy90109.livestatus.ClockTimerNotificationExtractor
import com.github.jimmy90109.livestatus.ClockTimerSource
import com.github.jimmy90109.livestatus.ClockTimerState
import com.github.jimmy90109.livestatus.ClockTimerUpdate
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors
import kotlinx.coroutines.launch


@Composable
internal fun IpassCard(
    installed: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "iPASS MONEY",
        appPackageName = IPASS_PACKAGE,
        fallbackIconRes = R.drawable.ic_notification,
        title = "乘車碼狀態",
        description = "進站後顯示乘車碼捷徑，準備下車時快速開啟。",
        supportedLanguages = listOf("繁中"),
        installed = installed,
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.ipassContainer,
        labelColor = colors.ipassSecondaryContainer,
        foregroundColor = colors.onSurface,
    ) {
        AppActionDivider(colors.onSurface)
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
internal fun ClockCard(
    installed: Boolean,
    enabled: Boolean,
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
        description = "將 Google 時鐘的主要倒數同步成即時通知；暫停時固定顯示剩餘時間。",
        supportedLanguages = listOf("不依賴語言"),
        installed = installed,
        enabled = enabled,
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
private const val CLOCK_PACKAGE = ClockTimerNotificationExtractor.CLOCK_PACKAGE
