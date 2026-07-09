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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ridecodereminder.RideNotificationListenerService
import com.example.ridecodereminder.RideNotificationParser
import com.example.ridecodereminder.RideReminder
import com.example.ridecodereminder.ui.theme.AppColors
import com.example.ridecodereminder.ui.theme.RideCodeTheme

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

private data class StatusSnapshot(
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
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_IPASS) }
    var settingsExpanded by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(status.requiredSettingsComplete) {
        settingsExpanded = !status.requiredSettingsComplete
    }
    val safeDrawingPadding = WindowInsets.safeDrawing.asPaddingValues()
    val scrollTopPadding = safeDrawingPadding.calculateTopPadding() + 20.dp
    val scrollBottomPadding = safeDrawingPadding.calculateBottomPadding() + 32.dp

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = color(AppColors.BACKGROUND),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
                .padding(horizontal = 20.dp),
            contentAlignment = Alignment.TopStart,
        ) {
            if (maxWidth > maxHeight) {
                Row(
                    modifier = Modifier.fillMaxSize(),
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
                            selectedTab = selectedTab,
                            onSelectTab = { selectedTab = it },
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
                    Spacer(Modifier.height(28.dp))
                    AppsSection(
                        status = status,
                        selectedTab = selectedTab,
                        onSelectTab = { selectedTab = it },
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

@Composable
private fun HomeScreenHostActivity.AppsSection(
    status: StatusSnapshot,
    selectedTab: Int,
    onSelectTab: (Int) -> Unit,
) {
    Column {
        SectionHeader(
            title = "App",
            subtitle = "檢查安裝狀態，並分別測試各 App 的即時通知。",
        )
        Spacer(Modifier.height(12.dp))
        AppTabs(selectedTab = selectedTab, onSelect = onSelectTab)
        Spacer(Modifier.height(12.dp))
        when (selectedTab) {
            TAB_FOODPANDA -> FoodpandaCard(status.foodpandaInstalled)
            TAB_UBER_EATS -> UberEatsCard(status.uberEatsInstalled)
            else -> IpassCard(status.ipassInstalled)
        }
    }
}

@Composable
private fun HeroCard() {
    CardSurface(color(AppColors.COMMON_CONTAINER), 36) {
        LabelPill("LIVE STATUS", color(AppColors.COMMON_PRIMARY), Color.White)
        Spacer(Modifier.height(34.dp))
        AppText("重要狀態，\n留在最前面。", 34, AppColors.ON_SURFACE, true)
        Spacer(Modifier.height(12.dp))
        AppText(
            "進站後顯示乘車碼捷徑；外送期間顯示 foodpanda 或 Uber Eats 訂單進度。",
            16,
            AppColors.ON_SURFACE_VARIANT,
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    expanded: Boolean = true,
    collapsible: Boolean = false,
    onToggle: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (collapsible) {
                    Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Button, onClick = onToggle)
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            AppText(title, 24, AppColors.ON_SURFACE, true)
            Spacer(Modifier.height(4.dp))
            AppText(subtitle, 15, AppColors.ON_SURFACE_VARIANT)
        }
        if (collapsible) {
            Text(
                text = if (expanded) "⌃" else "⌄",
                color = color(AppColors.ON_SURFACE_VARIANT),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
    }
}

@Composable
private fun SettingCard(
    number: String,
    title: String,
    description: String,
    enabled: Boolean,
    enabledText: String,
    disabledText: String,
    action: String,
    onClick: () -> Unit,
) {
    CardSurface(color(AppColors.COMMON_SURFACE), 26, 18) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelPill(number, color(AppColors.COMMON_CONTAINER), color(AppColors.ON_SURFACE))
            Spacer(Modifier.padding(horizontal = 5.dp))
            AppText(title, 20, AppColors.ON_SURFACE, true)
        }
        Spacer(Modifier.height(12.dp))
        AppText(description, 15, AppColors.ON_SURFACE_VARIANT)
        Spacer(Modifier.height(14.dp))
        StatusPill(enabled, enabledText, disabledText)
        Spacer(Modifier.height(14.dp))
        ActionButton("$action  →", AppColors.COMMON_PRIMARY, android.graphics.Color.WHITE, onClick)
    }
}

@Composable
private fun AppTabs(selectedTab: Int, onSelect: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(color(AppColors.COMMON_SURFACE), RoundedCornerShape(100.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AppTab("iPASS MONEY", TAB_IPASS, selectedTab, AppColors.IPASS_PRIMARY, onSelect)
        AppTab("foodpanda", TAB_FOODPANDA, selectedTab, AppColors.FOODPANDA_PRIMARY, onSelect)
        AppTab("Uber Eats", TAB_UBER_EATS, selectedTab, AppColors.UBER_EATS_PRIMARY, onSelect)
    }
}

@Composable
private fun RowScope.AppTab(
    label: String,
    tab: Int,
    selectedTab: Int,
    selectedColor: Int,
    onSelect: (Int) -> Unit,
) {
    val selected = tab == selectedTab
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier = Modifier
            .weight(1f)
            .background(
                if (selected) color(selectedColor) else Color.Transparent,
                shape,
            )
            .clip(shape)
            .clickable(role = Role.Tab) { onSelect(tab) }
            .padding(horizontal = 8.dp, vertical = 11.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else color(AppColors.ON_SURFACE_VARIANT),
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun HomeScreenHostActivity.IpassCard(installed: Boolean) {
    AppCard(
        appName = "iPASS MONEY",
        title = "乘車碼狀態",
        description = "進站後顯示乘車碼捷徑，準備下車時快速開啟。",
        installed = installed,
        cardColor = AppColors.IPASS_CONTAINER,
        labelColor = AppColors.IPASS_SECONDARY_CONTAINER,
        foregroundColor = AppColors.ON_SURFACE,
    ) {
        ActionButton("模擬上車，顯示提醒  ↑", AppColors.IPASS_PRIMARY, android.graphics.Color.WHITE) {
            RideReminder.show(this@IpassCard)
        }
        ActionButton("模擬下車，移除提醒  ✓", AppColors.IPASS_TERTIARY_CONTAINER, AppColors.ON_SURFACE) {
            RideReminder.clear(this@IpassCard)
        }
    }
}

@Composable
private fun HomeScreenHostActivity.FoodpandaCard(installed: Boolean) {
    AppCard(
        appName = "foodpanda",
        title = "外送訂單狀態",
        description = "外送夥伴出發或即將抵達時，顯示取餐提醒。",
        installed = installed,
        cardColor = AppColors.FOODPANDA_CONTAINER,
        labelColor = AppColors.FOODPANDA_SECONDARY_CONTAINER,
        foregroundColor = AppColors.FOODPANDA_TEXT,
    ) {
        ActionButton("模擬外送中", AppColors.FOODPANDA_PRIMARY, android.graphics.Color.WHITE) {
            RideReminder.showFoodpanda(
                this@FoodpandaCard,
                RideNotificationParser.FoodpandaEvent.COURIER_ON_THE_WAY,
            )
        }
        ActionButton("模擬即將抵達", AppColors.FOODPANDA_SECONDARY_CONTAINER, AppColors.FOODPANDA_TEXT) {
            RideReminder.showFoodpanda(
                this@FoodpandaCard,
                RideNotificationParser.FoodpandaEvent.COURIER_ARRIVING,
            )
        }
        ActionButton("清除 foodpanda 狀態  ✓", AppColors.FOODPANDA_SECONDARY_CONTAINER, AppColors.FOODPANDA_TEXT) {
            RideReminder.clearFoodpanda(this@FoodpandaCard)
        }
    }
}

@Composable
private fun HomeScreenHostActivity.UberEatsCard(installed: Boolean) {
    AppCard(
        appName = "Uber Eats",
        title = "五階段訂單進度",
        description = "從接單到即將抵達持續更新；可辨識時也會顯示四位數 PIN。",
        installed = installed,
        cardColor = AppColors.UBER_EATS_CONTAINER,
        labelColor = AppColors.UBER_EATS_SECONDARY_CONTAINER,
        foregroundColor = AppColors.UBER_EATS_TEXT,
    ) {
        UberEatsTestButton("模擬訂單已收到", RideNotificationParser.UberEatsEvent.ORDER_RECEIVED)
        UberEatsTestButton("模擬正在準備訂單", RideNotificationParser.UberEatsEvent.PREPARING)
        UberEatsTestButton("模擬正在取餐", RideNotificationParser.UberEatsEvent.PICKING_UP)
        UberEatsTestButton("模擬配送中", RideNotificationParser.UberEatsEvent.ON_THE_WAY)
        UberEatsTestButton(
            "模擬快到了（PIN 7616）",
            RideNotificationParser.UberEatsEvent.ARRIVING,
            "7616",
        )
        ActionButton("模擬送達，清除狀態  ✓", AppColors.UBER_EATS_SECONDARY_CONTAINER, AppColors.UBER_EATS_TEXT) {
            RideReminder.clearUberEats(this@UberEatsCard)
        }
    }
}

@Composable
private fun HomeScreenHostActivity.UberEatsTestButton(
    label: String,
    event: RideNotificationParser.UberEatsEvent,
    pin: String? = null,
) {
    val primary = event == RideNotificationParser.UberEatsEvent.ORDER_RECEIVED
    ActionButton(
        label,
        if (primary) AppColors.UBER_EATS_PRIMARY else AppColors.UBER_EATS_SECONDARY_CONTAINER,
        if (primary) android.graphics.Color.WHITE else AppColors.UBER_EATS_TEXT,
    ) {
        RideReminder.showUberEats(this, event, pin)
    }
}

@Composable
private fun AppCard(
    appName: String,
    title: String,
    description: String,
    installed: Boolean,
    cardColor: Int,
    labelColor: Int,
    foregroundColor: Int,
    actions: @Composable ColumnScope.() -> Unit,
) {
    CardSurface(color(cardColor), 30, 18) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
        ) {
            LabelPill(appName, color(labelColor), color(foregroundColor))
            Spacer(Modifier.weight(1f))
            StatusPill(installed, "已安裝", "尚未安裝")
        }
        Spacer(Modifier.height(12.dp))
        AppText(title, 20, AppColors.ON_SURFACE, true)
        Spacer(Modifier.height(6.dp))
        AppText(description, 15, AppColors.ON_SURFACE_VARIANT)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
private fun CardSurface(
    background: Color,
    radius: Int,
    padding: Int = 22,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, RoundedCornerShape(radius.dp))
            .padding(padding.dp),
        content = content,
    )
}

@Composable
private fun LabelPill(label: String, background: Color, foreground: Color) {
    Text(
        text = label,
        color = foreground,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(background, RoundedCornerShape(100.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun StatusPill(enabled: Boolean, enabledText: String, disabledText: String) {
    Text(
        text = "${if (enabled) "✓" else "•"}  ${if (enabled) enabledText else disabledText}",
        color = color(if (enabled) AppColors.SUCCESS_TEXT else AppColors.WARNING_TEXT),
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(
                color(if (enabled) AppColors.SUCCESS_CONTAINER else AppColors.WARNING_CONTAINER),
                RoundedCornerShape(100.dp),
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
private fun ActionButton(label: String, background: Int, foreground: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color(background), shape)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            color = color(foreground),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun AppText(value: String, size: Int, textColor: Int, bold: Boolean = false) {
    Text(
        text = value,
        color = color(textColor),
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        lineHeight = (size * 1.2f).sp,
    )
}

private fun color(value: Int): Color = Color(value)

private const val TAB_IPASS = 0
private const val TAB_FOODPANDA = 1
private const val TAB_UBER_EATS = 2
