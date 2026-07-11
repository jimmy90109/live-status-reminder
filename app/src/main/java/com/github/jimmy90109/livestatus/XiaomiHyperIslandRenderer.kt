package com.github.jimmy90109.livestatus

import android.app.Notification
import android.content.Context
import android.os.Build
import android.os.Bundle
import io.github.d4viddf.hyperisland_kit.HyperIslandNotification
import io.github.d4viddf.hyperisland_kit.HyperPicture
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoLeft
import io.github.d4viddf.hyperisland_kit.models.ImageTextInfoRight
import io.github.d4viddf.hyperisland_kit.models.PicInfo
import io.github.d4viddf.hyperisland_kit.models.TextInfo

object XiaomiHyperIslandRenderer {
    const val FOCUS_PARAM_EXTRA = "miui.focus.param"

    fun apply(
        context: Context,
        builder: Notification.Builder,
        payload: LiveStatusPayload,
    ) {
        if (!shouldRender(Build.MANUFACTURER, Build.BRAND)) return

        val iconKey = "live_status_${payload.id}_icon"
        val islandBuilder = HyperIslandNotification.Builder(
            context = context,
            businessName = "live_status_${payload.id}",
            ticker = payload.criticalText,
        )
            .setLogEnabled(false)
            .setEnableFloat(true)
            .setIslandFirstFloat(true)
            .setShowNotification(true)
            .setIslandConfig(timeout = 60_000, dismissible = true, expandedTimeMs = 5_000)
            .addPicture(HyperPicture(iconKey, context, payload.leftIconRes))
            .setSmallIsland(iconKey)
            .setBigIslandInfo(
                left = ImageTextInfoLeft(
                    type = 1,
                    picInfo = PicInfo(type = 1, pic = iconKey),
                    textInfo = TextInfo(title = payload.appName),
                ),
                right = ImageTextInfoRight(
                    type = 2,
                    textInfo = TextInfo(title = payload.criticalText),
                ),
            )
            .setChatInfo(
                title = payload.title,
                content = payload.contentText,
                pictureKey = iconKey,
                appPkg = context.packageName,
            )

        payload.progress?.let { progress ->
            islandBuilder.setProgressBar(progress.coerceIn(0, 100), "#06C167")
        }

        builder.addExtras(islandBuilder.buildResourceBundle())
        builder.addExtras(
            Bundle().apply {
                putString(FOCUS_PARAM_EXTRA, islandBuilder.buildJsonParam())
            },
        )
    }

    fun shouldRender(manufacturer: String?, brand: String?): Boolean =
        isXiaomiFamily(manufacturer) || isXiaomiFamily(brand)

    private fun isXiaomiFamily(value: String?): Boolean {
        val normalized = value?.lowercase()?.trim().orEmpty()
        return normalized == "xiaomi" || normalized == "redmi" || normalized == "poco"
    }
}
