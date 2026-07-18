package com.github.jimmy90109.livestatus.ui.home

import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.livestatus.AppReminderPreferences
import com.github.jimmy90109.livestatus.NotificationDebugPayloadStore
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch


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
    onOpenSettings: () -> Unit,
    onToggleSettings: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSamsungNowBarGuide: () -> Unit,
    onDismissNowBarTroubleshooting: () -> Unit,
    onAppEnabledChange: (AppReminderPreferences.App, Boolean) -> Unit,
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
                onDismissNowBarTroubleshooting = onDismissNowBarTroubleshooting,
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = scrollTopPadding, bottom = scrollBottomPadding),
            verticalArrangement = Arrangement.Top,
        ) {
            AppsSection(
                status = status,
                horizontalContentPadding = 0.dp,
                onAppEnabledChange = onAppEnabledChange,
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
    scrollBottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onToggleSettings: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSamsungNowBarGuide: () -> Unit,
    onDismissNowBarTroubleshooting: () -> Unit,
    onAppEnabledChange: (AppReminderPreferences.App, Boolean) -> Unit,
    onOpenClockDebug: () -> Unit,
    onOpenFoodpandaDebug: () -> Unit,
    onOpenUberDebug: () -> Unit,
    onOpenUberEatsDebug: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = scrollTopPadding, bottom = scrollBottomPadding),
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
                onDismissNowBarTroubleshooting = onDismissNowBarTroubleshooting,
            )
        }
        Spacer(Modifier.height(28.dp))
        AppsSection(
            status = status,
            horizontalContentPadding = 20.dp,
            onAppEnabledChange = onAppEnabledChange,
            onOpenClockDebug = onOpenClockDebug,
            onOpenFoodpandaDebug = onOpenFoodpandaDebug,
            onOpenUberDebug = onOpenUberDebug,
            onOpenUberEatsDebug = onOpenUberEatsDebug,
        )
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
    onDismissNowBarTroubleshooting: () -> Unit,
) {
    HeroCard(onOpenSettings = onOpenSettings)
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
        visible = status.isSamsungDevice && !status.nowBarTroubleshootingDismissed,
        modifier = Modifier.clip(RoundedCornerShape(26.dp)),
    ) {
        Column {
            Spacer(Modifier.height(10.dp))
            SamsungNowBarTroubleshootingCard(
                onClick = onOpenSamsungNowBarGuide,
                onDismiss = onDismissNowBarTroubleshooting,
            )
        }
    }
    AnimatedVisibility(
        visible = status.isXiaomiDevice,
        modifier = Modifier.clip(RoundedCornerShape(26.dp)),
    ) {
        Column {
            Spacer(Modifier.height(10.dp))
            XiaomiHyperIslandInfoCard()
        }
    }
}


private const val HOME_BACKGROUND_SHIFT_FRACTION = 0.05f
private const val HOME_BACKGROUND_DIM_FRACTION = 0.34f
