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

internal data class HeroRevealVisualState(
    val scale: Float,
    val alpha: Float,
)

internal fun heroRevealVisualState(
    visibleHeightPx: Float,
    fullHeightPx: Float,
): HeroRevealVisualState {
    if (fullHeightPx <= 0f) {
        return HeroRevealVisualState(scale = 1f, alpha = 1f)
    }

    return if (visibleHeightPx >= fullHeightPx * HERO_REVEAL_POSITION_THRESHOLD) {
        HeroRevealVisualState(scale = 1f, alpha = 1f)
    } else {
        HeroRevealVisualState(
            scale = HERO_REVEAL_MIN_SCALE,
            alpha = HERO_REVEAL_MIN_ALPHA,
        )
    }
}

internal class HeroRevealGestureGate {
    private var gestureStarted = false
    private var eligibilityResolved = false
    private var revealEligible = false
    private var heroDragActive = false

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
        if (heroDragActive) {
            return availableY > 0f && isAtTop
        }
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

    fun onHeroDragConsumed(consumedY: Float) {
        if (consumedY != 0f) {
            heroDragActive = true
        }
    }

    fun reset() {
        gestureStarted = false
        eligibilityResolved = false
        revealEligible = false
        heroDragActive = false
    }
}

internal class HeroRevealHapticTracker {
    private var startValue: HeroRevealValue? = null
    private var milestoneIndex = 0
    private var releaseHapticEmitted = false

    fun onDrag(
        startValue: HeroRevealValue,
        previousOffsetPx: Float,
        offsetPx: Float,
        fullHeightPx: Float,
    ): HapticEffect? {
        if (fullHeightPx <= 0f) {
            reset()
            return null
        }

        if (this.startValue == null) {
            this.startValue = startValue
            milestoneIndex = milestoneIndex(
                progress = dragProgress(startValue, previousOffsetPx, fullHeightPx),
                includeReverseHysteresis = false,
            )
        }

        val progress = dragProgress(this.startValue ?: startValue, offsetPx, fullHeightPx)
        val forwardIndex = milestoneIndex(progress, includeReverseHysteresis = false)
        if (forwardIndex > milestoneIndex) {
            milestoneIndex = forwardIndex
            return effectForMilestone(forwardIndex)
        }

        val reverseIndex = milestoneIndex(progress, includeReverseHysteresis = true)
        if (reverseIndex < milestoneIndex) {
            val crossedMilestone = milestoneIndex
            milestoneIndex = reverseIndex
            return effectForMilestone(crossedMilestone)
        }

        return null
    }

    fun onRelease(targetValue: HeroRevealValue): HapticEffect? {
        val gestureStartValue = startValue ?: return null
        return if (
            !releaseHapticEmitted &&
            targetValue != gestureStartValue &&
            milestoneIndex < HERO_REVEAL_HAPTIC_THRESHOLD_INDEX
        ) {
            releaseHapticEmitted = true
            HapticEffect.DRAG_THRESHOLD
        } else {
            null
        }
    }

    fun reset() {
        startValue = null
        milestoneIndex = 0
        releaseHapticEmitted = false
    }

    private fun dragProgress(
        startValue: HeroRevealValue,
        offsetPx: Float,
        fullHeightPx: Float,
    ): Float {
        val boundedOffset = offsetPx.coerceIn(0f, fullHeightPx)
        return when (startValue) {
            HeroRevealValue.Hidden -> boundedOffset / fullHeightPx
            HeroRevealValue.Expanded -> (fullHeightPx - boundedOffset) / fullHeightPx
        }.coerceIn(0f, HERO_REVEAL_POSITION_THRESHOLD)
    }

    private fun milestoneIndex(
        progress: Float,
        includeReverseHysteresis: Boolean,
    ): Int {
        val adjustedProgress = progress + if (includeReverseHysteresis) {
            HERO_REVEAL_HAPTIC_REVERSE_HYSTERESIS
        } else {
            HERO_REVEAL_HAPTIC_FLOAT_TOLERANCE
        }
        return (adjustedProgress / HERO_REVEAL_HAPTIC_STEP)
            .toInt()
            .coerceIn(0, HERO_REVEAL_HAPTIC_THRESHOLD_INDEX)
    }

    private fun effectForMilestone(index: Int): HapticEffect = when {
        index >= HERO_REVEAL_HAPTIC_THRESHOLD_INDEX -> HapticEffect.DRAG_THRESHOLD
        else -> HapticEffect.DRAG_LIGHT_TICK
    }
}

private const val HERO_REVEAL_POSITION_THRESHOLD = 0.5f
private const val HERO_REVEAL_MIN_SCALE = 0.96f
private const val HERO_REVEAL_MIN_ALPHA = 0.72f
private const val HERO_REVEAL_HAPTIC_STEP = 0.05f
private const val HERO_REVEAL_HAPTIC_REVERSE_HYSTERESIS = 0.01f
private const val HERO_REVEAL_HAPTIC_FLOAT_TOLERANCE = 0.0001f
private const val HERO_REVEAL_HAPTIC_THRESHOLD_INDEX = 10
