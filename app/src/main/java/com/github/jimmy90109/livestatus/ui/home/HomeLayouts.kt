package com.github.jimmy90109.livestatus.ui.home

import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.livestatus.AppReminderPreferences
import com.github.jimmy90109.livestatus.NotificationDebugPayloadStore
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors
import kotlinx.coroutines.CancellationException
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
    onOpenFoodpandaDebug: () -> Unit,
    onOpenUberDebug: () -> Unit,
    onOpenUberEatsDebug: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = scrollTopPadding, bottom = scrollBottomPadding),
            verticalArrangement = Arrangement.Top,
        ) {
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
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = scrollTopPadding),
            verticalArrangement = Arrangement.Top,
        ) {
            AppsSection(
                modifier = Modifier.fillMaxSize(),
                pageBottomPadding = appPagerBottomPadding,
                status = status,
                horizontalContentPadding = 0.dp,
                onAppEnabledChange = onAppEnabledChange,
                onOpenTaiwanPayDebug = onOpenTaiwanPayDebug,
                onOpenClockDebug = onOpenClockDebug,
                onOpenFoodpandaDebug = onOpenFoodpandaDebug,
                onOpenUberDebug = onOpenUberDebug,
                onOpenUberEatsDebug = onOpenUberEatsDebug,
            )
        }
    }
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
    onOpenFoodpandaDebug: () -> Unit,
    onOpenUberDebug: () -> Unit,
    onOpenUberEatsDebug: () -> Unit,
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
        val coroutineScope = rememberCoroutineScope()
        val heroExpanded = heroRevealState.settledValue == HeroRevealValue.Expanded
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
        ) {
            heroRevealNestedScrollConnection(
                enabled = requiredSettingsComplete,
                fullHeightPx = heroHeightPx.toFloat(),
                homeScrollState = homeScrollState,
                state = heroRevealState,
                velocityThresholdPxPerSecond = velocityThresholdPx,
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
                onOpenFoodpandaDebug = onOpenFoodpandaDebug,
                onOpenUberDebug = onOpenUberDebug,
                onOpenUberEatsDebug = onOpenUberEatsDebug,
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
    heroVisibleHeightPx: (() -> Float)? = null,
    heroBottomSpacingVisibleFraction: (() -> Float)? = null,
    onHeroHeightChanged: (Int) -> Unit = {},
) {
    RevealingHeroCard(
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
    visibleHeightPx: (() -> Float)?,
    onFullHeightChanged: (Int) -> Unit,
    onOpenSettings: () -> Unit,
) {
    Layout(
        content = {
            Box(
                modifier = Modifier.onSizeChanged { size ->
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
): NestedScrollConnection = object : NestedScrollConnection {
    private val gestureGate = HeroRevealGestureGate()

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (!enabled || source != NestedScrollSource.UserInput) {
            return Offset.Zero
        }
        gestureGate.onPreScroll(available.y)
        if (available.y >= 0f) return Offset.Zero

        return Offset(
            x = 0f,
            y = state.dispatchRawDelta(available.y),
        )
    }

    override fun onPostScroll(
        consumed: Offset,
        available: Offset,
        source: NestedScrollSource,
    ): Offset {
        if (
            !enabled ||
            source != NestedScrollSource.UserInput ||
            !gestureGate.canReveal(
                consumedY = consumed.y,
                availableY = available.y,
                isAtTop = homeScrollState.value == 0,
            )
        ) {
            return Offset.Zero
        }

        return Offset(
            x = 0f,
            y = state.dispatchRawDelta(available.y),
        )
    }

    override suspend fun onPreFling(available: Velocity): Velocity {
        if (!enabled || fullHeightPx <= 0f) {
            gestureGate.reset()
            return Velocity.Zero
        }

        val offsetPx = state.requireOffset()
        if (offsetPx <= 0f || offsetPx >= fullHeightPx) {
            gestureGate.reset()
            return Velocity.Zero
        }

        state.animateTo(
            targetValue = heroRevealTarget(
                offsetPx = offsetPx,
                fullHeightPx = fullHeightPx,
                velocityPxPerSecond = available.y,
                velocityThresholdPxPerSecond = velocityThresholdPxPerSecond,
            ),
            animationSpec = heroRevealSpring(),
        )
        gestureGate.reset()
        return Velocity(x = 0f, y = available.y)
    }

    override suspend fun onPostFling(
        consumed: Velocity,
        available: Velocity,
    ): Velocity {
        gestureGate.reset()
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
