package com.github.jimmy90109.livestatus

import android.content.Context

object AppReminderPreferences {
    private const val NOW_BAR_TROUBLESHOOTING_DISMISSED = "now_bar_troubleshooting_dismissed"

    enum class App(private val preferenceKey: String) {
        CLOCK("clock_enabled"),
        IPASS("ipass_enabled"),
        FOODPANDA("foodpanda_enabled"),
        UBER_RIDE("uber_ride_enabled"),
        UBER_EATS("uber_eats_enabled"),
        PIKMIN_BLOOM("pikmin_bloom_enabled"),
        ;

        fun isEnabled(context: Context, installed: Boolean = true): Boolean =
            installed && preferences(context).getBoolean(preferenceKey, true)

        fun setEnabled(context: Context, enabled: Boolean) {
            preferences(context).edit().putBoolean(preferenceKey, enabled).apply()
        }
    }

    fun isNowBarTroubleshootingDismissed(context: Context): Boolean =
        preferences(context).getBoolean(NOW_BAR_TROUBLESHOOTING_DISMISSED, false)

    fun setNowBarTroubleshootingDismissed(context: Context, dismissed: Boolean) {
        preferences(context)
            .edit()
            .putBoolean(NOW_BAR_TROUBLESHOOTING_DISMISSED, dismissed)
            .apply()
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences("app_reminder_preferences", Context.MODE_PRIVATE)
}
