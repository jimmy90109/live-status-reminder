package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.github.jimmy90109.livestatus.BuildConfig
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

internal const val LINE_PACKAGE = "jp.naver.line.android"

@Composable
internal fun LineCard(
    installed: Boolean,
    supported: Boolean,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    interactionEnabled: Boolean,
    onOpenDebug: () -> Unit,
) {
    if (!supported) return
    val colors = LocalAppColors.current
    AppCard(
        appName = stringResource(R.string.line_app_name),
        appPackageName = LINE_PACKAGE,
        fallbackIconRes = R.drawable.ic_call_notification,
        title = stringResource(R.string.line_call_card_title),
        description = stringResource(R.string.line_call_card_description),
        supportedLanguages = listOf(stringResource(R.string.line_debug_language)),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        showEnabledSwitch = true,
        cardColor = colors.boltContainer,
        labelColor = colors.boltSecondaryContainer,
        foregroundColor = colors.boltText,
        actionColor = colors.boltPrimary,
        notices = if (BuildConfig.DEBUG) {
            {
                AppCardActionButton(
                    stringResource(R.string.line_debug_open_payload),
                    colors.boltPrimary,
                    colors.boltText,
                    onClick = onOpenDebug,
                )
            }
        } else null,
    )
}
