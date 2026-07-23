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
internal fun FoodpandaCard(
    installed: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "foodpanda",
        appPackageName = FOODPANDA_PACKAGE,
        fallbackIconRes = R.drawable.ic_food_delivery_notification,
        title = "外送訂單狀態",
        description = "外送夥伴出發或即將抵達時，顯示取餐提醒。",
        supportedLanguages = listOf("繁中"),
        installed = installed,
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.foodpandaContainer,
        labelColor = colors.foodpandaSecondaryContainer,
        foregroundColor = colors.foodpandaText,
    ) {
        AppWarningNotice(
            title = stringResource(R.string.platform_special_status_warning_title),
            description = stringResource(R.string.platform_special_status_warning_description),
        )
        AppActionDivider(colors.foodpandaText)
        AppCardActionButton(
            "模擬外送中",
            colors.foodpandaPrimary,
            colors.foodpandaText,
            supportingText = stringResource(R.string.monitoring_foodpanda_on_the_way),
            enabled = enabled,
        ) {
            LiveStatusReminder.showFoodpanda(context, LiveStatusNotificationParser.FoodpandaEvent.COURIER_ON_THE_WAY)
        }
        AppCardActionButton(
            "模擬即將抵達",
            colors.foodpandaPrimary,
            colors.foodpandaText,
            supportingText = stringResource(R.string.monitoring_foodpanda_arriving),
            enabled = enabled,
        ) {
            LiveStatusReminder.showFoodpanda(context, LiveStatusNotificationParser.FoodpandaEvent.COURIER_ARRIVING)
        }
        AppCardActionButton(
            "清除 foodpanda 狀態  ✓",
            colors.foodpandaPrimary,
            colors.foodpandaText,
            supportingText = stringResource(R.string.monitoring_foodpanda_ended),
        ) {
            LiveStatusReminder.clearFoodpanda(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton("查看通知 payload", colors.foodpandaPrimary, colors.foodpandaText) {
                onOpenDebug()
            }
        }
    }
}


@Composable
internal fun UberEatsCard(
    installed: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "Uber Eats",
        appPackageName = UBER_EATS_PACKAGE,
        fallbackIconRes = R.drawable.ic_food_delivery_notification,
        title = "五階段訂單進度",
        description = "從接單到即將抵達持續更新；可辨識時也會顯示四位數 PIN。",
        supportedLanguages = listOf("繁中"),
        installed = installed,
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.uberEatsContainer,
        labelColor = colors.uberEatsSecondaryContainer,
        foregroundColor = colors.uberEatsText,
    ) {
        AppWarningNotice(
            title = stringResource(R.string.platform_special_status_warning_title),
            description = stringResource(R.string.platform_special_status_warning_description),
        )
        AppActionDivider(colors.uberEatsText)
        UberEatsTestButton(
            label = "模擬訂單已收到",
            supportingText = stringResource(R.string.monitoring_uber_eats_received),
            event = LiveStatusNotificationParser.UberEatsEvent.ORDER_RECEIVED,
            officialTitle = "訂單已收到",
            officialText = "抵達時間：12:00-12:10 PM",
            enabled = enabled,
        )
        UberEatsTestButton(
            label = "模擬正在準備訂單",
            supportingText = stringResource(R.string.monitoring_uber_eats_preparing),
            event = LiveStatusNotificationParser.UberEatsEvent.PREPARING,
            officialTitle = "正在準備訂單",
            officialText = "抵達時間：12:00-12:10 PM",
            enabled = enabled,
        )
        UberEatsTestButton(
            label = "模擬正在取餐",
            supportingText = stringResource(R.string.monitoring_uber_eats_picking_up),
            event = LiveStatusNotificationParser.UberEatsEvent.PICKING_UP,
            officialTitle = "正在取餐",
            officialText = "小明 · ABC-1234 · 抵達時間：12:00-12:10 PM",
            enabled = enabled,
        )
        UberEatsTestButton(
            label = "模擬配送中",
            supportingText = stringResource(R.string.monitoring_uber_eats_on_the_way),
            event = LiveStatusNotificationParser.UberEatsEvent.ON_THE_WAY,
            officialTitle = "正前往您所在位置",
            officialText = "小明 · ABC-1234 · 抵達時間為 12:00 PM",
            enabled = enabled,
        )
        UberEatsTestButton(
            label = "模擬快到了",
            supportingText = stringResource(R.string.monitoring_uber_eats_arriving),
            event = LiveStatusNotificationParser.UberEatsEvent.ARRIVING,
            officialTitle = "快到了！",
            officialText = "小明 · ABC-1234 · 即將抵達",
            enabled = enabled,
        )
        AppCardActionButton(
            "模擬送達，清除狀態  ✓",
            colors.uberEatsPrimary,
            colors.uberEatsText,
            supportingText = stringResource(R.string.monitoring_uber_eats_ended),
        ) {
            LiveStatusReminder.clearUberEats(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton("查看通知 payload", colors.uberEatsPrimary, colors.uberEatsText) {
                onOpenDebug()
            }
        }
    }
}

@Composable
private fun UberEatsTestButton(
    label: String,
    supportingText: String,
    event: LiveStatusNotificationParser.UberEatsEvent,
    officialTitle: String,
    officialText: String,
    enabled: Boolean,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCardActionButton(
        label,
        colors.uberEatsPrimary,
        colors.uberEatsText,
        supportingText = supportingText,
        enabled = enabled,
    ) {
        LiveStatusReminder.showUberEats(context, event, null, officialTitle, officialText)
    }
}


private const val FOODPANDA_PACKAGE = "com.global.foodpanda.android"
private const val UBER_EATS_PACKAGE = "com.ubercab.eats"
