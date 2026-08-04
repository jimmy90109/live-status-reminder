package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.jimmy90109.livestatus.BuildConfig
import com.github.jimmy90109.livestatus.LiveStatusNotificationParser
import com.github.jimmy90109.livestatus.LiveStatusReminder
import com.github.jimmy90109.livestatus.R
import com.github.jimmy90109.livestatus.TaiwanTaxiRideManager
import com.github.jimmy90109.livestatus.ui.theme.LocalAppColors

@Composable
internal fun TaiwanTaxiCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = stringResource(R.string.taiwan_taxi_app_name),
        appPackageName = TAIWAN_TAXI_PACKAGE,
        fallbackIconRes = R.drawable.ic_car_notification,
        title = stringResource(R.string.taiwan_taxi_card_title),
        description = stringResource(R.string.taiwan_taxi_card_description),
        supportedLanguages = listOf(stringResource(R.string.taiwan_taxi_language)),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.taiwanTaxiContainer,
        labelColor = colors.taiwanTaxiSecondaryContainer,
        foregroundColor = colors.taiwanTaxiText,
    ) {
        AppWarningNotice(
            title = stringResource(R.string.platform_special_status_warning_title),
            description = stringResource(R.string.platform_special_status_warning_description),
        )
        AppActionDivider(colors.taiwanTaxiText)
        AppCardActionButton(
            stringResource(R.string.taiwan_taxi_simulate_driver_found),
            colors.taiwanTaxiPrimary,
            colors.taiwanTaxiText,
            supportingText = stringResource(R.string.monitoring_taiwan_taxi_driver_found),
            enabled = enabled,
        ) {
            LiveStatusReminder.showTaiwanTaxi(
                context,
                LiveStatusNotificationParser.TaiwanTaxiUpdate(
                    event = LiveStatusNotificationParser.TaiwanTaxiEvent.DRIVER_FOUND,
                    plate = "ABC-1234",
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.taiwan_taxi_simulate_vehicle_arrived),
            colors.taiwanTaxiPrimary,
            colors.taiwanTaxiText,
            supportingText = stringResource(R.string.monitoring_taiwan_taxi_vehicle_arrived),
            enabled = enabled,
        ) {
            LiveStatusReminder.showTaiwanTaxi(
                context,
                LiveStatusNotificationParser.TaiwanTaxiUpdate(
                    event = LiveStatusNotificationParser.TaiwanTaxiEvent.VEHICLE_ARRIVED,
                    plate = "ABC-1234",
                ),
            )
        }
        AppCardActionButton(
            stringResource(R.string.taiwan_taxi_simulate_trip_ended),
            colors.taiwanTaxiPrimary,
            colors.taiwanTaxiText,
            supportingText = stringResource(R.string.monitoring_taiwan_taxi_trip_ended),
        ) {
            TaiwanTaxiRideManager.clear(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton(
                stringResource(R.string.taiwan_taxi_debug_open_payload),
                colors.taiwanTaxiPrimary,
                colors.taiwanTaxiText,
                onClick = onOpenDebug,
            )
        }
    }
}

@Composable
internal fun UberRideCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenDebug: () -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "Uber",
        appPackageName = UBER_PACKAGE,
        fallbackIconRes = R.drawable.ic_car_notification,
        title = "乘車進度",
        description = "一般行程顯示上車點、車輛與 PIN；優步小黃顯示職業駕駛接近進度。",
        supportedLanguages = listOf("繁中 / En"),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.commonContainer,
        labelColor = colors.commonSurface,
        foregroundColor = colors.onSurface,
    ) {
        AppWarningNotice(
            title = stringResource(R.string.platform_special_status_warning_title),
            description = stringResource(R.string.platform_special_status_warning_description),
        )
        AppActionDivider(colors.onSurface)
        UberRideSectionHeader(
            title = "標準型優步",
            language = "繁中 / En",
        )
        Spacer(Modifier.height(2.dp))
        UberRideTestButton(
            label = "模擬上車 ETA 與地點",
            supportingText = stringResource(R.string.monitoring_uber_pickup_en_route),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.PICKUP_EN_ROUTE,
                language = LiveStatusNotificationParser.UberRideLanguage.TRADITIONAL_CHINESE,
                title = "2 分鐘內上車",
                pickupEtaMinutes = 2,
                pickupPoint = "在 台北車站東三門 碰面",
            ),
            enabled = enabled,
        )
        UberRideTestButton(
            label = "模擬快抵達",
            supportingText = stringResource(R.string.monitoring_uber_pickup_nearby),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY,
                language = LiveStatusNotificationParser.UberRideLanguage.TRADITIONAL_CHINESE,
                title = "志明即將抵達",
                plate = "ABC-1234",
                vehicle = "白色 Toyota Corolla Cross",
                pin = "2468",
            ),
            enabled = enabled,
        )
        UberRideTestButton(
            label = "模擬已抵達",
            supportingText = stringResource(R.string.monitoring_uber_arrived),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.ARRIVED,
                language = LiveStatusNotificationParser.UberRideLanguage.TRADITIONAL_CHINESE,
                title = "志明 已抵達",
                plate = "ABC-1234",
                vehicle = "白色 Toyota Corolla Cross",
                pin = "2468",
            ),
            enabled = enabled,
        )
        UberRideTestButton(
            label = "模擬前往目的地",
            supportingText = stringResource(R.string.monitoring_uber_on_trip),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.ON_TRIP,
                language = LiveStatusNotificationParser.UberRideLanguage.TRADITIONAL_CHINESE,
                title = "下車地點： 4:30 PM",
                dropoffPoint = "正在前往： 台北 101",
            ),
            enabled = enabled,
        )
        AppCardActionButton(
            "模擬完成，清除狀態",
            colors.commonPrimary,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_uber_ended),
        ) {
            LiveStatusReminder.clearUberRide(context)
        }
        AppActionDivider(colors.onSurface)
        UberRideSectionHeader(
            title = "優步小黃",
            language = "繁中",
        )
        Spacer(Modifier.height(2.dp))
        UberRideTestButton(
            label = "模擬職業駕駛正在途中",
            supportingText = stringResource(R.string.monitoring_uber_taxi_pickup_en_route),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.PICKUP_EN_ROUTE,
                rideType = LiveStatusNotificationParser.UberRideType.UBER_TAXI,
                title = "職業駕駛正在途中",
                officialText = "王先生（4.96 顆星評分）將在 4 分鐘內抵達。",
                pickupEtaMinutes = 4,
            ),
            enabled = enabled,
        )
        UberRideTestButton(
            label = "模擬職業駕駛已在附近",
            supportingText = stringResource(R.string.monitoring_uber_taxi_pickup_approaching),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.PICKUP_APPROACHING,
                rideType = LiveStatusNotificationParser.UberRideType.UBER_TAXI,
                title = "職業駕駛在幾分鐘後就會抵達",
                officialText = "請準備好與職業駕駛碰面",
            ),
            enabled = enabled,
        )
        UberRideTestButton(
            label = "模擬職業駕駛即將抵達",
            supportingText = stringResource(R.string.monitoring_uber_taxi_pickup_nearby),
            update = LiveStatusNotificationParser.UberRideUpdate(
                event = LiveStatusNotificationParser.UberRideEvent.PICKUP_NEARBY,
                rideType = LiveStatusNotificationParser.UberRideType.UBER_TAXI,
                title = "職業駕駛即將抵達",
                officialText = "王先生即將抵達，駕駛車款為 Toyota Corolla Cross Hybrid (ABC1234)。",
                plate = "ABC1234",
                vehicle = "Toyota Corolla Cross Hybrid",
            ),
            enabled = enabled,
        )
        AppCardActionButton(
            "模擬小黃行程完成，清除狀態",
            colors.commonPrimary,
            colors.onSurface,
            supportingText = stringResource(R.string.monitoring_uber_taxi_ended),
        ) {
            LiveStatusReminder.clearUberRide(context)
        }
        if (BuildConfig.DEBUG) {
            AppCardActionButton("查看通知 payload", colors.commonPrimary, colors.onSurface) {
                onOpenDebug()
            }
        }
    }
}

@Composable
private fun UberRideSectionHeader(
    title: String,
    language: String,
) {
    val colors = LocalAppColors.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AppText(title, 15, colors.onSurface, true)
        LanguageTag(language, colors.commonSurface, colors.onSurface)
    }
}

@Composable
private fun UberRideTestButton(
    label: String,
    supportingText: String,
    update: LiveStatusNotificationParser.UberRideUpdate,
    enabled: Boolean,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCardActionButton(
        label,
        colors.commonPrimary,
        colors.onSurface,
        supportingText = supportingText,
        enabled = enabled,
    ) {
        LiveStatusReminder.showUberRide(context, update)
    }
}


@Composable
internal fun PikminBloomCard(
    installed: Boolean,
    enabled: Boolean,
    interactionEnabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
) {
    val colors = LocalAppColors.current
    val context = LocalContext.current
    AppCard(
        appName = "Pikmin Bloom",
        appPackageName = PIKMIN_BLOOM_PACKAGE,
        fallbackIconRes = R.drawable.ic_pikmin_flower_notification,
        title = "種花背景提醒",
        description = "偵測到背景種花通知時，立刻升級成即時提醒。",
        supportedLanguages = listOf("中文 / En"),
        installed = installed,
        enabled = enabled,
        interactionEnabled = interactionEnabled,
        onEnabledChange = onEnabledChange,
        cardColor = colors.pikminContainer,
        labelColor = colors.pikminSecondaryContainer,
        foregroundColor = colors.pikminText,
    ) {
        AppActionDivider(colors.pikminText)
        AppCardActionButton(
            "模擬種花中",
            colors.pikminPrimary,
            colors.pikminText,
            supportingText = stringResource(R.string.monitoring_pikmin_started),
            enabled = enabled,
        ) {
            LiveStatusReminder.showPikminBloom(context)
        }
        AppCardActionButton(
            "清除 Pikmin Bloom 狀態",
            colors.pikminPrimary,
            colors.pikminText,
            supportingText = stringResource(R.string.monitoring_pikmin_stopped),
        ) {
            LiveStatusReminder.clearPikminBloom(context)
        }
    }
}


private const val UBER_PACKAGE = "com.ubercab"
private const val TAIWAN_TAXI_PACKAGE = "dbx.taiwantaxi"
private const val PIKMIN_BLOOM_PACKAGE = "com.nianticlabs.pikmin"
