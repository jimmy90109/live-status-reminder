package com.github.jimmy90109.livestatus

import android.content.Context
import android.os.Build

internal object MediaPlaybackDefaultPolicy {
    fun isDefaultEnabled(manufacturer: String?, brand: String?): Boolean =
        manufacturer.isGoogle() || brand.isGoogle()

    private fun String?.isGoogle(): Boolean =
        this?.trim()?.equals("google", ignoreCase = true) == true
}

object AppReminderPreferences {
    private const val BRAND_WARNING_DISMISSED = "brand_warning_dismissed"

    enum class App(private val preferenceKey: String) {
        MEDIA_PLAYBACK("media_playback_enabled"),
        CLOCK("clock_enabled"),
        IPASS("ipass_enabled"),
        TAIWAN_PAY("taiwan_pay_enabled"),
        YOUBIKE("you_bike_enabled"),
        FOODPANDA("foodpanda_enabled"),
        TAIWAN_TAXI("taiwan_taxi_enabled"),
        UBER_RIDE("uber_ride_enabled"),
        UBER_EATS("uber_eats_enabled"),
        PIKMIN_BLOOM("pikmin_bloom_enabled"),
        YPT("ypt_enabled"),
        HEVY("hevy_enabled"),
        STRAVA("strava_enabled"),
        DISCORD_VOICE("discord_voice_enabled"),
        TEAMS_CALL("teams_call_enabled"),
        GOOGLE_RECORDER("google_recorder_enabled"),
        ;

        fun isEnabled(context: Context, installed: Boolean = true): Boolean {
            val defaultEnabled = when (this) {
                MEDIA_PLAYBACK -> MediaPlaybackDefaultPolicy.isDefaultEnabled(
                    Build.MANUFACTURER,
                    Build.BRAND,
                )
                else -> true
            }
            return installed && preferences(context).getBoolean(preferenceKey, defaultEnabled)
        }

        fun setEnabled(context: Context, enabled: Boolean) {
            preferences(context).edit().putBoolean(preferenceKey, enabled).apply()
        }
    }

    fun isBrandWarningDismissed(context: Context): Boolean =
        preferences(context).getBoolean(BRAND_WARNING_DISMISSED, false)

    fun setBrandWarningDismissed(context: Context, dismissed: Boolean) {
        preferences(context)
            .edit()
            .putBoolean(BRAND_WARNING_DISMISSED, dismissed)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences("app_reminder_preferences", Context.MODE_PRIVATE)
}
