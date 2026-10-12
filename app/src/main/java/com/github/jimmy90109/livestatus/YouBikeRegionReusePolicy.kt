package com.github.jimmy90109.livestatus

internal data class YouBikeRegionSelection(val region: YouBikeRegion, val isReused: Boolean = false)

internal object YouBikeRegionReusePolicy {
    fun parseRememberedRegion(value: String?): YouBikeRegion? =
        value?.let { runCatching { YouBikeRegion.valueOf(it) }.getOrNull() }
            ?.takeIf { it.isFareSupported }

    fun select(resolution: YouBikeStationResolution, rememberedRegion: YouBikeRegion?): YouBikeRegionSelection =
        when (resolution) {
            is YouBikeStationResolution.Supported -> YouBikeRegionSelection(resolution.region)
            is YouBikeStationResolution.Ambiguous -> {
                val region = rememberedRegion?.takeIf { it.isFareSupported && it in resolution.candidates }
                YouBikeRegionSelection(region ?: YouBikeRegion.UNRESOLVED, isReused = region != null)
            }
            YouBikeStationResolution.Unknown -> YouBikeRegionSelection(YouBikeRegion.UNRESOLVED)
            YouBikeStationResolution.Unsupported -> YouBikeRegionSelection(YouBikeRegion.UNSUPPORTED)
        }

    fun withManualRegion(session: YouBikeRideSession, region: YouBikeRegion): YouBikeRideSession? {
        val isAllowed = region == YouBikeRegion.UNSUPPORTED ||
            (region.isFareSupported && (session.candidateRegions.isEmpty() || region in session.candidateRegions))
        if (!isAllowed) return null
        return session.copy(region = region, manuallySelectedRegion = region, isRegionReused = false)
    }
}
