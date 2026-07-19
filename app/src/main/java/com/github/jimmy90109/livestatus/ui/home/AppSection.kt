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
internal fun AppsSection(
    status: StatusSnapshot,
    horizontalContentPadding: Dp,
    onAppEnabledChange: (AppReminderPreferences.App, Boolean) -> Unit,
    onOpenClockDebug: () -> Unit,
    onOpenFoodpandaDebug: () -> Unit,
    onOpenUberDebug: () -> Unit,
    onOpenUberEatsDebug: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = TAB_IPASS) { APP_PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var selectedTab by remember { mutableIntStateOf(TAB_IPASS) }
    var maxPageHeightPx by remember(
        status.clockInstalled,
        status.ipassInstalled,
        status.foodpandaInstalled,
        status.uberInstalled,
        status.uberEatsInstalled,
        status.pikminBloomInstalled,
    ) {
        mutableIntStateOf(0)
    }
    val fixedPagerHeightModifier = if (maxPageHeightPx > 0) {
        with(density) { Modifier.height(maxPageHeightPx.toDp()) }
    } else {
        Modifier
    }

    Column(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = horizontalContentPadding)) {
            SectionHeader(
                title = "App",
                subtitle = "檢查安裝狀態，並分別測試各 App 的即時通知。",
            )
        }
        Spacer(Modifier.height(12.dp))
        AppTabs(
            selectedTab = selectedTab,
            horizontalContentPadding = horizontalContentPadding,
            onSelect = { page ->
                selectedTab = page
                coroutineScope.launch {
                    pagerState.animateScrollToPage(
                        page = page,
                        animationSpec = tween(
                            durationMillis = APP_PAGE_ANIMATION_MILLIS,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                }
            },
        )
        Spacer(Modifier.height(12.dp))
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .then(fixedPagerHeightModifier),
            userScrollEnabled = false,
            verticalAlignment = Alignment.Top,
            beyondViewportPageCount = APP_PAGE_COUNT - 1,
        ) { page ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .onSizeChanged { size ->
                        if (size.height > maxPageHeightPx) {
                            maxPageHeightPx = size.height
                        }
                    }
                    .padding(horizontal = horizontalContentPadding),
            ) {
                when (page) {
                    TAB_CLOCK -> ClockCard(
                        installed = status.clockInstalled,
                        enabled = status.clockEnabled,
                        onEnabledChange = {
                            onAppEnabledChange(AppReminderPreferences.App.CLOCK, it)
                        },
                        onOpenDebug = onOpenClockDebug,
                    )
                    TAB_FOODPANDA -> FoodpandaCard(
                        installed = status.foodpandaInstalled,
                        enabled = status.foodpandaEnabled,
                        onEnabledChange = {
                            onAppEnabledChange(AppReminderPreferences.App.FOODPANDA, it)
                        },
                        onOpenDebug = onOpenFoodpandaDebug,
                    )
                    TAB_UBER_EATS -> UberEatsCard(
                        installed = status.uberEatsInstalled,
                        enabled = status.uberEatsEnabled,
                        onEnabledChange = {
                            onAppEnabledChange(AppReminderPreferences.App.UBER_EATS, it)
                        },
                        onOpenDebug = onOpenUberEatsDebug,
                    )
                    TAB_UBER -> UberRideCard(
                        installed = status.uberInstalled,
                        enabled = status.uberEnabled,
                        onEnabledChange = {
                            onAppEnabledChange(AppReminderPreferences.App.UBER_RIDE, it)
                        },
                        onOpenDebug = onOpenUberDebug,
                    )
                    TAB_PIKMIN_BLOOM -> PikminBloomCard(
                        installed = status.pikminBloomInstalled,
                        enabled = status.pikminBloomEnabled,
                        onEnabledChange = {
                            onAppEnabledChange(AppReminderPreferences.App.PIKMIN_BLOOM, it)
                        },
                    )
                    else -> IpassCard(
                        installed = status.ipassInstalled,
                        enabled = status.ipassEnabled,
                        onEnabledChange = {
                            onAppEnabledChange(AppReminderPreferences.App.IPASS, it)
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun AppTabs(
    selectedTab: Int,
    horizontalContentPadding: Dp,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Spacer(Modifier.width(horizontalContentPadding))
        AppTab("iPASS MONEY", TAB_IPASS, selectedTab, colors.ipassPrimary, colors.commonOnPrimary, onSelect)
        AppTab("foodpanda", TAB_FOODPANDA, selectedTab, colors.foodpandaPrimary, colors.commonOnPrimary, onSelect)
        AppTab("Uber Eats", TAB_UBER_EATS, selectedTab, colors.uberEatsPrimary, colors.commonOnPrimary, onSelect)
        AppTab("Uber", TAB_UBER, selectedTab, colors.commonPrimary, colors.commonOnPrimary, onSelect)
        AppTab("Pikmin Bloom", TAB_PIKMIN_BLOOM, selectedTab, colors.pikminPrimary, colors.commonOnPrimary, onSelect)
        AppTab("Clock", TAB_CLOCK, selectedTab, colors.clockPrimary, colors.commonOnPrimary, onSelect)
        Spacer(Modifier.width(horizontalContentPadding))
    }
}

@Composable
private fun AppTab(
    label: String,
    tab: Int,
    selectedTab: Int,
    selectedColor: Color,
    selectedContentColor: Color,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    val selected = tab == selectedTab
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .background(if (selected) selectedColor else colors.commonSurface, shape)
            .clip(shape)
            .clickable(role = Role.Tab) { onSelect(tab) }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = if (selected) selectedContentColor else colors.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}



private const val TAB_IPASS = 0
private const val TAB_FOODPANDA = 1
private const val TAB_UBER_EATS = 2
private const val TAB_UBER = 3
private const val TAB_PIKMIN_BLOOM = 4
private const val TAB_CLOCK = 5
private const val APP_PAGE_COUNT = 6
private const val APP_PAGE_ANIMATION_MILLIS = 300
