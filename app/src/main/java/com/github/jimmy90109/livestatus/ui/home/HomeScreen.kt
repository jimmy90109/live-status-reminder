package com.github.jimmy90109.livestatus.ui.home

import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.livestatus.AppReminderPreferences
import com.github.jimmy90109.livestatus.NotificationDebugPayloadStore
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class DebugTarget(val appName: String) {
    TAIWAN_PAY("台灣 Pay"),
    CLOCK("Clock"),
    YOU_BIKE("YouBike"),
    TAIWAN_TAXI("55688"),
    UBER("Uber"),
    FOODPANDA("foodpanda"),
    UBER_EATS("Uber Eats"),
}

@Composable
internal fun HomeScreenHostActivity.MainScreen(
    status: StatusSnapshot,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSamsungNowBarGuide: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onDismissBrandWarning: () -> Unit,
    onAppEnabledChange: (AppReminderPreferences.App, Boolean) -> Unit,
) {
    var settingsExpanded by rememberSaveable { mutableStateOf(true) }
    var showNotificationAccessDisclosure by rememberSaveable { mutableStateOf(false) }
    var settingsLayerVisible by rememberSaveable { mutableStateOf(false) }
    var debugTarget by rememberSaveable { mutableStateOf<DebugTarget?>(null) }
    val settingsProgress = remember {
        Animatable(SETTINGS_CLOSED_PROGRESS)
    }
    val coroutineScope = rememberCoroutineScope()

    fun openSettingsPage() {
        coroutineScope.launch {
            settingsLayerVisible = true
            settingsProgress.snapTo(SETTINGS_CLOSED_PROGRESS)
            settingsProgress.animateTo(
                SETTINGS_OPEN_PROGRESS,
                settingsPageSpring(),
            )
        }
    }

    fun closeSettingsPage() {
        coroutineScope.launch {
            settingsProgress.animateTo(
                SETTINGS_CLOSED_PROGRESS,
                settingsPageSpring(),
            )
            settingsLayerVisible = false
        }
    }

    LaunchedEffect(status.requiredSettingsComplete) {
        settingsExpanded = !status.requiredSettingsComplete
    }
    PredictiveSettingsBackHandler(
        enabled = settingsLayerVisible,
        onProgress = { progress ->
            settingsProgress.snapTo(progress)
        },
        onCancel = {
            settingsProgress.animateTo(
                SETTINGS_OPEN_PROGRESS,
                settingsPageSpring(),
            )
        },
        onBack = {
            settingsProgress.animateTo(
                SETTINGS_CLOSED_PROGRESS,
                settingsPageSpring(),
            )
            settingsLayerVisible = false
        },
    )
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val safeTopPadding = safeDrawingPadding.calculateTopPadding()
    val scrollTopPadding = safeTopPadding + 20.dp
    val heroTopPadding = maxOf(safeTopPadding, 20.dp)
    val scrollBottomPadding = safeDrawingPadding.calculateBottomPadding() + 32.dp
    val appPagerBottomPadding =
        maxOf(
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
            20.dp,
        )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LocalAppColors.current.background,
    ) {
        val currentDebugTarget = debugTarget
        if (currentDebugTarget != null) {
            val colors = LocalAppColors.current
            NotificationDebugPage(
                appName = currentDebugTarget.appName,
                payloadsFlow = when (currentDebugTarget) {
                    DebugTarget.TAIWAN_PAY -> NotificationDebugPayloadStore.taiwanPayPayloads
                    DebugTarget.CLOCK -> NotificationDebugPayloadStore.clockPayloads
                    DebugTarget.YOU_BIKE -> NotificationDebugPayloadStore.youBikePayloads
                    DebugTarget.TAIWAN_TAXI -> NotificationDebugPayloadStore.taiwanTaxiPayloads
                    DebugTarget.UBER -> NotificationDebugPayloadStore.uberPayloads
                    DebugTarget.FOODPANDA -> NotificationDebugPayloadStore.foodpandaPayloads
                    DebugTarget.UBER_EATS -> NotificationDebugPayloadStore.uberEatsPayloads
                },
                cardColor = when (currentDebugTarget) {
                    DebugTarget.TAIWAN_PAY -> colors.taiwanPayContainer
                    DebugTarget.CLOCK -> colors.clockContainer
                    DebugTarget.YOU_BIKE -> colors.youBikeContainer
                    DebugTarget.TAIWAN_TAXI -> colors.taiwanTaxiContainer
                    DebugTarget.UBER -> colors.commonContainer
                    DebugTarget.FOODPANDA -> colors.foodpandaContainer
                    DebugTarget.UBER_EATS -> colors.uberEatsContainer
                },
                actionColor = when (currentDebugTarget) {
                    DebugTarget.TAIWAN_PAY -> colors.taiwanPayPrimary
                    DebugTarget.CLOCK -> colors.onSurface
                    DebugTarget.YOU_BIKE -> colors.youBikeText
                    DebugTarget.TAIWAN_TAXI -> colors.taiwanTaxiText
                    DebugTarget.UBER -> colors.onSurface
                    DebugTarget.FOODPANDA -> colors.foodpandaText
                    DebugTarget.UBER_EATS -> colors.uberEatsText
                },
                showPinDetails = currentDebugTarget == DebugTarget.UBER ||
                    currentDebugTarget == DebugTarget.UBER_EATS,
                topPadding = scrollTopPadding,
                bottomPadding = scrollBottomPadding,
                onBack = { debugTarget = null },
                onClear = {
                    when (currentDebugTarget) {
                        DebugTarget.TAIWAN_PAY -> NotificationDebugPayloadStore.clearTaiwanPay()
                        DebugTarget.CLOCK -> NotificationDebugPayloadStore.clearClock()
                        DebugTarget.YOU_BIKE -> NotificationDebugPayloadStore.clearYouBike()
                        DebugTarget.TAIWAN_TAXI -> NotificationDebugPayloadStore.clearTaiwanTaxi()
                        DebugTarget.UBER -> NotificationDebugPayloadStore.clearUber()
                        DebugTarget.FOODPANDA -> NotificationDebugPayloadStore.clearFoodpanda()
                        DebugTarget.UBER_EATS -> NotificationDebugPayloadStore.clearUberEats()
                    }
                },
            )
            StatusBarProtection()
            return@Surface
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentAlignment = Alignment.TopStart,
        ) {
            if (maxWidth > maxHeight) {
                HomeBackgroundLayer(progress = settingsProgress.value) {
                    HomeContentWide(
                        status = status,
                        settingsExpanded = settingsExpanded,
                        scrollTopPadding = scrollTopPadding,
                        scrollBottomPadding = scrollBottomPadding,
                        appPagerBottomPadding = appPagerBottomPadding,
                        onOpenSettings = { openSettingsPage() },
                        onToggleSettings = { settingsExpanded = !settingsExpanded },
                        onOpenNotificationAccess = {
                            showNotificationAccessDisclosure = true
                        },
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onOpenSamsungNowBarGuide = onOpenSamsungNowBarGuide,
                        onDismissBrandWarning = onDismissBrandWarning,
                        onAppEnabledChange = onAppEnabledChange,
                        onOpenTaiwanPayDebug = { debugTarget = DebugTarget.TAIWAN_PAY },
                        onOpenClockDebug = { debugTarget = DebugTarget.CLOCK },
                        onOpenYouBikeDebug = { debugTarget = DebugTarget.YOU_BIKE },
                        onOpenFoodpandaDebug = { debugTarget = DebugTarget.FOODPANDA },
                        onOpenTaiwanTaxiDebug = { debugTarget = DebugTarget.TAIWAN_TAXI },
                        onOpenUberDebug = { debugTarget = DebugTarget.UBER },
                        onOpenUberEatsDebug = { debugTarget = DebugTarget.UBER_EATS },
                    )
                }
            } else {
                HomeBackgroundLayer(progress = settingsProgress.value) {
                    HomeContentNarrow(
                        status = status,
                        settingsExpanded = settingsExpanded,
                        scrollTopPadding = heroTopPadding,
                        appPagerBottomPadding = appPagerBottomPadding,
                        onOpenSettings = { openSettingsPage() },
                        onToggleSettings = { settingsExpanded = !settingsExpanded },
                        onOpenNotificationAccess = {
                            showNotificationAccessDisclosure = true
                        },
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onOpenSamsungNowBarGuide = onOpenSamsungNowBarGuide,
                        onDismissBrandWarning = onDismissBrandWarning,
                        onAppEnabledChange = onAppEnabledChange,
                        onOpenTaiwanPayDebug = { debugTarget = DebugTarget.TAIWAN_PAY },
                        onOpenClockDebug = { debugTarget = DebugTarget.CLOCK },
                        onOpenYouBikeDebug = { debugTarget = DebugTarget.YOU_BIKE },
                        onOpenFoodpandaDebug = { debugTarget = DebugTarget.FOODPANDA },
                        onOpenTaiwanTaxiDebug = { debugTarget = DebugTarget.TAIWAN_TAXI },
                        onOpenUberDebug = { debugTarget = DebugTarget.UBER },
                        onOpenUberEatsDebug = { debugTarget = DebugTarget.UBER_EATS },
                    )
                }
            }
            if (settingsLayerVisible) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = size.width * settingsProgress.value
                        },
                ) {
                    SettingsPage(
                        status = status,
                        topPadding = scrollTopPadding,
                        bottomPadding = scrollBottomPadding,
                        onBack = { closeSettingsPage() },
                        onOpenNotificationAccess = {
                            showNotificationAccessDisclosure = true
                        },
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onOpenSamsungNowBarGuide = onOpenSamsungNowBarGuide,
                        onOpenPrivacyPolicy = onOpenPrivacyPolicy,
                    )
                }
            }
        }
        StatusBarProtection()
    }

    if (showNotificationAccessDisclosure) {
        NotificationAccessDisclosureDialog(
            onDismiss = { showNotificationAccessDisclosure = false },
            onContinue = {
                showNotificationAccessDisclosure = false
                onOpenNotificationAccess()
            },
        )
    }
}

@Composable
private fun StatusBarProtection() {
    val color = LocalAppColors.current.background

    Box(Modifier.fillMaxSize()) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to color.copy(alpha = 0.8f),
                            0.35f to color.copy(alpha = 0.72f),
                            0.72f to color.copy(alpha = 0.32f),
                            1f to color.copy(alpha = 0f),
                        ),
                    ),
                ),
        )
    }
}


@OptIn(ExperimentalActivityApi::class)
@Composable
private fun PredictiveSettingsBackHandler(
    enabled: Boolean,
    onProgress: suspend (Float) -> Unit,
    onCancel: suspend () -> Unit,
    onBack: suspend () -> Unit,
) {
    PredictiveBackHandler(enabled = enabled) { progress ->
        try {
            progress.collect { backEvent ->
                onProgress(backEvent.progress)
            }
            onBack()
        } catch (exception: CancellationException) {
            onCancel()
            throw exception
        }
    }
}

@Composable
private fun NotificationAccessDisclosureDialog(
    onDismiss: () -> Unit,
    onContinue: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("允許讀取通知前，請先了解") },
        text = {
            Text(
                "即時狀態提醒會讀取媒體播放、Clock、iPASS MONEY、台灣 Pay、YouBike、foodpanda、55688、Uber、Uber Eats 與 Pikmin Bloom 的通知內容，" +
                    "用來辨識曲名與作者、倒數計時、乘車、YouBike 騎乘費用、外送進度、55688 車牌、Uber / Uber Eats PIN 與 Pikmin Bloom 種花狀態，並在本機產生提醒。\n\n" +
                    "通知內容只在您的裝置上即時處理；App 不會上傳、出售或分享這些資料，" +
                    "也不會永久儲存通知內容、車牌或 PIN。YouBike 功能只暫存目前騎乘所需的時間、站名、車柱、車號與服務區域，最長 24 小時。您隨時可以在系統設定中關閉通知存取權限。",
            )
        },
        confirmButton = {
            TextButton(onClick = onContinue) {
                Text("了解並前往設定")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("暫時不要")
            }
        },
    )
}

private const val SETTINGS_OPEN_PROGRESS = 0f
private const val SETTINGS_CLOSED_PROGRESS = 1f
private const val HOME_BACKGROUND_SHIFT_FRACTION = 0.05f
private const val HOME_BACKGROUND_DIM_FRACTION = 0.34f

private fun settingsPageSpring() = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow,
)
