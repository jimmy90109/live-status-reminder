package com.github.jimmy90109.livestatus

import android.content.Context

object YouBikeRegionPreferenceStore {
    private const val PREFERENCES = "you_bike_region_preference"
    private const val KEY_REGION = "last_manual_region"

    fun load(context: Context): YouBikeRegion? = YouBikeRegionReusePolicy.parseRememberedRegion(
        context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).getString(KEY_REGION, null),
    )

    fun save(context: Context, region: YouBikeRegion) {
        val editor = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE).edit()
        if (region.isFareSupported) editor.putString(KEY_REGION, region.name)
        else editor.remove(KEY_REGION)
        editor.apply()
    }
}
