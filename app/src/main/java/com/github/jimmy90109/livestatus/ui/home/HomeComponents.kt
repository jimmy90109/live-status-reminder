package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

@Composable
internal fun HeroCard(onOpenSettings: () -> Unit) {
    val colors = LocalAppColors.current
    CardSurface(colors.commonContainer, 36) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelPill("LIVE STATUS", colors.commonPrimary, colors.commonOnPrimary)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onOpenSettings) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = "設定",
                    tint = colors.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(34.dp))
        AppText("重要狀態，\n留在最前面。", 34, colors.onSurface, true)
        Spacer(Modifier.height(12.dp))
        AppText(
            "進站後顯示乘車碼捷徑；外送期間顯示訂單進度；Pikmin Bloom 種花時提醒你記得結束。",
            16,
            colors.onSurfaceVariant,
        )
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    subtitle: String,
    expanded: Boolean = true,
    collapsible: Boolean = false,
    onToggle: () -> Unit = {},
) {
    val colors = LocalAppColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            AppText(title, 24, colors.onSurface, true)
            Spacer(Modifier.height(4.dp))
            AppText(subtitle, 15, colors.onSurfaceVariant)
        }
        if (collapsible) {
            IconButton(
                onClick = onToggle,
                modifier = Modifier.padding(start = 12.dp),
            ) {
                Icon(
                    imageVector = if (expanded) {
                        Icons.Rounded.KeyboardArrowUp
                    } else {
                        Icons.Rounded.KeyboardArrowDown
                    },
                    contentDescription = if (expanded) "收合" else "展開",
                    tint = colors.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun SettingCard(
    number: String,
    title: String,
    description: String,
    enabled: Boolean,
    enabledText: String,
    disabledText: String,
    action: String,
    onClick: () -> Unit,
) {
    val colors = LocalAppColors.current
    CardSurface(colors.commonSurface, 26, 18) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            LabelPill(number, colors.commonContainer, colors.onSurface)
            Spacer(Modifier.padding(horizontal = 5.dp))
            AppText(title, 20, colors.onSurface, true)
        }
        Spacer(Modifier.height(12.dp))
        AppText(description, 15, colors.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        StatusPill(enabled, enabledText, disabledText)
        Spacer(Modifier.height(14.dp))
        ActionButton("$action  →", colors.commonPrimary, colors.commonOnPrimary, onClick = onClick)
    }
}

@Composable
internal fun CardSurface(
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
internal fun LabelPill(label: String, background: Color, foreground: Color) {
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
internal fun StatusPill(enabled: Boolean, enabledText: String, disabledText: String) {
    val colors = LocalAppColors.current
    Text(
        text = "${if (enabled) "✓" else "•"}  ${if (enabled) enabledText else disabledText}",
        color = if (enabled) colors.successText else colors.warningText,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(
                if (enabled) colors.successContainer else colors.warningContainer,
                RoundedCornerShape(100.dp),
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

@Composable
internal fun ActionButton(
    label: String,
    background: Color,
    foreground: Color,
    supportingText: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.46f)
            .background(background, shape)
            .clip(shape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Column {
            Text(
                text = label,
                color = foreground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            supportingText?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    color = foreground,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}

@Composable
internal fun AppText(value: String, size: Int, textColor: Color, bold: Boolean = false) {
    Text(
        text = value,
        color = textColor,
        fontSize = size.sp,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        lineHeight = (size * 1.2f).sp,
    )
}
