package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

@Composable
internal fun SettingsPage(
    status: StatusSnapshot,
    topPadding: Dp,
    bottomPadding: Dp,
    onBack: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSamsungNowBarGuide: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenPrivacyPolicy: () -> Unit,
) {
    val colors = LocalAppColors.current
    Box(
        modifier = Modifier.fillMaxSize().background(colors.background).testTag("settings_page"),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = HOME_SINGLE_PANE_MAX_WIDTH)
                .fillMaxSize()
                .background(colors.background)
                .verticalScroll(LocalHomeUiState.current?.settingsScroll ?: rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = topPadding, bottom = bottomPadding),
            verticalArrangement = Arrangement.Top,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = rememberHapticAction(action = onBack)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.action_back),
                        tint = colors.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.padding(horizontal = 4.dp))
                AppText(stringResource(R.string.settings_title), 28, colors.onSurface, true)
            }
            Spacer(Modifier.height(20.dp))
            RequiredSettingsSection(
                status = status,
                expanded = true,
                collapsible = false,
                onToggle = {},
                onOpenNotificationAccess = onOpenNotificationAccess,
                onRequestNotificationPermission = onRequestNotificationPermission,
            )
            status.brandWarning?.let { brandWarning ->
                Spacer(Modifier.height(10.dp))
                BrandWarningCard(
                    brandWarning = brandWarning,
                    onOpenSamsungNowBarGuide = onOpenSamsungNowBarGuide,
                    onOpenAppSettings = onOpenAppSettings,
                )
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(14.dp))
            ActionButton(
                stringResource(R.string.privacy_policy_link),
                colors.commonSurface,
                colors.onSurface,
                onClick = onOpenPrivacyPolicy,
            )
        }
    }
}

@Composable
internal fun RequiredSettingsSection(
    status: StatusSnapshot,
    expanded: Boolean,
    collapsible: Boolean = status.requiredSettingsComplete,
    onToggle: () -> Unit,
    onOpenNotificationAccess: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
) {
    Column {
        SectionHeader(
            title = "必要設定",
            subtitle = stringResource(R.string.required_settings_description),
            expanded = expanded,
            collapsible = collapsible,
            onToggle = onToggle,
        )
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(
            visible = expanded,
            modifier = Modifier.clip(RoundedCornerShape(26.dp)),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingCard(
                    number = "01",
                    title = "讀取狀態通知",
                    description = "允許 App 辨識媒體播放、Clock、Google Recorder、YPT、Hevy、iPASS MONEY、台灣 Pay、YouBike、foodpanda、McDonald's、55688、Uber、Uber Eats、Citymapper 導航與 Pikmin Bloom 狀態。",
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
            }
        }
    }
}

@Composable
internal fun BrandWarningCard(
    brandWarning: BrandWarning,
    onOpenSamsungNowBarGuide: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onDismiss: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    CardSurface(background = colors.warningContainer, radius = 26, padding = 18) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                AppText(
                    when (brandWarning) {
                        BrandWarning.SAMSUNG_NOW_BAR ->
                            "⚠ Samsung Now Bar 可能限制第三方 Live Updates"
                        BrandWarning.XIAOMI_HYPER_ISLAND ->
                            stringResource(R.string.xiaomi_hyperos_warning_title)
                        BrandWarning.ASUS_LIVE_UPDATES ->
                            stringResource(R.string.asus_live_updates_warning_title)
                    },
                    18,
                    colors.warningText,
                    true,
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = rememberHapticAction(action = onDismiss)) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.brand_warning_dismiss),
                        tint = colors.warningText,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        when (brandWarning) {
            BrandWarning.SAMSUNG_NOW_BAR -> AppText(
                "如果通知已出現，但沒有上島，可以查看 Samsung One UI 的疑難排解流程。",
                15,
                colors.warningText,
            )
            BrandWarning.XIAOMI_HYPER_ISLAND -> XiaomiWarningDescription()
            BrandWarning.ASUS_LIVE_UPDATES -> AppText(
                stringResource(R.string.asus_live_updates_warning_description),
                15,
                colors.warningText,
            )
        }
        when (brandWarning) {
            BrandWarning.SAMSUNG_NOW_BAR -> {
                Spacer(Modifier.height(14.dp))
                ActionButton(
                    "查看解決方法 →",
                    colors.warningText,
                    colors.warningContainer,
                    onClick = onOpenSamsungNowBarGuide,
                )
            }
            BrandWarning.XIAOMI_HYPER_ISLAND -> {
                Spacer(Modifier.height(14.dp))
                ActionButton(
                    stringResource(R.string.xiaomi_open_app_settings),
                    colors.warningText,
                    colors.warningContainer,
                    onClick = onOpenAppSettings,
                )
            }
            BrandWarning.ASUS_LIVE_UPDATES -> Unit
        }
    }
}

@Composable
private fun XiaomiWarningDescription() {
    val textColor = LocalAppColors.current.warningText
    AppText(
        stringResource(R.string.xiaomi_hyperos_warning_intro),
        15,
        textColor,
    )
    Spacer(Modifier.height(8.dp))
    XiaomiChecklistItem("1.", stringResource(R.string.xiaomi_hyperos_warning_autostart))
    Spacer(Modifier.height(6.dp))
    XiaomiChecklistItem("2.", stringResource(R.string.xiaomi_hyperos_warning_background_lock))
    Spacer(Modifier.height(6.dp))
    XiaomiChecklistItem("3.", stringResource(R.string.xiaomi_hyperos_warning_floating_notification))
    Spacer(Modifier.height(10.dp))
    AppText(
        stringResource(R.string.xiaomi_hyperos_warning_footer),
        15,
        textColor,
    )
}

@Composable
private fun XiaomiChecklistItem(number: String, text: String) {
    val textColor = LocalAppColors.current.warningText
    Row(verticalAlignment = Alignment.Top) {
        AppText(number, 15, textColor)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            AppText(text, 15, textColor)
        }
    }
}
