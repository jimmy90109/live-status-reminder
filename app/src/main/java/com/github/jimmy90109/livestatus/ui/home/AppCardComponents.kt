package com.github.jimmy90109.livestatus.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

internal val LocalAppCardInteractionEnabled = staticCompositionLocalOf { true }

@Composable
internal fun AppCard(
    appName: String,
    appPackageName: String,
    @DrawableRes fallbackIconRes: Int,
    title: String?,
    description: String?,
    supportedLanguages: List<String>,
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    showEnabledSwitch: Boolean = true,
    usePackageIcon: Boolean = true,
    cardColor: Color,
    labelColor: Color,
    foregroundColor: Color,
    actions: @Composable ColumnScope.() -> Unit,
) {
    val colors = LocalAppColors.current
    CardSurface(cardColor, 30, 18) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppLabelPill(
                    label = appName,
                    packageName = appPackageName,
                    installed = installed,
                    usePackageIcon = usePackageIcon,
                    fallbackIconRes = fallbackIconRes,
                    background = labelColor,
                    foreground = foregroundColor,
                )
                supportedLanguages.forEach { language ->
                    LanguageTag(language, labelColor, foregroundColor)
                }
            }
            if (installed && showEnabledSwitch) {
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    enabled = interactionEnabled,
                    modifier = Modifier.padding(start = 12.dp),
                )
            } else if (!installed) {
                StatusPill(false, "已安裝", "尚未安裝")
            } else {
                StatusPill(true, "已安裝", "尚未安裝")
            }
        }
        if (title != null || description != null) {
            Spacer(Modifier.height(12.dp))
            title?.let { AppText(it, 20, colors.onSurface, true) }
            if (title != null && description != null) Spacer(Modifier.height(6.dp))
            description?.let { AppText(it, 15, colors.onSurfaceVariant) }
        }
        Spacer(Modifier.height(6.dp))
        CompositionLocalProvider(LocalAppCardInteractionEnabled provides interactionEnabled) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
        }
    }
}

@Composable
internal fun LanguageTag(
    label: String,
    background: Color,
    foreground: Color,
) {
    androidx.compose.material3.Text(
        text = label,
        color = foreground,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        maxLines = 1,
        modifier = Modifier
            .background(background, RoundedCornerShape(100.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
private fun AppLabelPill(
    label: String,
    packageName: String,
    installed: Boolean,
    usePackageIcon: Boolean,
    @DrawableRes fallbackIconRes: Int,
    background: Color,
    foreground: Color,
) {
    val context = LocalContext.current
    val appIcon = remember(packageName, installed, usePackageIcon) {
        if (!installed || !usePackageIcon) {
            null
        } else {
            runCatching {
                context.packageManager.getApplicationIcon(packageName).toBitmap()
            }.getOrNull()
        }
    }
    Row(
        modifier = Modifier
            .background(background, RoundedCornerShape(100.dp))
            .padding(start = 8.dp, top = 6.dp, end = 12.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (appIcon != null) {
            Image(
                painter = BitmapPainter(appIcon.asImageBitmap()),
                contentDescription = "$label icon",
                modifier = Modifier
                    .size(20.dp)
                    .clip(RoundedCornerShape(6.dp)),
            )
        } else {
            Icon(
                painter = painterResource(fallbackIconRes),
                contentDescription = "$label icon",
                tint = foreground,
                modifier = Modifier.size(18.dp),
            )
        }
        Spacer(Modifier.width(7.dp))
        androidx.compose.material3.Text(
            text = label,
            color = foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
        )
    }
}

private fun Drawable.toBitmap(): Bitmap {
    val bitmapWidth = intrinsicWidth.takeIf { it > 0 } ?: DEFAULT_ICON_BITMAP_SIZE
    val bitmapHeight = intrinsicHeight.takeIf { it > 0 } ?: DEFAULT_ICON_BITMAP_SIZE
    val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

private const val DEFAULT_ICON_BITMAP_SIZE = 48

@Composable
internal fun AppActionDivider(color: Color) {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 6.dp),
        color = color.copy(alpha = 0.18f),
    )
}

@Composable
internal fun AppWarningNotice(
    title: String,
    description: String,
    containerColor: Color? = null,
    contentColor: Color? = null,
) {
    val colors = LocalAppColors.current
    val resolvedContainerColor = containerColor ?: colors.warningContainer
    val resolvedContentColor = contentColor ?: colors.warningText
    CardSurface(resolvedContainerColor, 14, 14) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = resolvedContentColor,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(9.dp))
            Column {
                AppText(title, 14, resolvedContentColor, true)
                Spacer(Modifier.height(4.dp))
                AppText(description, 13, resolvedContentColor)
            }
        }
    }
}
