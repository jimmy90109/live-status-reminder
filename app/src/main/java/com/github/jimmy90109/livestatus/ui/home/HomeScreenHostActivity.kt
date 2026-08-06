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
import com.github.jimmy90109.livestatus.TaiwanTaxiRideManager
import com.github.jimmy90109.livestatus.YouBikeRideManager
import com.github.jimmy90109.livestatus.YptStudyNotificationParser
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
            ACTION_OPEN_TAIWAN_PAY -> openTaiwanPay()
            ACTION_OPEN_YOU_BIKE -> openYouBike()
            ACTION_OPEN_FOODPANDA -> openFoodpanda()
            ACTION_OPEN_TAIWAN_TAXI -> openTaiwanTaxi()
            ACTION_OPEN_UBER -> openUber()
            ACTION_OPEN_UBER_EATS -> openUberEats()
            ACTION_OPEN_PIKMIN_BLOOM -> openPikminBloom()
            ACTION_OPEN_YPT -> openYpt()
            else -> {
                setContent {
                    LiveStatusTheme {
                        MainScreen(
                            status = statusSnapshot,
                            onOpenNotificationAccess = ::openNotificationListenerSettings,
                            onRequestNotificationPermission = ::requestNotificationPermission,
                            onOpenSamsungNowBarGuide = ::openSamsungNowBarGuide,
                            onOpenPrivacyPolicy = ::openPrivacyPolicy,
                            onDismissBrandWarning = ::dismissBrandWarning,
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
        YouBikeRideManager.restore(this)
    }

    private fun refreshStatus() {
        val clockInstalled = isPackageInstalled(CLOCK_PACKAGE)
        val ipassInstalled = isPackageInstalled(IPASS_PACKAGE)
        val taiwanPayInstalled = isPackageInstalled(TAIWAN_PAY_PACKAGE)
        val youBikeInstalled = isPackageInstalled(YOU_BIKE_PACKAGE)
        val foodpandaInstalled = isPackageInstalled(FOODPANDA_PACKAGE)
        val taiwanTaxiInstalled = isPackageInstalled(TAIWAN_TAXI_PACKAGE)
        val uberInstalled = isPackageInstalled(UBER_PACKAGE)
        val uberEatsInstalled = isPackageInstalled(UBER_EATS_PACKAGE)
        val pikminBloomInstalled = isPackageInstalled(PIKMIN_BLOOM_PACKAGE)
        val yptInstalled = isPackageInstalled(YPT_PACKAGE)
        val brandWarning = detectBrandWarning()
        statusSnapshot = StatusSnapshot(
            notificationAccess = isNotificationAccessEnabled(),
            notificationPermission = canPostNotifications(),
            clockInstalled = clockInstalled,
            ipassInstalled = ipassInstalled,
            taiwanPayInstalled = taiwanPayInstalled,
            youBikeInstalled = youBikeInstalled,
            foodpandaInstalled = foodpandaInstalled,
            taiwanTaxiInstalled = taiwanTaxiInstalled,
            uberInstalled = uberInstalled,
            uberEatsInstalled = uberEatsInstalled,
            pikminBloomInstalled = pikminBloomInstalled,
            yptInstalled = yptInstalled,
            brandWarning = brandWarning,
            brandWarningDismissed =
                AppReminderPreferences.isBrandWarningDismissed(this),
            mediaPlaybackEnabled = AppReminderPreferences.App.MEDIA_PLAYBACK.isEnabled(this),
            clockEnabled = AppReminderPreferences.App.CLOCK.isEnabled(this, clockInstalled),
            ipassEnabled = AppReminderPreferences.App.IPASS.isEnabled(this, ipassInstalled),
            taiwanPayEnabled =
                AppReminderPreferences.App.TAIWAN_PAY.isEnabled(this, taiwanPayInstalled),
            youBikeEnabled =
                AppReminderPreferences.App.YOUBIKE.isEnabled(this, youBikeInstalled),
            youBikeExactAlarmAllowed = YouBikeRideManager.canScheduleExactAlarms(this),
            foodpandaEnabled = AppReminderPreferences.App.FOODPANDA.isEnabled(this, foodpandaInstalled),
            taiwanTaxiEnabled =
                AppReminderPreferences.App.TAIWAN_TAXI.isEnabled(this, taiwanTaxiInstalled),
            uberEnabled = AppReminderPreferences.App.UBER_RIDE.isEnabled(this, uberInstalled),
            uberEatsEnabled = AppReminderPreferences.App.UBER_EATS.isEnabled(this, uberEatsInstalled),
            pikminBloomEnabled =
                AppReminderPreferences.App.PIKMIN_BLOOM.isEnabled(this, pikminBloomInstalled),
            yptEnabled = AppReminderPreferences.App.YPT.isEnabled(this, yptInstalled),
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

    private fun detectBrandWarning(): BrandWarning? = when {
        isSamsungDevice() -> BrandWarning.SAMSUNG_NOW_BAR
        isXiaomiDevice() -> BrandWarning.XIAOMI_HYPER_ISLAND
        else -> null
    }

    private fun isXiaomiFamily(value: String?): Boolean {
        val normalized = value?.lowercase()?.trim().orEmpty()
        return normalized == "xiaomi" || normalized == "redmi" || normalized == "poco"
    }

    private fun openIpass() = openPackage(IPASS_PACKAGE, "iPASS MONEY")

    private fun openTaiwanPay() = openPackage(TAIWAN_PAY_PACKAGE, "台灣行動支付")

    private fun openClock() = openPackage(CLOCK_PACKAGE, "Clock")

    private fun openYouBike() = openPackage(YOU_BIKE_PACKAGE, "YouBike")

    private fun openFoodpanda() = openPackage(FOODPANDA_PACKAGE, "foodpanda")

    private fun openTaiwanTaxi() = openPackage(TAIWAN_TAXI_PACKAGE, "55688")

    private fun openUber() = openPackage(UBER_PACKAGE, "Uber")

    private fun openUberEats() = openPackage(UBER_EATS_PACKAGE, "Uber Eats")

    private fun openPikminBloom() = openPackage(PIKMIN_BLOOM_PACKAGE, "Pikmin Bloom")

    private fun openYpt() = openPackage(YPT_PACKAGE, "YPT")

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
            AppReminderPreferences.App.MEDIA_PLAYBACK -> LiveStatusReminder.clearMediaPlayback(this)
            AppReminderPreferences.App.CLOCK -> LiveStatusReminder.clearClockTimer(this)
            AppReminderPreferences.App.IPASS -> LiveStatusReminder.clear(this)
            AppReminderPreferences.App.TAIWAN_PAY -> LiveStatusReminder.clearTaiwanPay(this)
            AppReminderPreferences.App.YOUBIKE -> YouBikeRideManager.clear(this)
            AppReminderPreferences.App.FOODPANDA -> LiveStatusReminder.clearFoodpanda(this)
            AppReminderPreferences.App.TAIWAN_TAXI -> TaiwanTaxiRideManager.clear(this)
            AppReminderPreferences.App.UBER_RIDE -> LiveStatusReminder.clearUberRide(this)
            AppReminderPreferences.App.UBER_EATS -> LiveStatusReminder.clearUberEats(this)
            AppReminderPreferences.App.PIKMIN_BLOOM -> LiveStatusReminder.clearPikminBloom(this)
            AppReminderPreferences.App.YPT -> LiveStatusReminder.clearYptStudy(this)
        }
    }

    private fun dismissBrandWarning() {
        AppReminderPreferences.setBrandWarningDismissed(this, true)
        refreshStatus()
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 7
        private const val ACTION_OPEN_CLOCK =
            "com.github.jimmy90109.livestatus.action.OPEN_CLOCK"
        private const val ACTION_OPEN_IPASS =
            "com.github.jimmy90109.livestatus.action.OPEN_IPASS"
        private const val ACTION_OPEN_TAIWAN_PAY =
            "com.github.jimmy90109.livestatus.action.OPEN_TAIWAN_PAY"
        private const val ACTION_OPEN_YOU_BIKE =
            "com.github.jimmy90109.livestatus.action.OPEN_YOUBIKE"
        private const val ACTION_OPEN_FOODPANDA =
            "com.github.jimmy90109.livestatus.action.OPEN_FOODPANDA"
        private const val ACTION_OPEN_TAIWAN_TAXI =
            "com.github.jimmy90109.livestatus.action.OPEN_TAIWAN_TAXI"
        private const val ACTION_OPEN_UBER =
            "com.github.jimmy90109.livestatus.action.OPEN_UBER"
        private const val ACTION_OPEN_UBER_EATS =
            "com.github.jimmy90109.livestatus.action.OPEN_UBER_EATS"
        private const val ACTION_OPEN_PIKMIN_BLOOM =
            "com.github.jimmy90109.livestatus.action.OPEN_PIKMIN_BLOOM"
        private const val ACTION_OPEN_YPT =
            "com.github.jimmy90109.livestatus.action.OPEN_YPT"
        private const val CLOCK_PACKAGE = ClockTimerNotificationExtractor.CLOCK_PACKAGE
        private const val IPASS_PACKAGE = "com.ipass.ipassmoney"
        private const val TAIWAN_PAY_PACKAGE = "tw.com.twmp.twhcewallet"
        private const val YOU_BIKE_PACKAGE = "tw.com.youbike.plus"
        private const val FOODPANDA_PACKAGE = "com.global.foodpanda.android"
        private const val TAIWAN_TAXI_PACKAGE = "dbx.taiwantaxi"
        private const val UBER_PACKAGE = "com.ubercab"
        private const val UBER_EATS_PACKAGE = "com.ubercab.eats"
        private const val PIKMIN_BLOOM_PACKAGE = "com.nianticlabs.pikmin"
        private const val YPT_PACKAGE = YptStudyNotificationParser.PACKAGE_NAME
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
        fun createOpenTaiwanPayIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_TAIWAN_PAY)

        @JvmStatic
        fun createOpenYouBikeIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_YOU_BIKE)

        @JvmStatic
        fun createOpenFoodpandaIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_FOODPANDA)

        @JvmStatic
        fun createOpenTaiwanTaxiIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_TAIWAN_TAXI)

        @JvmStatic
        fun createOpenUberIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_UBER)

        @JvmStatic
        fun createOpenUberEatsIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_UBER_EATS)

        @JvmStatic
        fun createOpenPikminBloomIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_PIKMIN_BLOOM)

        @JvmStatic
        fun createOpenYptIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_YPT)

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
    val taiwanPayInstalled: Boolean = false,
    val youBikeInstalled: Boolean = false,
    val foodpandaInstalled: Boolean = false,
    val taiwanTaxiInstalled: Boolean = false,
    val uberInstalled: Boolean = false,
    val uberEatsInstalled: Boolean = false,
    val pikminBloomInstalled: Boolean = false,
    val yptInstalled: Boolean = false,
    val brandWarning: BrandWarning? = null,
    val brandWarningDismissed: Boolean = false,
    val mediaPlaybackEnabled: Boolean = false,
    val clockEnabled: Boolean = false,
    val ipassEnabled: Boolean = false,
    val taiwanPayEnabled: Boolean = false,
    val youBikeEnabled: Boolean = false,
    val youBikeExactAlarmAllowed: Boolean = false,
    val foodpandaEnabled: Boolean = false,
    val taiwanTaxiEnabled: Boolean = false,
    val uberEnabled: Boolean = false,
    val uberEatsEnabled: Boolean = false,
    val pikminBloomEnabled: Boolean = false,
    val yptEnabled: Boolean = false,
) {
    val requiredSettingsComplete: Boolean
        get() = notificationAccess && notificationPermission
}
