package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.TexpressArrival
import com.github.jimmy90109.livestatus.TexpressArrivalNotificationParser
import com.github.jimmy90109.livestatus.TexpressArrivalUpdate
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

@Composable
internal fun TexpressCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = stringResource(R.string.texpress_app_name),
        appPackageName = TexpressArrivalNotificationParser.PACKAGE_NAME,
        fallbackIconRes = R.drawable.ic_train_notification,
        title = stringResource(R.string.texpress_card_title),
        description = stringResource(R.string.texpress_card_description),
        supportedLanguages = listOf(stringResource(R.string.texpress_language)),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.texpressContainer,
        labelColor = colors.texpressSecondaryContainer,
        foregroundColor = colors.texpressText,
        actionColor = colors.texpressPrimary,
        additionalTags = {
            LanguageTag(
                stringResource(R.string.app_beta_tag),
                colors.warningContainer,
                colors.warningText,
            )
        },
    ) {
        AppActionDivider(colors.texpressText)
        AppCardActionButton(
            stringResource(R.string.texpress_simulate_arrival),
            colors.texpressPrimary,
            colors.texpressText,
            supportingText = stringResource(R.string.texpress_simulate_arrival_supporting),
            enabled = enabled,
        ) {
            LiveStatusReminder.showTexpressArrival(
                context,
                TexpressArrivalUpdate(
                    sourceKey = "simulation:texpress",
                    postTime = System.currentTimeMillis(),
                    arrival = TexpressArrival(
                        trainNumber = "242",
                        minutes = 5,
                        station = "板橋站",
                    ),
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.texpress_simulate_clear),
            colors.texpressPrimary,
            colors.texpressText,
            supportingText = stringResource(R.string.texpress_simulate_clear_supporting),
        ) {
            LiveStatusReminder.clearTexpressArrival(context)
        }
    }
}
