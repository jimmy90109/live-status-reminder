package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jimmy90109.livestatus.BuildConfig
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.TaipeiMetroGoUpdate
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

@Composable
internal fun TaipeiMetroGoCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = stringResource(R.string.taipei_metro_go_app_name),
        appPackageName = TAIPEI_METRO_GO_PACKAGE,
        fallbackIconRes = R.drawable.ic_metro_notification,
        title = stringResource(R.string.taipei_metro_go_card_title),
        description = stringResource(R.string.taipei_metro_go_card_description),
        supportedLanguages = listOf(stringResource(R.string.taipei_metro_go_language)),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.taipeiMetroGoContainer,
        labelColor = colors.taipeiMetroGoSecondaryContainer,
        foregroundColor = colors.taipeiMetroGoText,
        actionColor = colors.taipeiMetroGoPrimary,
    ) {
        AppActionDivider(colors.taipeiMetroGoText)
        AppCardActionButton(
            stringResource(R.string.taipei_metro_go_simulate_running),
            colors.taipeiMetroGoPrimary,
            colors.taipeiMetroGoText,
            supportingText = stringResource(R.string.taipei_metro_go_simulate_running_supporting),
            enabled = enabled,
        ) {
            LiveStatusReminder.showTaipeiMetroGo(
                context,
                TaipeiMetroGoUpdate(
                    sourceKey = "simulation:taipei-metro-go",
                    postTime = System.currentTimeMillis(),
                    origin = "市政府",
                    destination = "大直",
                    currentStation = "中山國中",
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.taipei_metro_go_simulate_clear),
            colors.taipeiMetroGoPrimary,
            colors.taipeiMetroGoText,
            supportingText = stringResource(R.string.taipei_metro_go_simulate_clear_supporting),
        ) {
            LiveStatusReminder.clearTaipeiMetroGo(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.taipei_metro_go_debug_open_payload),
                colors.taipeiMetroGoPrimary,
                colors.taipeiMetroGoText,
                onClick = onOpenDebug,
            )
        }
    }
}

private const val TAIPEI_METRO_GO_PACKAGE = "tw.com.trtc.is.android05"
