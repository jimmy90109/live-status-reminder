package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.livestatus.AppReminderPreferences
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@Composable
internal fun AppsSection(
    status: StatusSnapshot,
    horizontalContentPadding: Dp,
    onAppEnabledChange: (AppReminderPreferences.App, Boolean) -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = TAB_IPASS) { APP_PAGE_COUNT }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current
    var maxPageHeightPx by remember(
        status.ipassInstalled,
        status.foodpandaInstalled,
        status.uberEatsInstalled,
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
            Spacer(Modifier.height(12.dp))
            AppTabs(
                selectedTab = pagerState.currentPage,
                onSelect = { page ->
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
        }
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
                    TAB_FOODPANDA -> FoodpandaCard(
                        installed = status.foodpandaInstalled,
                        enabled = status.foodpandaEnabled,
                        onEnabledChange = {
                            onAppEnabledChange(AppReminderPreferences.App.FOODPANDA, it)
                        },
                    )
                    TAB_UBER_EATS -> UberEatsCard(
                        installed = status.uberEatsInstalled,
                        enabled = status.uberEatsEnabled,
                        onEnabledChange = {
                            onAppEnabledChange(AppReminderPreferences.App.UBER_EATS, it)
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
private fun AppTabs(selectedTab: Int, onSelect: (Int) -> Unit) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.commonSurface, RoundedCornerShape(100.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppTab("iPASS MONEY", TAB_IPASS, selectedTab, colors.ipassPrimary, colors.commonOnPrimary, onSelect)
        AppTab("foodpanda", TAB_FOODPANDA, selectedTab, colors.foodpandaPrimary, colors.commonOnPrimary, onSelect)
        AppTab("Uber Eats", TAB_UBER_EATS, selectedTab, colors.uberEatsPrimary, colors.commonOnPrimary, onSelect)
    }
}

@Composable
private fun RowScope.AppTab(
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
            .weight(1f)
            .background(if (selected) selectedColor else Color.Transparent, shape)
            .clip(shape)
            .clickable(role = Role.Tab) { onSelect(tab) }
            .padding(horizontal = 8.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = if (selected) selectedContentColor else colors.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun IpassCard(
    installed: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "iPASS MONEY",
        title = "乘車碼狀態",
        description = "進站後顯示乘車碼捷徑，準備下車時快速開啟。",
        installed = installed,
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.ipassContainer,
        labelColor = colors.ipassSecondaryContainer,
        foregroundColor = colors.onSurface,
    ) {
        ActionButton(
            "模擬上車，顯示提醒  ↑",
            colors.ipassPrimary,
            colors.commonOnPrimary,
            enabled = enabled,
        ) {
            LiveStatusReminder.show(context)
        }
        ActionButton("模擬下車，移除提醒  ✓", colors.ipassTertiaryContainer, colors.onSurface) {
            LiveStatusReminder.clear(context)
        }
    }
}

@Composable
private fun FoodpandaCard(
    installed: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "foodpanda",
        title = "外送訂單狀態",
        description = "外送夥伴出發或即將抵達時，顯示取餐提醒。",
        installed = installed,
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.foodpandaContainer,
        labelColor = colors.foodpandaSecondaryContainer,
        foregroundColor = colors.foodpandaText,
    ) {
        ActionButton("模擬外送中", colors.foodpandaPrimary, colors.commonOnPrimary, enabled = enabled) {
            LiveStatusReminder.showFoodpanda(context, LiveStatusNotificationParser.FoodpandaEvent.COURIER_ON_THE_WAY)
        }
        ActionButton("模擬即將抵達", colors.foodpandaSecondaryContainer, colors.foodpandaText, enabled = enabled) {
            LiveStatusReminder.showFoodpanda(context, LiveStatusNotificationParser.FoodpandaEvent.COURIER_ARRIVING)
        }
        ActionButton("清除 foodpanda 狀態  ✓", colors.foodpandaSecondaryContainer, colors.foodpandaText) {
            LiveStatusReminder.clearFoodpanda(context)
        }
    }
}

@Composable
private fun UberEatsCard(
    installed: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "Uber Eats",
        title = "五階段訂單進度",
        description = "從接單到即將抵達持續更新；可辨識時也會顯示四位數 PIN。",
        installed = installed,
        enabled = enabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.uberEatsContainer,
        labelColor = colors.uberEatsSecondaryContainer,
        foregroundColor = colors.uberEatsText,
    ) {
        UberEatsTestButton(
            label = "模擬訂單已收到",
            event = LiveStatusNotificationParser.UberEatsEvent.ORDER_RECEIVED,
            officialTitle = "訂單已收到",
            officialText = "抵達時間：12:00-12:10 PM",
            enabled = enabled,
        )
        UberEatsTestButton(
            label = "模擬正在準備訂單",
            event = LiveStatusNotificationParser.UberEatsEvent.PREPARING,
            officialTitle = "正在準備訂單",
            officialText = "抵達時間：12:00-12:10 PM",
            enabled = enabled,
        )
        UberEatsTestButton(
            label = "模擬正在取餐",
            event = LiveStatusNotificationParser.UberEatsEvent.PICKING_UP,
            officialTitle = "正在取餐",
            officialText = "小明 · ABC-1234 · 抵達時間：12:00-12:10 PM",
            enabled = enabled,
        )
        UberEatsTestButton(
            label = "模擬配送中",
            event = LiveStatusNotificationParser.UberEatsEvent.ON_THE_WAY,
            officialTitle = "正前往您所在位置",
            officialText = "小明 · ABC-1234 · 抵達時間為 12:00 PM",
            enabled = enabled,
        )
        UberEatsTestButton(
            label = "模擬快到了",
            event = LiveStatusNotificationParser.UberEatsEvent.ARRIVING,
            officialTitle = "快到了！",
            officialText = "小明 · ABC-1234 · 即將抵達",
            enabled = enabled,
        )
        ActionButton("模擬送達，清除狀態  ✓", colors.uberEatsSecondaryContainer, colors.uberEatsText) {
            LiveStatusReminder.clearUberEats(context)
        }
    }
}

@Composable
private fun UberEatsTestButton(
    label: String,
    event: LiveStatusNotificationParser.UberEatsEvent,
    officialTitle: String,
    officialText: String,
    enabled: Boolean,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    val primary = event == LiveStatusNotificationParser.UberEatsEvent.ORDER_RECEIVED
    ActionButton(
        label,
        if (primary) colors.uberEatsPrimary else colors.uberEatsSecondaryContainer,
        if (primary) colors.commonOnPrimary else colors.uberEatsText,
        enabled = enabled,
    ) {
        LiveStatusReminder.showUberEats(context, event, null, officialTitle, officialText)
    }
}

@Composable
private fun AppCard(
    appName: String,
    title: String,
    description: String,
    installed: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    cardColor: Color,
    labelColor: Color,
    foregroundColor: Color,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    CardSurface(cardColor, 30, 18) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            LabelPill(appName, labelColor, foregroundColor)
            Spacer(Modifier.weight(1f))
            if (installed) {
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.padding(start = 12.dp),
                )
            } else {
                StatusPill(false, "已安裝", "尚未安裝")
            }
        }
        Spacer(Modifier.height(12.dp))
        AppText(title, 20, colors.onSurface, true)
        Spacer(Modifier.height(6.dp))
        AppText(description, 15, colors.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

private const val TAB_IPASS = 0
private const val TAB_FOODPANDA = 1
private const val TAB_UBER_EATS = 2
private const val APP_PAGE_COUNT = 3
private const val APP_PAGE_ANIMATION_MILLIS = 300
