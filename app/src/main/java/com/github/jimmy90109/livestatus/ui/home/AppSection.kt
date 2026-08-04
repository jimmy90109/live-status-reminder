package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.livestatus.AppReminderPreferences
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors
import kotlinx.coroutines.launch

@Composable
internal fun AppsSection(
    modifier: Modifier = Modifier,
    containingScrollState: ScrollState? = null,
    pageBottomPadding: Dp,
    status: StatusSnapshot,
    horizontalContentPadding: Dp,
    onAppEnabledChange: (AppReminderPreferences.App, Boolean) -> Unit,
    onOpenTaiwanPayDebug: () -> Unit,
    onOpenClockDebug: () -> Unit,
    onOpenYouBikeDebug: () -> Unit,
    onOpenFoodpandaDebug: () -> Unit,
    onOpenTaiwanTaxiDebug: () -> Unit,
    onOpenUberDebug: () -> Unit,
    onOpenUberEatsDebug: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = CATEGORY_TRANSIT_CODE) {
        APP_CATEGORY_PAGE_COUNT
    }
    val coroutineScope = rememberCoroutineScope()
    var selectedTab by remember { mutableIntStateOf(CATEGORY_TRANSIT_CODE) }
    val containingScrollConnection = remember(containingScrollState) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                val scrollState = containingScrollState
                if (scrollState == null || available.y >= 0f) return Offset.Zero

                val consumed = scrollState.dispatchRawDelta(-available.y)
                return Offset(x = 0f, y = -consumed)
            }
        }
    }

    LaunchedEffect(pagerState.settledPage) {
        selectedTab = pagerState.settledPage
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .nestedScroll(containingScrollConnection),
    ) {
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
                .padding(horizontal = horizontalContentPadding)
                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
                .weight(1f),
            pageSpacing = 12.dp,
            verticalAlignment = Alignment.Top,
            beyondViewportPageCount = APP_CATEGORY_PAGE_COUNT - 1,
        ) { page ->
            val pageScrollState = rememberScrollState()

            Column(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .verticalScroll(pageScrollState)
                    .padding(bottom = pageBottomPadding),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when (page) {
                    CATEGORY_DELIVERY -> {
                        FoodpandaCard(
                            installed = status.foodpandaInstalled,
                            enabled = status.foodpandaEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.FOODPANDA, it)
                            },
                            onOpenDebug = onOpenFoodpandaDebug,
                        )
                        UberEatsCard(
                            installed = status.uberEatsInstalled,
                            enabled = status.uberEatsEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.UBER_EATS, it)
                            },
                            onOpenDebug = onOpenUberEatsDebug,
                        )
                    }
                    CATEGORY_RIDE -> {
                        UberRideCard(
                            installed = status.uberInstalled,
                            enabled = status.uberEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.UBER_RIDE, it)
                            },
                            onOpenDebug = onOpenUberDebug,
                        )
                        TaiwanTaxiCard(
                            installed = status.taiwanTaxiInstalled,
                            enabled = status.taiwanTaxiEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.TAIWAN_TAXI, it)
                            },
                            onOpenDebug = onOpenTaiwanTaxiDebug,
                        )
                    }
                    CATEGORY_RENTAL -> YouBikeCard(
                        installed = status.youBikeInstalled,
                        enabled = status.youBikeEnabled,
                        exactAlarmAllowed = status.youBikeExactAlarmAllowed,
                        interactionEnabled = status.requiredSettingsComplete,
                        onEnabledChange = {
                            onAppEnabledChange(AppReminderPreferences.App.YOUBIKE, it)
                        },
                        onOpenDebug = onOpenYouBikeDebug,
                    )
                    CATEGORY_GAME -> PikminBloomCard(
                        installed = status.pikminBloomInstalled,
                        enabled = status.pikminBloomEnabled,
                        interactionEnabled = status.requiredSettingsComplete,
                        onEnabledChange = {
                            onAppEnabledChange(AppReminderPreferences.App.PIKMIN_BLOOM, it)
                        },
                    )
                    CATEGORY_TOOL -> {
                        MediaPlaybackCard(
                            enabled = status.mediaPlaybackEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.MEDIA_PLAYBACK, it)
                            },
                        )
                        ClockCard(
                            installed = status.clockInstalled,
                            enabled = status.clockEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.CLOCK, it)
                            },
                            onOpenDebug = onOpenClockDebug,
                        )
                    }
                    else -> {
                        IpassCard(
                            installed = status.ipassInstalled,
                            enabled = status.ipassEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.IPASS, it)
                            },
                        )
                        TaiwanPayCard(
                            installed = status.taiwanPayInstalled,
                            enabled = status.taiwanPayEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.TAIWAN_PAY, it)
                            },
                            onOpenDebug = onOpenTaiwanPayDebug,
                        )
                    }
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
    val scrollState = rememberScrollState()
    val leftFadeAlpha by animateFloatAsState(
        targetValue = if (scrollState.canScrollBackward) 1f else 0f,
        animationSpec = tween(APP_TABS_EDGE_FADE_ANIMATION_MILLIS),
        label = "App tabs left edge fade",
    )
    val rightFadeAlpha by animateFloatAsState(
        targetValue = if (scrollState.canScrollForward) 1f else 0f,
        animationSpec = tween(APP_TABS_EDGE_FADE_ANIMATION_MILLIS),
        label = "App tabs right edge fade",
    )
    Box(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Spacer(Modifier.width(horizontalContentPadding))
            AppTab(
                stringResource(R.string.app_category_transit_code),
                CATEGORY_TRANSIT_CODE,
                selectedTab,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_delivery),
                CATEGORY_DELIVERY,
                selectedTab,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_ride),
                CATEGORY_RIDE,
                selectedTab,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_rental),
                CATEGORY_RENTAL,
                selectedTab,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_game),
                CATEGORY_GAME,
                selectedTab,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_tool),
                CATEGORY_TOOL,
                selectedTab,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            Spacer(Modifier.width(horizontalContentPadding))
        }
        Canvas(Modifier.matchParentSize()) {
            val fadeWidth = (
                horizontalContentPadding.toPx() + APP_TABS_EDGE_FADE_OVERLAP.toPx()
            ).coerceAtMost(size.width / 2f)
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(colors.background, Color.Transparent),
                    startX = 0f,
                    endX = fadeWidth,
                ),
                size = Size(fadeWidth, size.height),
                alpha = leftFadeAlpha,
            )
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Transparent, colors.background),
                    startX = size.width - fadeWidth,
                    endX = size.width,
                ),
                topLeft = Offset(size.width - fadeWidth, 0f),
                size = Size(fadeWidth, size.height),
                alpha = rightFadeAlpha,
            )
        }
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



private const val CATEGORY_TRANSIT_CODE = 0
private const val CATEGORY_DELIVERY = 1
private const val CATEGORY_RIDE = 2
private const val CATEGORY_RENTAL = 3
private const val CATEGORY_GAME = 4
private const val CATEGORY_TOOL = 5
private const val APP_CATEGORY_PAGE_COUNT = 6
private const val APP_PAGE_ANIMATION_MILLIS = 300
private const val APP_TABS_EDGE_FADE_ANIMATION_MILLIS = 180
private val APP_TABS_EDGE_FADE_OVERLAP = 24.dp
