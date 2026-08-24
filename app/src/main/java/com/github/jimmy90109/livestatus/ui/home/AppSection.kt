package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.github.jimmy90109.livestatus.AppReminderPreferences
import com.github.jimmy90109.livestatus.BuildConfig
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.abs

@Composable
internal fun AppsSection(
    modifier: Modifier = Modifier,
    containingScrollState: ScrollState? = null,
    wideLeadingContent: (@Composable () -> Unit)? = null,
    wideTopPadding: Dp = 0.dp,
    wideBottomPadding: Dp = 0.dp,
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
    onOpenBoltDebug: () -> Unit,
    onOpenUberEatsDebug: () -> Unit,
    onOpenYptDebug: () -> Unit,
    onOpenHevyDebug: () -> Unit,
    onOpenStravaDebug: () -> Unit,
    onOpenDiscordDebug: () -> Unit,
    onOpenTeamsDebug: () -> Unit,
    onOpenGoogleRecorderDebug: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = CATEGORY_TRANSIT_CODE) {
        APP_CATEGORY_PAGE_COUNT
    }
    val coroutineScope = rememberCoroutineScope()
    val tabTransitionProgress = remember { Animatable(1f) }
    var tabTransitionFromPage by remember { mutableIntStateOf(CATEGORY_TRANSIT_CODE) }
    var tabTransitionToPage by remember { mutableIntStateOf(CATEGORY_TRANSIT_CODE) }
    var tabTransitionActive by remember { mutableStateOf(false) }
    var pageTransitionActive by remember { mutableStateOf(false) }
    var pageAnimationJob by remember { mutableStateOf<Job?>(null) }
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
    val wideLeadingScrollState = rememberScrollState()

    val onSelectPage: (Int) -> Unit = selectPage@{ page ->
        if (tabTransitionActive && page == tabTransitionToPage) return@selectPage

        pageAnimationJob?.cancel()
        pageAnimationJob = coroutineScope.launch {
            val fromPage = pagerState.settledPage
            if (page == fromPage) return@launch

            tabTransitionProgress.snapTo(0f)
            tabTransitionFromPage = fromPage
            tabTransitionToPage = page
            tabTransitionActive = true
            try {
                if (abs(page - fromPage) > 1) {
                    pagerState.scrollToPage(page)
                    pageTransitionActive = true
                    tabTransitionProgress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(
                            durationMillis = APP_PAGE_ANIMATION_MILLIS,
                            easing = FastOutSlowInEasing,
                        ),
                    )
                } else {
                    coroutineScope {
                        launch {
                            pagerState.animateScrollToPage(
                                page = page,
                                animationSpec = tween(
                                    durationMillis = APP_PAGE_ANIMATION_MILLIS,
                                    easing = FastOutSlowInEasing,
                                ),
                            )
                        }
                        tabTransitionProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = APP_PAGE_ANIMATION_MILLIS,
                                easing = FastOutSlowInEasing,
                            ),
                        )
                    }
                }
            } finally {
                if (tabTransitionToPage == page) {
                    pageTransitionActive = false
                    tabTransitionActive = false
                }
            }
        }
    }
    val tabsContent: @Composable (Dp) -> Unit = { contentPadding ->
        AppTabs(
            currentPage = pagerState.currentPage,
            currentPageOffsetFraction = pagerState.currentPageOffsetFraction,
            transitionFromPage = tabTransitionFromPage,
            transitionToPage = tabTransitionToPage,
            transitionProgress = tabTransitionProgress.value,
            transitionActive = tabTransitionActive,
            horizontalContentPadding = contentPadding,
            onSelect = onSelectPage,
        )
    }
    val pagerContent: @Composable (Modifier, Boolean, Dp, Boolean) -> Unit =
        { pagerModifier, swipeEnabled, pageTopPadding, clipTopCorners ->
            val resolvedPagerModifier = if (clipTopCorners) {
                pagerModifier.clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp))
            } else {
                pagerModifier
            }
            HorizontalPager(
                state = pagerState,
                modifier = resolvedPagerModifier.fillMaxWidth(),
                pageSpacing = 12.dp,
                verticalAlignment = Alignment.Top,
                beyondViewportPageCount = APP_CATEGORY_PAGE_COUNT - 1,
                userScrollEnabled = swipeEnabled && !pageTransitionActive,
            ) { page ->
                val pageScrollState = rememberScrollState()
                val transitionPageStep = (
                    pagerState.layoutInfo.pageSize + pagerState.layoutInfo.pageSpacing
                ).toFloat()
                val transitionDirection =
                    if (tabTransitionToPage > tabTransitionFromPage) 1f else -1f
                val transitionTranslationX = if (pageTransitionActive) {
                    when (page) {
                        tabTransitionFromPage ->
                            (tabTransitionToPage - tabTransitionFromPage) * transitionPageStep -
                                transitionDirection * transitionPageStep * tabTransitionProgress.value
                        tabTransitionToPage ->
                            transitionDirection * transitionPageStep *
                                (1f - tabTransitionProgress.value)
                        else -> 0f
                    }
                } else {
                    0f
                }

                Column(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                        .zIndex(
                            if (pageTransitionActive && page == tabTransitionFromPage) 1f else 0f,
                        )
                        .graphicsLayer {
                            translationX = transitionTranslationX
                        }
                        .verticalScroll(pageScrollState)
                        .padding(top = pageTopPadding, bottom = pageBottomPadding),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    when (page) {
                    CATEGORY_DELIVERY -> {
                        UberEatsCard(
                            installed = status.uberEatsInstalled,
                            enabled = status.uberEatsEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.UBER_EATS, it)
                            },
                            onOpenDebug = onOpenUberEatsDebug,
                        )
                        FoodpandaCard(
                            installed = status.foodpandaInstalled,
                            enabled = status.foodpandaEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.FOODPANDA, it)
                            },
                            onOpenDebug = onOpenFoodpandaDebug,
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
                        if (BuildConfig.DEBUG) {
                            BoltDebugCard(
                                installed = status.boltInstalled,
                                interactionEnabled = status.requiredSettingsComplete,
                                onOpenDebug = onOpenBoltDebug,
                            )
                        }
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
                    CATEGORY_SPORT -> {
                        StravaCard(
                            installed = status.stravaInstalled,
                            enabled = status.stravaEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.STRAVA, it)
                            },
                            onOpenDebug = onOpenStravaDebug,
                        )
                        HevyCard(
                            installed = status.hevyInstalled,
                            enabled = status.hevyEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.HEVY, it)
                            },
                            onOpenDebug = onOpenHevyDebug,
                        )
                        PikminBloomCard(
                            installed = status.pikminBloomInstalled,
                            enabled = status.pikminBloomEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.PIKMIN_BLOOM, it)
                            },
                        )
                    }
                    CATEGORY_TOOL -> {
                        GoogleRecorderCard(
                            installed = status.googleRecorderInstalled,
                            enabled = status.googleRecorderEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.GOOGLE_RECORDER, it)
                            },
                            onOpenDebug = onOpenGoogleRecorderDebug,
                        )
                        YptCard(
                            installed = status.yptInstalled,
                            enabled = status.yptEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.YPT, it)
                            },
                            onOpenDebug = onOpenYptDebug,
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
                    CATEGORY_MEDIA -> {
                        DiscordVoiceCard(
                            installed = status.discordInstalled,
                            enabled = status.discordVoiceEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.DISCORD_VOICE, it)
                            },
                            onOpenDebug = onOpenDiscordDebug,
                        )
                        TeamsCallCard(
                            installed = status.teamsInstalled,
                            enabled = status.teamsCallEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.TEAMS_CALL, it)
                            },
                            onOpenDebug = onOpenTeamsDebug,
                        )
                        MediaPlaybackCard(
                            enabled = status.mediaPlaybackEnabled,
                            interactionEnabled = status.requiredSettingsComplete,
                            onEnabledChange = {
                                onAppEnabledChange(AppReminderPreferences.App.MEDIA_PLAYBACK, it)
                            },
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

    if (wideLeadingContent != null) {
        Row(
            modifier = modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(
                        state = wideLeadingScrollState,
                        reverseScrolling = true,
                    )
                    .padding(top = wideTopPadding, bottom = wideBottomPadding),
            ) {
                wideLeadingContent()
                Spacer(Modifier.height(12.dp))
                SectionHeader(
                    title = "App",
                    subtitle = "檢查安裝狀態，並分別測試各 App 的即時通知。",
                )
                Spacer(Modifier.height(12.dp))
                tabsContent(0.dp)
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                pagerContent(Modifier.weight(1f), true, wideTopPadding, false)
            }
        }
    } else {
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
            tabsContent(horizontalContentPadding)
            Spacer(Modifier.height(12.dp))
            pagerContent(
                Modifier
                    .weight(1f)
                    .padding(horizontal = horizontalContentPadding),
                true,
                0.dp,
                true,
            )
        }
    }
}

@Composable
private fun AppTabs(
    currentPage: Int,
    currentPageOffsetFraction: Float,
    transitionFromPage: Int,
    transitionToPage: Int,
    transitionProgress: Float,
    transitionActive: Boolean,
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
                currentPage,
                currentPageOffsetFraction,
                transitionFromPage,
                transitionToPage,
                transitionProgress,
                transitionActive,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_delivery),
                CATEGORY_DELIVERY,
                currentPage,
                currentPageOffsetFraction,
                transitionFromPage,
                transitionToPage,
                transitionProgress,
                transitionActive,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_ride),
                CATEGORY_RIDE,
                currentPage,
                currentPageOffsetFraction,
                transitionFromPage,
                transitionToPage,
                transitionProgress,
                transitionActive,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_rental),
                CATEGORY_RENTAL,
                currentPage,
                currentPageOffsetFraction,
                transitionFromPage,
                transitionToPage,
                transitionProgress,
                transitionActive,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_sport),
                CATEGORY_SPORT,
                currentPage,
                currentPageOffsetFraction,
                transitionFromPage,
                transitionToPage,
                transitionProgress,
                transitionActive,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_tool),
                CATEGORY_TOOL,
                currentPage,
                currentPageOffsetFraction,
                transitionFromPage,
                transitionToPage,
                transitionProgress,
                transitionActive,
                colors.commonPrimary,
                colors.commonOnPrimary,
                onSelect,
            )
            AppTab(
                stringResource(R.string.app_category_media),
                CATEGORY_MEDIA,
                currentPage,
                currentPageOffsetFraction,
                transitionFromPage,
                transitionToPage,
                transitionProgress,
                transitionActive,
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
    currentPage: Int,
    currentPageOffsetFraction: Float,
    transitionFromPage: Int,
    transitionToPage: Int,
    transitionProgress: Float,
    transitionActive: Boolean,
    selectedColor: Color,
    selectedContentColor: Color,
    onSelect: (Int) -> Unit,
) {
    val colors = LocalAppColors.current
    val selectionFraction = if (transitionActive) {
        when (tab) {
            transitionFromPage -> 1f - transitionProgress
            transitionToPage -> transitionProgress
            else -> 0f
        }
    } else {
        (1f - abs(currentPage - tab + currentPageOffsetFraction)).coerceIn(0f, 1f)
    }
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .background(
                lerp(colors.commonSurface, selectedColor, selectionFraction),
                shape,
            )
            .clip(shape)
            .hapticClickable(
                role = Role.Tab,
                effect = if (tab == currentPage) null else HapticEffect.SELECTION,
            ) {
                onSelect(tab)
            }
            .padding(horizontal = 16.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Text(
            text = label,
            color = lerp(
                colors.onSurfaceVariant,
                selectedContentColor,
                selectionFraction,
            ),
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
private const val CATEGORY_SPORT = 4
private const val CATEGORY_TOOL = 5
private const val CATEGORY_MEDIA = 6
private const val APP_CATEGORY_PAGE_COUNT = 7
private const val APP_PAGE_ANIMATION_MILLIS = 300
private const val APP_TABS_EDGE_FADE_ANIMATION_MILLIS = 180
private val APP_TABS_EDGE_FADE_OVERLAP = 24.dp
