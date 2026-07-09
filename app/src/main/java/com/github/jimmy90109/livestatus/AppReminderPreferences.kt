package com.github.jimmy90109.livestatus

import android.content.Context

object AppReminderPreferences {
    enum class App(private val preferenceKey: String) {
        IPASS("ipass_enabled"),
        FOODPANDA("foodpanda_enabled"),
        UBER_EATS("uber_eats_enabled"),
        ;

        fun isEnabled(context: Context, installed: Boolean = true): Boolean =
            installed && preferences(context).getBoolean(preferenceKey, true)

        fun setEnabled(context: Context, enabled: Boolean) {
            preferences(context).edit().putBoolean(preferenceKey, enabled).apply()
        }
    }

    private fun preferences(context: Context) =
        context.getSharedPreferences("app_reminder_preferences", Context.MODE_PRIVATE)
}
