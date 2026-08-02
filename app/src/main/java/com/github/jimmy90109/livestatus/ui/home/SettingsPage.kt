package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
    onOpenPrivacyPolicy: () -> Unit,
) {
    val colors = LocalAppColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(top = topPadding, bottom = bottomPadding),
        verticalArrangement = Arrangement.Top,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    tint = colors.onSurfaceVariant,
                )
            }
            Spacer(Modifier.padding(horizontal = 4.dp))
            AppText("設定", 28, colors.onSurface, true)
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
            )
            Spacer(Modifier.height(10.dp))
        }
        Spacer(Modifier.height(14.dp))
        ActionButton(
            "隱私權政策  →",
            colors.commonSurface,
            colors.onSurface,
            onClick = onOpenPrivacyPolicy,
        )
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
            subtitle = "完成設定後，App 就能把媒體播放、倒數計時、乘車、外送與種花狀態變成即時通知。",
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
                    description = "允許 App 辨識媒體播放、Clock、iPASS MONEY、台灣 Pay、YouBike、foodpanda、Uber、Uber Eats 與 Pikmin Bloom 狀態。",
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
    onDismiss: (() -> Unit)? = null,
) {
    val colors = LocalAppColors.current
    CardSurface(colors.warningContainer, 26, 18) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                AppText(
                    when (brandWarning) {
                        BrandWarning.SAMSUNG_NOW_BAR ->
                            "⚠ Samsung Now Bar 可能限制第三方 Live Updates"
                        BrandWarning.XIAOMI_HYPER_ISLAND ->
                            "小米 HyperOS 膠囊支援"
                    },
                    18,
                    colors.warningText,
                    true,
                )
            }
            if (onDismiss != null) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "關閉",
                        tint = colors.warningText,
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        AppText(
            when (brandWarning) {
                BrandWarning.SAMSUNG_NOW_BAR ->
                    "如果通知已出現，但沒有上島，可以查看 Samsung One UI 的疑難排解流程。"
                BrandWarning.XIAOMI_HYPER_ISLAND ->
                    "如果膠囊沒有出現，請在 HyperOS 系統設定中允許浮動或焦點通知；" +
                        "若系統不接受第三方膠囊，App 仍會顯示一般即時通知。"
            },
            15,
            colors.warningText,
        )
        if (brandWarning == BrandWarning.SAMSUNG_NOW_BAR) {
            Spacer(Modifier.height(14.dp))
            ActionButton(
                "查看解決方法  →",
                colors.warningText,
                colors.warningContainer,
                onClick = onOpenSamsungNowBarGuide,
            )
        }
    }
}
