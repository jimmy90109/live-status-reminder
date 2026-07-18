package com.github.jimmy90109.livestatus.ui.home

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.SystemClock
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.jimmy90109.livestatus.AppReminderPreferences
import com.github.jimmy90109.livestatus.BuildConfig
import com.github.jimmy90109.livestatus.ClockTimerNotificationExtractor
import com.github.jimmy90109.livestatus.ClockTimerSource
import com.github.jimmy90109.livestatus.ClockTimerState
import com.github.jimmy90109.livestatus.ClockTimerUpdate
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors
import kotlinx.coroutines.launch


@Composable
internal fun AppCard(
    appName: String,
    appPackageName: String,
    @DrawableRes fallbackIconRes: Int,
    title: String,
    description: String,
    supportedLanguages: List<String>,
    installed: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
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
                    fallbackIconRes = fallbackIconRes,
                    background = labelColor,
                    foreground = foregroundColor,
                )
                supportedLanguages.forEach { language ->
                    LanguageTag(language, labelColor, foregroundColor)
                }
            }
            if (installed) {
                Switch(
                    checked = enabled,
                    onCheckedChange = onEnabledChange,
                    modifier = Modifier.padding(start = 12.dp),
                )
            } else {
                StatusPill(false, "已安裝", "尚未安裝")
            }
        }
        Spacer(Modifier.height(12.dp))
        AppText(title, 20, colors.onSurface, true)
        Spacer(Modifier.height(6.dp))
        AppText(description, 15, colors.onSurfaceVariant)
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
    }
}

@Composable
private fun LanguageTag(
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
    @DrawableRes fallbackIconRes: Int,
    background: Color,
    foreground: Color,
) {
    val context = LocalContext.current
    val appIcon = remember(packageName, installed) {
        if (!installed) {
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
