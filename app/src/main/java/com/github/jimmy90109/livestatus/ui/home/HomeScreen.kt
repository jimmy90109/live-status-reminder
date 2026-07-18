package com.github.jimmy90109.livestatus.ui.home

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.ExperimentalActivityApi
import androidx.activity.compose.PredictiveBackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
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
import com.github.jimmy90109.livestatus.ClockTimerNotificationExtractor
import com.github.jimmy90109.livestatus.LiveStatusNotificationListenerService
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.NotificationDebugPayloadStore
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors
import com.github.jimmy90109.livestatus.ui.theme.LiveStatusTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

open class HomeScreenHostActivity : ComponentActivity() {
    private var statusSnapshot by mutableStateOf(StatusSnapshot())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        LiveStatusReminder.createChannel(this)

        when (intent.action) {
            ACTION_OPEN_CLOCK -> openClock()
            ACTION_OPEN_IPASS -> openIpass()
            ACTION_OPEN_FOODPANDA -> openFoodpanda()
            ACTION_OPEN_UBER -> openUber()
            ACTION_OPEN_UBER_EATS -> openUberEats()
            ACTION_OPEN_PIKMIN_BLOOM -> openPikminBloom()
            else -> {
                setContent {
                    LiveStatusTheme {
                        MainScreen(
                            status = statusSnapshot,
                            onOpenNotificationAccess = ::openNotificationListenerSettings,
                            onRequestNotificationPermission = ::requestNotificationPermission,
                            onOpenSamsungNowBarGuide = ::openSamsungNowBarGuide,
                            onOpenPrivacyPolicy = ::openPrivacyPolicy,
                            onDismissNowBarTroubleshooting = ::dismissNowBarTroubleshooting,
                            onAppEnabledChange = ::setAppEnabled,
                        )
                    }
                }
                return
            }
        }
        finish()
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val clockInstalled = isPackageInstalled(CLOCK_PACKAGE)
        val ipassInstalled = isPackageInstalled(IPASS_PACKAGE)
        val foodpandaInstalled = isPackageInstalled(FOODPANDA_PACKAGE)
        val uberInstalled = isPackageInstalled(UBER_PACKAGE)
        val uberEatsInstalled = isPackageInstalled(UBER_EATS_PACKAGE)
        val pikminBloomInstalled = isPackageInstalled(PIKMIN_BLOOM_PACKAGE)
        statusSnapshot = StatusSnapshot(
            notificationAccess = isNotificationAccessEnabled(),
            notificationPermission = canPostNotifications(),
            clockInstalled = clockInstalled,
            ipassInstalled = ipassInstalled,
            foodpandaInstalled = foodpandaInstalled,
            uberInstalled = uberInstalled,
            uberEatsInstalled = uberEatsInstalled,
            pikminBloomInstalled = pikminBloomInstalled,
            isSamsungDevice = isSamsungDevice(),
            isXiaomiDevice = isXiaomiDevice(),
            nowBarTroubleshootingDismissed =
                AppReminderPreferences.isNowBarTroubleshootingDismissed(this),
            clockEnabled = AppReminderPreferences.App.CLOCK.isEnabled(this, clockInstalled),
            ipassEnabled = AppReminderPreferences.App.IPASS.isEnabled(this, ipassInstalled),
            foodpandaEnabled = AppReminderPreferences.App.FOODPANDA.isEnabled(this, foodpandaInstalled),
            uberEnabled = AppReminderPreferences.App.UBER_RIDE.isEnabled(this, uberInstalled),
            uberEatsEnabled = AppReminderPreferences.App.UBER_EATS.isEnabled(this, uberEatsInstalled),
            pikminBloomEnabled =
                AppReminderPreferences.App.PIKMIN_BLOOM.isEnabled(this, pikminBloomInstalled),
        )
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners",
        )
        val listener = ComponentName(this, LiveStatusNotificationListenerService::class.java)
        return enabledListeners?.contains(listener.flattenToString()) == true
    }

    private fun canPostNotifications(): Boolean =
        checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED &&
            getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    private fun isPackageInstalled(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null

    private fun openNotificationListenerSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }

    private fun requestNotificationPermission() {
        requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQUEST_NOTIFICATIONS)
    }

    private fun openSamsungNowBarGuide() {
        openWebPage(SAMSUNG_NOW_BAR_GUIDE_URL)
    }

    private fun openPrivacyPolicy() {
        openWebPage(PRIVACY_POLICY_URL)
    }

    private fun openWebPage(url: String) {
        CustomTabsIntent.Builder()
            .setShowTitle(true)
            .build()
            .launchUrl(this, url.toUri())
    }

    private fun isSamsungDevice(): Boolean =
        Build.MANUFACTURER.equals("samsung", ignoreCase = true) ||
            Build.BRAND.equals("samsung", ignoreCase = true)

    private fun isXiaomiDevice(): Boolean =
        isXiaomiFamily(Build.MANUFACTURER) || isXiaomiFamily(Build.BRAND)

    private fun isXiaomiFamily(value: String?): Boolean {
        val normalized = value?.lowercase()?.trim().orEmpty()
        return normalized == "xiaomi" || normalized == "redmi" || normalized == "poco"
    }

    private fun openIpass() = openPackage(IPASS_PACKAGE, "iPASS MONEY")

    private fun openClock() = openPackage(CLOCK_PACKAGE, "Clock")

    private fun openFoodpanda() = openPackage(FOODPANDA_PACKAGE, "foodpanda")

    private fun openUber() = openPackage(UBER_PACKAGE, "Uber")

    private fun openUberEats() = openPackage(UBER_EATS_PACKAGE, "Uber Eats")

    private fun openPikminBloom() = openPackage(PIKMIN_BLOOM_PACKAGE, "Pikmin Bloom")

    private fun openPackage(targetPackageName: String, appName: String) {
        packageManager.getLaunchIntentForPackage(targetPackageName)?.let {
            startActivity(it)
            return
        }
        Toast.makeText(this, "尚未安裝 $appName，將開啟 Google Play。", Toast.LENGTH_LONG).show()
        startActivity(
            Intent(Intent.ACTION_VIEW, "market://details?id=$targetPackageName".toUri()),
        )
    }

    private fun setAppEnabled(app: AppReminderPreferences.App, enabled: Boolean) {
        app.setEnabled(this, enabled)
        if (!enabled) clearReminder(app)
        refreshStatus()
    }

    private fun clearReminder(app: AppReminderPreferences.App) {
        when (app) {
            AppReminderPreferences.App.CLOCK -> LiveStatusReminder.clearClockTimer(this)
            AppReminderPreferences.App.IPASS -> LiveStatusReminder.clear(this)
            AppReminderPreferences.App.FOODPANDA -> LiveStatusReminder.clearFoodpanda(this)
            AppReminderPreferences.App.UBER_RIDE -> LiveStatusReminder.clearUberRide(this)
            AppReminderPreferences.App.UBER_EATS -> LiveStatusReminder.clearUberEats(this)
            AppReminderPreferences.App.PIKMIN_BLOOM -> LiveStatusReminder.clearPikminBloom(this)
        }
    }

    private fun dismissNowBarTroubleshooting() {
        AppReminderPreferences.setNowBarTroubleshootingDismissed(this, true)
        refreshStatus()
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 7
        private const val ACTION_OPEN_CLOCK =
            "com.github.jimmy90109.livestatus.action.OPEN_CLOCK"
        private const val ACTION_OPEN_IPASS =
            "com.github.jimmy90109.livestatus.action.OPEN_IPASS"
        private const val ACTION_OPEN_FOODPANDA =
            "com.github.jimmy90109.livestatus.action.OPEN_FOODPANDA"
        private const val ACTION_OPEN_UBER =
            "com.github.jimmy90109.livestatus.action.OPEN_UBER"
        private const val ACTION_OPEN_UBER_EATS =
            "com.github.jimmy90109.livestatus.action.OPEN_UBER_EATS"
        private const val ACTION_OPEN_PIKMIN_BLOOM =
            "com.github.jimmy90109.livestatus.action.OPEN_PIKMIN_BLOOM"
        private const val CLOCK_PACKAGE = ClockTimerNotificationExtractor.CLOCK_PACKAGE
        private const val IPASS_PACKAGE = "com.ipass.ipassmoney"
        private const val FOODPANDA_PACKAGE = "com.global.foodpanda.android"
        private const val UBER_PACKAGE = "com.ubercab"
        private const val UBER_EATS_PACKAGE = "com.ubercab.eats"
        private const val PIKMIN_BLOOM_PACKAGE = "com.nianticlabs.pikmin"
        private const val SAMSUNG_NOW_BAR_GUIDE_URL =
            "https://jimmy90109.github.io/live-status-reminder/samsung-now-bar.html"
        private const val PRIVACY_POLICY_URL =
            "https://jimmy90109.github.io/live-status-reminder/"

        @JvmStatic
        fun createOpenClockIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_CLOCK)

        @JvmStatic
        fun createOpenIpassIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_IPASS)

        @JvmStatic
        fun createOpenFoodpandaIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_FOODPANDA)

        @JvmStatic
        fun createOpenUberIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_UBER)

        @JvmStatic
        fun createOpenUberEatsIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_UBER_EATS)

        @JvmStatic
        fun createOpenPikminBloomIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_PIKMIN_BLOOM)

        private fun openAppIntent(context: Context, action: String): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

internal data class StatusSnapshot(
    val notificationAccess: Boolean = false,
    val notificationPermission: Boolean = false,
    val clockInstalled: Boolean = false,
    val ipassInstalled: Boolean = false,
    val foodpandaInstalled: Boolean = false,
    val uberInstalled: Boolean = false,
    val uberEatsInstalled: Boolean = false,
    val pikminBloomInstalled: Boolean = false,
    val isSamsungDevice: Boolean = false,
    val isXiaomiDevice: Boolean = false,
    val nowBarTroubleshootingDismissed: Boolean = false,
    val clockEnabled: Boolean = false,
    val ipassEnabled: Boolean = false,
    val foodpandaEnabled: Boolean = false,
    val uberEnabled: Boolean = false,
    val uberEatsEnabled: Boolean = false,
    val pikminBloomEnabled: Boolean = false,
) {
    val requiredSettingsComplete: Boolean
        get() = notificationAccess && notificationPermission
}

private enum class DebugTarget(val appName: String) {
    CLOCK("Clock"),
    UBER("Uber"),
    FOODPANDA("foodpanda"),
    UBER_EATS("Uber Eats"),
}

@Composable
private fun HomeScreenHostActivity.MainScreen(
    status: StatusSnapshot,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSamsungNowBarGuide: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
    onDismissNowBarTroubleshooting: () -> Unit,
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
    val scrollTopPadding = safeDrawingPadding.calculateTopPadding() + 20.dp
    val scrollBottomPadding = safeDrawingPadding.calculateBottomPadding() + 32.dp

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
                    DebugTarget.CLOCK -> NotificationDebugPayloadStore.clockPayloads
                    DebugTarget.UBER -> NotificationDebugPayloadStore.uberPayloads
                    DebugTarget.FOODPANDA -> NotificationDebugPayloadStore.foodpandaPayloads
                    DebugTarget.UBER_EATS -> NotificationDebugPayloadStore.uberEatsPayloads
                },
                cardColor = when (currentDebugTarget) {
                    DebugTarget.CLOCK -> colors.commonContainer
                    DebugTarget.UBER -> colors.commonContainer
                    DebugTarget.FOODPANDA -> colors.foodpandaContainer
                    DebugTarget.UBER_EATS -> colors.uberEatsContainer
                },
                actionColor = when (currentDebugTarget) {
                    DebugTarget.CLOCK -> colors.onSurface
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
                        DebugTarget.CLOCK -> NotificationDebugPayloadStore.clearClock()
                        DebugTarget.UBER -> NotificationDebugPayloadStore.clearUber()
                        DebugTarget.FOODPANDA -> NotificationDebugPayloadStore.clearFoodpanda()
                        DebugTarget.UBER_EATS -> NotificationDebugPayloadStore.clearUberEats()
                    }
                },
            )
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
                        onOpenSettings = { openSettingsPage() },
                        onToggleSettings = { settingsExpanded = !settingsExpanded },
                        onOpenNotificationAccess = {
                            showNotificationAccessDisclosure = true
                        },
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onOpenSamsungNowBarGuide = onOpenSamsungNowBarGuide,
                        onDismissNowBarTroubleshooting = onDismissNowBarTroubleshooting,
                        onAppEnabledChange = onAppEnabledChange,
                        onOpenClockDebug = { debugTarget = DebugTarget.CLOCK },
                        onOpenFoodpandaDebug = { debugTarget = DebugTarget.FOODPANDA },
                        onOpenUberDebug = { debugTarget = DebugTarget.UBER },
                        onOpenUberEatsDebug = { debugTarget = DebugTarget.UBER_EATS },
                    )
                }
            } else {
                HomeBackgroundLayer(progress = settingsProgress.value) {
                    HomeContentNarrow(
                        status = status,
                        settingsExpanded = settingsExpanded,
                        scrollTopPadding = scrollTopPadding,
                        scrollBottomPadding = scrollBottomPadding,
                        onOpenSettings = { openSettingsPage() },
                        onToggleSettings = { settingsExpanded = !settingsExpanded },
                        onOpenNotificationAccess = {
                            showNotificationAccessDisclosure = true
                        },
                        onRequestNotificationPermission = onRequestNotificationPermission,
                        onOpenSamsungNowBarGuide = onOpenSamsungNowBarGuide,
                        onDismissNowBarTroubleshooting = onDismissNowBarTroubleshooting,
                        onAppEnabledChange = onAppEnabledChange,
                        onOpenClockDebug = { debugTarget = DebugTarget.CLOCK },
                        onOpenFoodpandaDebug = { debugTarget = DebugTarget.FOODPANDA },
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
private fun HomeBackgroundLayer(
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
private fun HomeContentWide(
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
private fun HomeContentNarrow(
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
                "即時狀態提醒會讀取 Clock、iPASS MONEY、foodpanda、Uber、Uber Eats 與 Pikmin Bloom 的通知內容，" +
                    "用來辨識倒數計時、乘車、外送進度、Uber / Uber Eats PIN 與 Pikmin Bloom 種花狀態，並在本機產生提醒。\n\n" +
                    "通知內容只在您的裝置上即時處理；App 不會上傳、出售或分享這些資料，" +
                    "也不會永久儲存通知內容或 PIN。您隨時可以在系統設定中關閉通知存取權限。",
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
