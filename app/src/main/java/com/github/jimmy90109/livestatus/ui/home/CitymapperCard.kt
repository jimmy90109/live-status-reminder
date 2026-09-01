package com.github.jimmy90109.livestatus.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.livestatus.BuildConfig
import com.github.jimmy90109.livestatus.CitymapperNavigationPresentation
import com.github.jimmy90109.livestatus.CitymapperNavigationStage
import com.github.jimmy90109.livestatus.CitymapperNavigationText
import com.github.jimmy90109.livestatus.CitymapperNavigationUpdate
import com.github.jimmy90109.livestatus.CitymapperTransitMode
import com.github.jimmy90109.livestatus.CitymapperTransitTiming
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

@Composable
internal fun CitymapperCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = stringResource(R.string.citymapper_app_name),
        appPackageName = CITYMAPPER_PACKAGE,
        fallbackIconRes = R.drawable.ic_navigation_notification,
        title = stringResource(R.string.citymapper_card_title),
        description = stringResource(R.string.citymapper_card_description),
        supportedLanguages = listOf(stringResource(R.string.citymapper_source_language)),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.citymapperContainer,
        labelColor = colors.citymapperSecondaryContainer,
        foregroundColor = colors.citymapperText,
        actionColor = colors.citymapperPrimary,
        additionalTags = {
            LanguageTag(
                stringResource(R.string.citymapper_beta_tag),
                colors.warningContainer,
                colors.warningText,
            )
        },
    ) {
        AppActionDivider(colors.citymapperText)
        CitymapperSimulationHeader(R.string.citymapper_simulation_group_walking)
        CitymapperSimulationButton(
            R.string.citymapper_simulate_walking,
            R.string.citymapper_simulate_walking_supporting,
            CitymapperSimulation.WALKING,
            enabled,
        )
        CitymapperSimulationHeader(R.string.citymapper_simulation_group_bus)
        CitymapperSimulationButton(
            R.string.citymapper_simulate_bus_waiting,
            R.string.citymapper_simulate_bus_waiting_supporting,
            CitymapperSimulation.BUS_WAITING,
            enabled,
        )
        CitymapperSimulationHeader(R.string.citymapper_simulation_group_train)
        CitymapperSimulationButton(
            R.string.citymapper_simulate_train_departure,
            R.string.citymapper_simulate_train_departure_supporting,
            CitymapperSimulation.TRAIN_DEPARTURE,
            enabled,
        )
        CitymapperSimulationHeader(R.string.citymapper_simulation_group_metro)
        CitymapperSimulationButton(
            R.string.citymapper_simulate_metro_waiting,
            R.string.citymapper_simulate_metro_waiting_supporting,
            CitymapperSimulation.METRO_WAITING,
            enabled,
        )
        CitymapperSimulationButton(
            R.string.citymapper_simulate_metro_riding,
            R.string.citymapper_simulate_metro_riding_supporting,
            CitymapperSimulation.METRO_RIDING,
            enabled,
        )
        AppCardActionButton(
            stringResource(R.string.citymapper_simulate_clear),
            colors.citymapperPrimary,
            colors.citymapperText,
            supportingText = stringResource(R.string.citymapper_simulate_clear_supporting),
        ) {
            LiveStatusReminder.clearCitymapperNavigation(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.citymapper_debug_open_payload),
                colors.citymapperPrimary,
                colors.citymapperText,
                onClick = onOpenDebug,
            )
        }
    }
}

@Composable
private fun CitymapperSimulationHeader(@StringRes titleRes: Int) {
    val colors = LocalAppColors.current
    Spacer(Modifier.height(2.dp))
    AppText(stringResource(titleRes), 15, colors.onSurface, true)
}

@Composable
private fun CitymapperSimulationButton(
    @StringRes labelRes: Int,
    @StringRes supportingTextRes: Int,
    simulation: CitymapperSimulation,
    enabled: Boolean,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCardActionButton(
        stringResource(labelRes),
        colors.citymapperPrimary,
        colors.citymapperText,
        supportingText = stringResource(supportingTextRes),
        enabled = enabled,
    ) {
        LiveStatusReminder.showCitymapperNavigation(context, simulation.createUpdate())
    }
}

internal enum class CitymapperSimulation {
    WALKING,
    BUS_WAITING,
    TRAIN_DEPARTURE,
    METRO_WAITING,
    METRO_RIDING,
    ;

    fun createUpdate(): CitymapperNavigationUpdate = when (this) {
        WALKING -> update(
            title = "步行至公車站",
            contentText = "(距離8分鐘)\n示例路口\n預計抵達時間：下午3:30到達",
            presentation = CitymapperNavigationPresentation(
                stage = CitymapperNavigationStage.WALKING,
                walkingMinutes = 8,
            ),
        )
        BUS_WAITING -> update(
            title = "等候 107 或 201（示例站）",
            contentText = "11, 12, 18分鐘\n預計抵達時間：下午3:30到達",
            presentation = CitymapperNavigationPresentation(
                stage = CitymapperNavigationStage.WAITING,
                transitMode = CitymapperTransitMode.BUS,
                transitTiming = CitymapperTransitTiming.CountdownMinutes(11),
            ),
        )
        TRAIN_DEPARTURE -> update(
            title = "1201 次往新竹-Hsinchu",
            contentText = "下午2:03發車\n預計抵達時間：下午3:30到達",
            presentation = CitymapperNavigationPresentation(
                stage = CitymapperNavigationStage.TRAIN_DEPARTURE,
                transitMode = CitymapperTransitMode.TRAIN,
                transitTiming = CitymapperTransitTiming.ScheduledTime("下午2:03"),
            ),
        )
        METRO_WAITING -> update(
            title = "等候 BL（往頂埔）",
            contentText = "3, 8, 16分鐘\n預計抵達時間：下午3:30到達",
            presentation = CitymapperNavigationPresentation(
                stage = CitymapperNavigationStage.WAITING,
                transitMode = CitymapperTransitMode.METRO,
                transitTiming = CitymapperTransitTiming.CountdownMinutes(3),
            ),
        )
        METRO_RIDING -> update(
            title = "乘坐 4 站",
            contentText = "市政府-Taipei City Hall\n預計抵達時間：下午3:30到達",
            presentation = CitymapperNavigationPresentation(
                stage = CitymapperNavigationStage.RIDING,
                transitMode = CitymapperTransitMode.METRO,
                stops = 4,
            ),
        )
    }

    private fun update(
        title: String,
        contentText: String,
        presentation: CitymapperNavigationPresentation,
    ) = CitymapperNavigationUpdate(
        sourceKey = "citymapper-simulation-${name.lowercase()}",
        postTime = 0L,
        text = CitymapperNavigationText(title, contentText),
        presentation = presentation,
    )
}

private const val CITYMAPPER_PACKAGE = "com.citymapper.app.release"
