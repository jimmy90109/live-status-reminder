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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

@Composable
internal fun HeroCard() {
    val colors = LocalAppColors.current
    CardSurface(colors.commonContainer, 36) {
        LabelPill("LIVE STATUS", colors.commonPrimary, colors.commonOnPrimary)
        Spacer(Modifier.height(34.dp))
        AppText("重要狀態，\n留在最前面。", 34, colors.onSurface, true)
        Spacer(Modifier.height(12.dp))
        AppText(
            "進站後顯示乘車碼捷徑；外送期間顯示 foodpanda 或 Uber Eats 訂單進度。",
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
            AppText(title, 24, colors.onSurface, true)
            Spacer(Modifier.height(4.dp))
            AppText(subtitle, 15, colors.onSurfaceVariant)
        }
        if (collapsible) {
            Text(
                text = if (expanded) "⌃" else "⌄",
                color = colors.onSurfaceVariant,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 12.dp),
            )
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
        ActionButton("$action  →", colors.commonPrimary, colors.commonOnPrimary, onClick)
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
internal fun ActionButton(label: String, background: Color, foreground: Color, onClick: () -> Unit) {
    val shape = RoundedCornerShape(100.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(background, shape)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = label,
            color = foreground,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
        )
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
