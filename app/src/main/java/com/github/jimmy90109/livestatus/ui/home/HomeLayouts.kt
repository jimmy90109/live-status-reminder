package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.livestatus.AppReminderPreferences
import com.github.jimmy90109.livestatus.R
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
internal fun HomeBackgroundLayer(
    progress: Float,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val openAmount = 1f - progress
                translationX = -size.width * HOME_BACKGROUND_SHIFT_FRACTION * openAmount
                alpha = 1f - (HOME_BACKGROUND_DIM_FRACTION * openAmount)
            },
    ) {
        content()
    }
}

@Composable
internal fun HomeContentWide(
    status: StatusSnapshot,
    settingsExpanded: Boolean,
    scrollTopPadding: Dp,
    scrollBottomPadding: Dp,
    appPagerBottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onToggleSettings: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSamsungNowBarGuide: () -> Unit,
    onDismissBrandWarning: () -> Unit,
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
    AppsSection(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        wideLeadingContent = {
            HomeIntroColumn(
                status = status,
                settingsExpanded = settingsExpanded,
                onOpenSettings = onOpenSettings,
                onToggleSettings = onToggleSettings,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenSamsungNowBarGuide = onOpenSamsungNowBarGuide,
                onDismissBrandWarning = onDismissBrandWarning,
            )
        },
        wideTopPadding = scrollTopPadding,
        wideBottomPadding = scrollBottomPadding,
        pageBottomPadding = appPagerBottomPadding,
        status = status,
        horizontalContentPadding = 0.dp,
        onAppEnabledChange = onAppEnabledChange,
        onOpenTaiwanPayDebug = onOpenTaiwanPayDebug,
        onOpenClockDebug = onOpenClockDebug,
        onOpenYouBikeDebug = onOpenYouBikeDebug,
        onOpenFoodpandaDebug = onOpenFoodpandaDebug,
        onOpenTaiwanTaxiDebug = onOpenTaiwanTaxiDebug,
        onOpenUberDebug = onOpenUberDebug,
        onOpenBoltDebug = onOpenBoltDebug,
        onOpenUberEatsDebug = onOpenUberEatsDebug,
        onOpenYptDebug = onOpenYptDebug,
        onOpenHevyDebug = onOpenHevyDebug,
        onOpenStravaDebug = onOpenStravaDebug,
        onOpenDiscordDebug = onOpenDiscordDebug,
        onOpenTeamsDebug = onOpenTeamsDebug,
        onOpenGoogleRecorderDebug = onOpenGoogleRecorderDebug,
    )
}

@Composable
internal fun HomeContentNarrow(
    status: StatusSnapshot,
    settingsExpanded: Boolean,
    scrollTopPadding: Dp,
    appPagerBottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onToggleSettings: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSamsungNowBarGuide: () -> Unit,
    onDismissBrandWarning: () -> Unit,
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
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val appsViewportHeight =
            (maxHeight - scrollTopPadding).coerceAtLeast(1.dp)
        val homeScrollState = rememberScrollState()
        val requiredSettingsComplete = status.requiredSettingsComplete
        val heroRevealState = remember {
            AnchoredDraggableState(
                initialValue = initialHeroRevealValue(requiredSettingsComplete),
            )
        }
        var heroHeightPx by remember { mutableIntStateOf(0) }
        val velocityThresholdPx = with(LocalDensity.current) {
            HERO_REVEAL_FLING_THRESHOLD.toPx()
        }
        val hapticFeedback = LocalHapticFeedback.current
        val coroutineScope = rememberCoroutineScope()
        val heroExpanded = heroRevealState.settledValue == HeroRevealValue.Expanded
        val heroVisualState by remember(requiredSettingsComplete, heroRevealState) {
            derivedStateOf {
                if (!requiredSettingsComplete || heroHeightPx <= 0) {
                    HeroRevealVisualState(scale = 1f, alpha = 1f)
                } else {
                    heroRevealVisualState(
                        visibleHeightPx = heroRevealState.requireOffset(),
                        fullHeightPx = heroHeightPx.toFloat(),
                    )
                }
            }
        }
        val heroStateDescription = stringResource(
            if (heroExpanded) {
                R.string.home_intro_expanded
            } else {
                R.string.home_intro_collapsed
            },
        )
        val heroActionLabel = stringResource(
            if (heroExpanded) {
                R.string.home_intro_hide
            } else {
                R.string.home_intro_show
            },
        )
        val heroRevealConnection = remember(
            requiredSettingsComplete,
            heroHeightPx,
            homeScrollState,
            heroRevealState,
            velocityThresholdPx,
            hapticFeedback,
        ) {
            heroRevealNestedScrollConnection(
                enabled = requiredSettingsComplete,
                fullHeightPx = heroHeightPx.toFloat(),
                homeScrollState = homeScrollState,
                state = heroRevealState,
                velocityThresholdPxPerSecond = velocityThresholdPx,
                onHapticEffect = { effect ->
                    hapticFeedback.performHapticFeedback(effect.toFeedbackType())
                },
            )
        }

        LaunchedEffect(requiredSettingsComplete, heroHeightPx) {
            if (heroHeightPx <= 0) return@LaunchedEffect

            heroRevealState.animateTo(
                targetValue = initialHeroRevealValue(requiredSettingsComplete),
                animationSpec = heroRevealSpring(),
            )
        }

        val heroAccessibilityModifier = if (requiredSettingsComplete) {
            Modifier.semantics {
                stateDescription = heroStateDescription
                customActions = listOf(
                    CustomAccessibilityAction(heroActionLabel) {
                        coroutineScope.launch {
                            heroRevealState.animateTo(
                                targetValue = if (heroExpanded) {
                                    HeroRevealValue.Hidden
                                } else {
                                    HeroRevealValue.Expanded
                                },
                                animationSpec = heroRevealSpring(),
                            )
                        }
                        true
                    },
                )
            }
        } else {
            Modifier
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .then(heroAccessibilityModifier)
                .nestedScroll(heroRevealConnection)
                .verticalScroll(homeScrollState)
                .padding(top = scrollTopPadding),
            verticalArrangement = Arrangement.Top,
        ) {
            Column(Modifier.padding(horizontal = 20.dp)) {
                HomeIntroColumn(
                    status = status,
                    settingsExpanded = settingsExpanded,
                    onOpenSettings = onOpenSettings,
                    onToggleSettings = onToggleSettings,
                    onOpenNotificationAccess = onOpenNotificationAccess,
                    onRequestNotificationPermission = onRequestNotificationPermission,
                    onOpenSamsungNowBarGuide = onOpenSamsungNowBarGuide,
                    onDismissBrandWarning = onDismissBrandWarning,
                    heroVisualState = heroVisualState,
                    heroVisibleHeightPx = if (requiredSettingsComplete) {
                        {
                            if (heroHeightPx <= 0) {
                                0f
                            } else {
                                heroRevealState.requireOffset()
                            }
                        }
                    } else {
                        null
                    },
                    heroBottomSpacingVisibleFraction = if (requiredSettingsComplete) {
                        {
                            if (heroHeightPx <= 0) {
                                0f
                            } else {
                                heroRevealState.requireOffset() / heroHeightPx
                            }
                        }
                    } else {
                        null
                    },
                    onHeroHeightChanged = { heightPx ->
                        if (heightPx != heroHeightPx) {
                            heroHeightPx = heightPx
                            heroRevealState.updateAnchors(
                                DraggableAnchors {
                                    HeroRevealValue.Hidden at 0f
                                    HeroRevealValue.Expanded at heightPx.toFloat()
                                },
                            )
                        }
                    },
                )
            }
            if (!requiredSettingsComplete) {
                Spacer(Modifier.height(28.dp))
            }
            AppsSection(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(appsViewportHeight),
                containingScrollState = homeScrollState,
                pageBottomPadding = appPagerBottomPadding,
                status = status,
                horizontalContentPadding = 20.dp,
                onAppEnabledChange = onAppEnabledChange,
                onOpenTaiwanPayDebug = onOpenTaiwanPayDebug,
                onOpenClockDebug = onOpenClockDebug,
                onOpenYouBikeDebug = onOpenYouBikeDebug,
                onOpenFoodpandaDebug = onOpenFoodpandaDebug,
                onOpenTaiwanTaxiDebug = onOpenTaiwanTaxiDebug,
                onOpenUberDebug = onOpenUberDebug,
                onOpenBoltDebug = onOpenBoltDebug,
                onOpenUberEatsDebug = onOpenUberEatsDebug,
                onOpenYptDebug = onOpenYptDebug,
                onOpenHevyDebug = onOpenHevyDebug,
                onOpenStravaDebug = onOpenStravaDebug,
                onOpenDiscordDebug = onOpenDiscordDebug,
                onOpenTeamsDebug = onOpenTeamsDebug,
                onOpenGoogleRecorderDebug = onOpenGoogleRecorderDebug,
            )
        }
    }
}

@Composable
private fun HomeIntroColumn(
    status: StatusSnapshot,
    settingsExpanded: Boolean,
    onOpenSettings: () -> Unit,
    onToggleSettings: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSamsungNowBarGuide: () -> Unit,
    onDismissBrandWarning: () -> Unit,
    heroVisualState: HeroRevealVisualState? = null,
    heroVisibleHeightPx: (() -> Float)? = null,
    heroBottomSpacingVisibleFraction: (() -> Float)? = null,
    onHeroHeightChanged: (Int) -> Unit = {},
) {
    RevealingHeroCard(
        visualState = heroVisualState,
        visibleHeightPx = heroVisibleHeightPx,
        onFullHeightChanged = onHeroHeightChanged,
        onOpenSettings = onOpenSettings,
    )
    if (heroBottomSpacingVisibleFraction != null) {
        RevealableVerticalSpacer(
            fullHeight = 28.dp,
            visibleFraction = heroBottomSpacingVisibleFraction,
        )
    }
    AnimatedVisibility(
        visible = !status.requiredSettingsComplete,
        modifier = Modifier.clip(RoundedCornerShape(26.dp)),
    ) {
        Column {
            Spacer(Modifier.height(28.dp))
            RequiredSettingsSection(
                status = status,
                expanded = settingsExpanded,
                onToggle = onToggleSettings,
                onOpenNotificationAccess = onOpenNotificationAccess,
                onRequestNotificationPermission = onRequestNotificationPermission,
            )
        }
    }
    AnimatedVisibility(
        visible = status.brandWarning != null && !status.brandWarningDismissed,
        modifier = Modifier.clip(RoundedCornerShape(26.dp)),
    ) {
        Column {
            Spacer(Modifier.height(10.dp))
            status.brandWarning?.let { brandWarning ->
                BrandWarningCard(
                    brandWarning = brandWarning,
                    onOpenSamsungNowBarGuide = onOpenSamsungNowBarGuide,
                    onDismiss = onDismissBrandWarning,
                )
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun RevealableVerticalSpacer(
    fullHeight: Dp,
    visibleFraction: (() -> Float)?,
) {
    val fullHeightPx = with(LocalDensity.current) {
        fullHeight.roundToPx()
    }
    Layout(
        content = {},
        modifier = Modifier.fillMaxWidth(),
    ) { _, constraints ->
        val height = visibleFraction
            ?.invoke()
            ?.coerceIn(0f, 1f)
            ?.let { fraction -> (fullHeightPx * fraction).roundToInt() }
            ?: fullHeightPx

        layout(constraints.minWidth, height) {}
    }
}

@Composable
private fun RevealingHeroCard(
    visualState: HeroRevealVisualState?,
    visibleHeightPx: (() -> Float)?,
    onFullHeightChanged: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val targetVisualState = visualState ?: HeroRevealVisualState(scale = 1f, alpha = 1f)
    val weakenedTranslationYPx = with(LocalDensity.current) {
        HERO_REVEAL_WEAKENED_TRANSLATION_Y.toPx()
    }
    val animatedScale by animateFloatAsState(
        targetValue = targetVisualState.scale,
        animationSpec = tween(
            durationMillis = HERO_REVEAL_VISUAL_TRANSITION_MILLIS,
            easing = LinearOutSlowInEasing,
        ),
        label = "Hero reveal scale",
    )
    val animatedAlpha by animateFloatAsState(
        targetValue = targetVisualState.alpha,
        animationSpec = tween(
            durationMillis = HERO_REVEAL_VISUAL_TRANSITION_MILLIS,
            easing = LinearOutSlowInEasing,
        ),
        label = "Hero reveal alpha",
    )
    val animatedTranslationY by animateFloatAsState(
        targetValue = if (targetVisualState.scale < 1f) -weakenedTranslationYPx else 0f,
        animationSpec = tween(
            durationMillis = HERO_REVEAL_VISUAL_TRANSITION_MILLIS,
            easing = LinearOutSlowInEasing,
        ),
        label = "Hero reveal vertical position",
    )
    Layout(
        content = {
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = animatedAlpha
                        translationY = animatedTranslationY
                        transformOrigin = TransformOrigin(
                            pivotFractionX = 0.5f,
                            pivotFractionY = 1f,
                        )
                    }
                    .onSizeChanged { size ->
                        onFullHeightChanged(size.height)
                    },
            ) {
                HeroCard(onOpenSettings = onOpenSettings)
            }
        },
        modifier = Modifier.fillMaxWidth(),
    ) { measurables, constraints ->
        val placeable = measurables.single().measure(constraints)
        val visibleHeight = visibleHeightPx
            ?.invoke()
            ?.roundToInt()
            ?.coerceIn(0, placeable.height)
            ?: placeable.height

        layout(placeable.width, visibleHeight) {
            placeable.placeRelative(
                x = 0,
                y = visibleHeight - placeable.height,
            )
        }
    }
}

private fun heroRevealNestedScrollConnection(
    enabled: Boolean,
    fullHeightPx: Float,
    homeScrollState: ScrollState,
    state: AnchoredDraggableState<HeroRevealValue>,
    velocityThresholdPxPerSecond: Float,
    onHapticEffect: (HapticEffect) -> Unit,
): NestedScrollConnection = object : NestedScrollConnection {
    private val gestureGate = HeroRevealGestureGate()
    private val hapticTracker = HeroRevealHapticTracker()

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!enabled) {
            hapticTracker.reset()
            return Offset.Zero
        }
        if (source != NestedScrollSource.UserInput) {
            return Offset.Zero
        }
        gestureGate.onPreScroll(available.y)
        if (available.y >= 0f) return Offset.Zero

        val previousOffsetPx = state.requireOffset()
        val consumedY = state.dispatchRawDelta(available.y)
        gestureGate.onHeroDragConsumed(consumedY)
        hapticTracker.onDrag(
            startValue = state.settledValue,
            previousOffsetPx = previousOffsetPx,
            offsetPx = previousOffsetPx + consumedY,
            fullHeightPx = fullHeightPx,
        )?.let(onHapticEffect)
        return Offset(
            x = 0f,
            y = consumedY,
        )
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!enabled) {
            hapticTracker.reset()
            return Offset.Zero
        }
        if (
            source != NestedScrollSource.UserInput ||
            !gestureGate.canReveal(
                consumedY = consumed.y,
                availableY = available.y,
                isAtTop = homeScrollState.value == 0,
            )
        ) {
            return Offset.Zero
        }

        val previousOffsetPx = state.requireOffset()
        val consumedY = state.dispatchRawDelta(available.y)
        gestureGate.onHeroDragConsumed(consumedY)
        hapticTracker.onDrag(
            startValue = state.settledValue,
            previousOffsetPx = previousOffsetPx,
            offsetPx = previousOffsetPx + consumedY,
            fullHeightPx = fullHeightPx,
        )?.let(onHapticEffect)
        return Offset(
            x = 0f,
            y = consumedY,
        )
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!enabled || fullHeightPx <= 0f) {
            gestureGate.reset()
            hapticTracker.reset()
            return Velocity.Zero
        }

        val offsetPx = state.requireOffset()
        if (offsetPx <= 0f || offsetPx >= fullHeightPx) {
            gestureGate.reset()
            hapticTracker.reset()
            return Velocity.Zero
        }

        val targetValue = heroRevealTarget(
            offsetPx = offsetPx,
            fullHeightPx = fullHeightPx,
            velocityPxPerSecond = available.y,
            velocityThresholdPxPerSecond = velocityThresholdPxPerSecond,
        )
        hapticTracker.onRelease(targetValue)?.let(onHapticEffect)
        try {
            state.animateTo(
                targetValue = targetValue,
                animationSpec = heroRevealSpring(),
            )
        } finally {
            gestureGate.reset()
            hapticTracker.reset()
        }
        return Velocity(x = 0f, y = available.y)
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        gestureGate.reset()
        hapticTracker.reset()
        return Velocity.Zero
    }
}

private fun heroRevealSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)

private const val HOME_BACKGROUND_SHIFT_FRACTION = 0.05f
private const val HOME_BACKGROUND_DIM_FRACTION = 0.34f
private val HERO_REVEAL_FLING_THRESHOLD = 125.dp
private val HERO_REVEAL_WEAKENED_TRANSLATION_Y = 12.dp
private const val HERO_REVEAL_VISUAL_TRANSITION_MILLIS = 200
