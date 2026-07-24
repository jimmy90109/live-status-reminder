package com.github.jimmy90109.livestatus.ui.home

internal enum class HeroRevealValue {
    Hidden,
    Expanded,
}

internal fun initialHeroRevealValue(
    requiredSettingsComplete: Boolean,
): HeroRevealValue = if (requiredSettingsComplete) {
    HeroRevealValue.Hidden
} else {
    HeroRevealValue.Expanded
}

internal fun heroRevealTarget(
    offsetPx: Float,
    fullHeightPx: Float,
    velocityPxPerSecond: Float,
    velocityThresholdPxPerSecond: Float,
): HeroRevealValue {
    if (fullHeightPx <= 0f) return HeroRevealValue.Hidden

    if (velocityPxPerSecond >= velocityThresholdPxPerSecond) {
        return HeroRevealValue.Expanded
    }
    if (velocityPxPerSecond <= -velocityThresholdPxPerSecond) {
        return HeroRevealValue.Hidden
    }

    val boundedOffset = offsetPx.coerceIn(0f, fullHeightPx)
    return if (boundedOffset >= fullHeightPx * HERO_REVEAL_POSITION_THRESHOLD) {
        HeroRevealValue.Expanded
    } else {
        HeroRevealValue.Hidden
    }
}

internal class HeroRevealGestureGate {
    private var gestureStarted = false
    private var eligibilityResolved = false
    private var revealEligible = false

    fun onPreScroll(availableY: Float) {
        if (gestureStarted) return

        gestureStarted = true
        if (availableY < 0f) {
            eligibilityResolved = true
            revealEligible = false
        }
    }

    fun canReveal(
        consumedY: Float,
        availableY: Float,
        isAtTop: Boolean,
    ): Boolean {
        if (!gestureStarted) {
            onPreScroll(availableY)
        }
        if (!eligibilityResolved) {
            revealEligible =
                availableY > 0f &&
                consumedY == 0f &&
                isAtTop
            eligibilityResolved = true
        }

        return revealEligible && availableY > 0f && isAtTop
    }

    fun reset() {
        gestureStarted = false
        eligibilityResolved = false
        revealEligible = false
    }
}

private const val HERO_REVEAL_POSITION_THRESHOLD = 0.5f
