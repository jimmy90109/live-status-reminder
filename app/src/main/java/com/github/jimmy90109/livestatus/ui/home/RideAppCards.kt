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
internal fun UberRideCard(
    installed: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "Uber",
        appPackageName = UBER_PACKAGE,
        fallbackIconRes = R.drawable.ic_notification,
        title = "乘車進度",
        description = "接單後顯示上車點、車輛與 PIN；上車後改顯示下車點。",
        supportedLanguages = listOf("英文"),
        installed = installed,
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.commonContainer,
        labelColor = colors.commonSurface,
        foregroundColor = colors.onSurface,
    ) {
        UberRideTestButton(
            label = "模擬上車 ETA 與地點",
            supportingText = stringResource(R.string.monitoring_uber_pickup_en_route),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.PICKUP_EN_ROUTE,
                title = "Pick up in 14 min",
                pickupPoint = "Meet at Demo Transit Center",
            ),
            enabled = enabled,
        )
        UberRideTestButton(
            label = "模擬快抵達",
            supportingText = stringResource(R.string.monitoring_uber_pickup_nearby),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY,
                title = "Pick up in 2 min",
                plate = "ABC1234",
                vehicle = "Blue Toyota Prius",
                pin = "1234",
            ),
            enabled = enabled,
        )
        UberRideTestButton(
            label = "模擬已抵達",
            supportingText = stringResource(R.string.monitoring_uber_arrived),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.ARRIVED,
                title = "Driver arrived",
                plate = "ABC1234",
                vehicle = "Blue Toyota Prius",
                pin = "1234",
            ),
            enabled = enabled,
        )
        UberRideTestButton(
            label = "模擬前往目的地",
            supportingText = stringResource(R.string.monitoring_uber_on_trip),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.ON_TRIP,
                title = "Dropoff at 4:30 PM",
                dropoffPoint = "Demo Office Tower",
            ),
            enabled = enabled,
        )
        ActionButton(
            "模擬完成，清除狀態  ✓",
            colors.commonSurface,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_uber_ended),
        ) {
            LiveStatusReminder.clearUberRide(context)
        }
        if (BuildConfig.DEBUG) {
            ActionButton("查看通知 payload", colors.commonSurface, colors.onSurface) {
                onOpenDebug()
            }
        }
    }
}

@Composable
private fun UberRideTestButton(
    label: String,
    supportingText: String,
    update: LiveStatusNotificationParser.UberRideUpdate,
    enabled: Boolean,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val primary = update.event == LiveStatusNotificationParser.UberRideEvent.PICKUP_EN_ROUTE
    ActionButton(
        label,
        if (primary) colors.commonPrimary else colors.commonSurface,
        if (primary) colors.commonOnPrimary else colors.onSurface,
        supportingText = supportingText,
        enabled = enabled,
    ) {
        LiveStatusReminder.showUberRide(context, update)
    }
}


@Composable
internal fun PikminBloomCard(
    installed: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "Pikmin Bloom",
        appPackageName = PIKMIN_BLOOM_PACKAGE,
        fallbackIconRes = R.drawable.ic_pikmin_flower_notification,
        title = "種花背景提醒",
        description = "偵測到背景種花通知時，立刻升級成即時提醒。",
        supportedLanguages = listOf("中文"),
        installed = installed,
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.pikminContainer,
        labelColor = colors.pikminSecondaryContainer,
        foregroundColor = colors.pikminText,
    ) {
        ActionButton(
            "模擬種花中",
            colors.pikminPrimary,
            colors.commonOnPrimary,
            supportingText = stringResource(R.string.monitoring_pikmin_started),
            enabled = enabled,
        ) {
            LiveStatusReminder.showPikminBloom(context)
        }
        ActionButton(
            "清除 Pikmin Bloom 狀態  ✓",
            colors.pikminSecondaryContainer,
            colors.pikminText,
            supportingText = stringResource(R.string.monitoring_pikmin_stopped),
        ) {
            LiveStatusReminder.clearPikminBloom(context)
        }
    }
}


private const val UBER_PACKAGE = "com.ubercab"
private const val PIKMIN_BLOOM_PACKAGE = "com.nianticlabs.pikmin"
