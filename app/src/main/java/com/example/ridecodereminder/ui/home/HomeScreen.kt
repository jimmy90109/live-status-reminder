package com.example.ridecodereminder.ui.home

import android.Manifest
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridecodereminder.RideNotificationListenerService
import com.example.ridecodereminder.RideNotificationParser
import com.example.ridecodereminder.RideReminder
import com.example.ridecodereminder.ui.theme.LocalAppColors
import com.example.ridecodereminder.ui.theme.RideCodeTheme
import kotlinx.coroutines.launch

open class HomeScreenHostActivity : ComponentActivity() {
    private var statusSnapshot by mutableStateOf(StatusSnapshot())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        RideReminder.createChannel(this)

        when (intent.action) {
            ACTION_OPEN_IPASS -> openIpass()
            ACTION_OPEN_FOODPANDA -> openFoodpanda()
            ACTION_OPEN_UBER_EATS -> openUberEats()
            else -> {
                setContent {
                    RideCodeTheme {
                        MainScreen(
                            status = statusSnapshot,
                            onOpenNotificationAccess = ::openNotificationListenerSettings,
                            onRequestNotificationPermission = ::requestNotificationPermission,
                            onOpenLiveUpdateSettings = ::openLiveUpdateSettings,
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
        statusSnapshot = StatusSnapshot(
            notificationAccess = isNotificationAccessEnabled(),
            notificationPermission = canPostNotifications(),
            liveUpdates = getSystemService(NotificationManager::class.java)
                .canPostPromotedNotifications(),
            ipassInstalled = isPackageInstalled(IPASS_PACKAGE),
            foodpandaInstalled = isPackageInstalled(FOODPANDA_PACKAGE),
            uberEatsInstalled = isPackageInstalled(UBER_EATS_PACKAGE),
        )
    }

    private fun isNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners",
        )
        val listener = ComponentName(this, RideNotificationListenerService::class.java)
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

    private fun openLiveUpdateSettings() {
        val settingsIntent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        if (settingsIntent.resolveActivity(packageManager) != null) {
            startActivity(settingsIntent)
        } else {
            Toast.makeText(this, "這台裝置沒有提供 Live Update 設定頁。", Toast.LENGTH_LONG).show()
        }
    }

    private fun openIpass() = openPackage(IPASS_PACKAGE, "iPASS MONEY")

    private fun openFoodpanda() = openPackage(FOODPANDA_PACKAGE, "foodpanda")

    private fun openUberEats() = openPackage(UBER_EATS_PACKAGE, "Uber Eats")

    private fun openPackage(targetPackageName: String, appName: String) {
        packageManager.getLaunchIntentForPackage(targetPackageName)?.let {
            startActivity(it)
            return
        }
        Toast.makeText(this, "尚未安裝 $appName，將開啟 Google Play。", Toast.LENGTH_LONG).show()
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$targetPackageName")),
        )
    }

    companion object {
        private const val REQUEST_NOTIFICATIONS = 7
        private const val ACTION_OPEN_IPASS =
            "com.example.ridecodereminder.action.OPEN_IPASS"
        private const val ACTION_OPEN_FOODPANDA =
            "com.example.ridecodereminder.action.OPEN_FOODPANDA"
        private const val ACTION_OPEN_UBER_EATS =
            "com.example.ridecodereminder.action.OPEN_UBER_EATS"
        private const val IPASS_PACKAGE = "com.ipass.ipassmoney"
        private const val FOODPANDA_PACKAGE = "com.global.foodpanda.android"
        private const val UBER_EATS_PACKAGE = "com.ubercab.eats"

        @JvmStatic
        fun createOpenIpassIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_IPASS)

        @JvmStatic
        fun createOpenFoodpandaIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_FOODPANDA)

        @JvmStatic
        fun createOpenUberEatsIntent(context: Context): Intent =
            openAppIntent(context, ACTION_OPEN_UBER_EATS)

        private fun openAppIntent(context: Context, action: String): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(action)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

internal data class StatusSnapshot(
    val notificationAccess: Boolean = false,
    val notificationPermission: Boolean = false,
    val liveUpdates: Boolean = false,
    val ipassInstalled: Boolean = false,
    val foodpandaInstalled: Boolean = false,
    val uberEatsInstalled: Boolean = false,
) {
    val requiredSettingsComplete: Boolean
        get() = notificationAccess && notificationPermission && liveUpdates
}

@Composable
private fun HomeScreenHostActivity.MainScreen(
    status: StatusSnapshot,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenLiveUpdateSettings: () -> Unit,
) {
    var settingsExpanded by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(status.requiredSettingsComplete) {
        settingsExpanded = !status.requiredSettingsComplete
    }
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val scrollTopPadding = safeDrawingPadding.calculateTopPadding() + 20.dp
    val scrollBottomPadding = safeDrawingPadding.calculateBottomPadding() + 32.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LocalAppColors.current.background,
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal)),
            contentAlignment = Alignment.TopStart,
        ) {
            if (maxWidth > maxHeight) {
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
                        HeroCard()
                        Spacer(Modifier.height(28.dp))
                        RequiredSettingsSection(
                            status = status,
                            expanded = settingsExpanded,
                            onToggle = { settingsExpanded = !settingsExpanded },
                            onOpenNotificationAccess = onOpenNotificationAccess,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onOpenLiveUpdateSettings = onOpenLiveUpdateSettings,
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
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = scrollTopPadding, bottom = scrollBottomPadding),
                    verticalArrangement = Arrangement.Top,
                ) {
                    Column(Modifier.padding(horizontal = 20.dp)) {
                        HeroCard()
                        Spacer(Modifier.height(28.dp))
                        RequiredSettingsSection(
                            status = status,
                            expanded = settingsExpanded,
                            onToggle = { settingsExpanded = !settingsExpanded },
                            onOpenNotificationAccess = onOpenNotificationAccess,
                            onRequestNotificationPermission = onRequestNotificationPermission,
                            onOpenLiveUpdateSettings = onOpenLiveUpdateSettings,
                        )
                    }
                    Spacer(Modifier.height(28.dp))
                    AppsSection(
                        status = status,
                        horizontalContentPadding = 20.dp,
                    )
                }
            }
        }
    }
}

@Composable
private fun RequiredSettingsSection(
    status: StatusSnapshot,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenLiveUpdateSettings: () -> Unit,
) {
    Column {
        SectionHeader(
            title = "必要設定",
            subtitle = "完成設定後，App 就能把乘車與外送狀態變成即時通知。",
            expanded = expanded,
            collapsible = status.requiredSettingsComplete,
            onToggle = onToggle,
        )
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(visible = expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingCard(
                    number = "01",
                    title = "讀取狀態通知",
                    description = "允許 App 辨識 iPASS MONEY、foodpanda 與 Uber Eats 狀態。",
                    enabled = status.notificationAccess,
                    enabledText = "已開啟",
                    disabledText = "尚未開啟",
                    action = "開啟通知存取權限",
                    onClick = onOpenNotificationAccess,
                )
                SettingCard(
                    number = "02",
                    title = "顯示即時通知",
                    description = "讓提醒固定顯示在通知列，點一下就能開啟對應 App。",
                    enabled = status.notificationPermission,
                    enabledText = "已允許",
                    disabledText = "尚未允許",
                    action = "允許提醒通知",
                    onClick = onRequestNotificationPermission,
                )
                SettingCard(
                    number = "03",
                    title = "Live Update",
                    description = "允許乘車與外送狀態顯示為 Android 16 即時通知與狀態列 chip。",
                    enabled = status.liveUpdates,
                    enabledText = "系統允許顯示",
                    disabledText = "未允許，將使用一般通知",
                    action = "開啟 Live Update 設定",
                    onClick = onOpenLiveUpdateSettings,
                )
            }
        }
    }
}
