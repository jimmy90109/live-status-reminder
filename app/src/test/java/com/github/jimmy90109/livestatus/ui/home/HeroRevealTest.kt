package com.github.jimmy90109.livestatus.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class HeroRevealTest {
    @Test
    fun initialValue_isHiddenWhenRequiredSettingsAreComplete() {
        assertEquals(
            HeroRevealValue.Hidden,
            initialHeroRevealValue(requiredSettingsComplete = true),
        )
    }

    @Test
    fun initialValue_isExpandedWhenRequiredSettingsAreIncomplete() {
        assertEquals(
            HeroRevealValue.Expanded,
            initialHeroRevealValue(requiredSettingsComplete = false),
        )
    }

    @Test
    fun target_belowHalfReturnsHidden() {
        assertEquals(
            HeroRevealValue.Hidden,
            heroRevealTarget(
                offsetPx = 49f,
                fullHeightPx = 100f,
                velocityPxPerSecond = 0f,
                velocityThresholdPxPerSecond = 125f,
            ),
        )
    }

    @Test
    fun target_atHalfReturnsExpanded() {
        assertEquals(
            HeroRevealValue.Expanded,
            heroRevealTarget(
                offsetPx = 50f,
                fullHeightPx = 100f,
                velocityPxPerSecond = 0f,
                velocityThresholdPxPerSecond = 125f,
            ),
        )
    }

    @Test
    fun target_fastDownwardFlingReturnsExpandedBeforeHalf() {
        assertEquals(
            HeroRevealValue.Expanded,
            heroRevealTarget(
                offsetPx = 10f,
                fullHeightPx = 100f,
                velocityPxPerSecond = 125f,
                velocityThresholdPxPerSecond = 125f,
            ),
        )
    }

    @Test
    fun target_fastUpwardFlingReturnsHiddenAfterHalf() {
        assertEquals(
            HeroRevealValue.Hidden,
            heroRevealTarget(
                offsetPx = 90f,
                fullHeightPx = 100f,
                velocityPxPerSecond = -125f,
                velocityThresholdPxPerSecond = 125f,
            ),
        )
    }

    @Test
    fun target_boundsOffsetBeforeApplyingPositionThreshold() {
        assertEquals(
            HeroRevealValue.Hidden,
            heroRevealTarget(
                offsetPx = -50f,
                fullHeightPx = 100f,
                velocityPxPerSecond = 0f,
                velocityThresholdPxPerSecond = 125f,
            ),
        )
        assertEquals(
            HeroRevealValue.Expanded,
            heroRevealTarget(
                offsetPx = 150f,
                fullHeightPx = 100f,
                velocityPxPerSecond = 0f,
                velocityThresholdPxPerSecond = 125f,
            ),
        )
    }

    @Test
    fun target_withUnknownHeightReturnsHidden() {
        assertEquals(
            HeroRevealValue.Hidden,
            heroRevealTarget(
                offsetPx = 0f,
                fullHeightPx = 0f,
                velocityPxPerSecond = 500f,
                velocityThresholdPxPerSecond = 125f,
            ),
        )
    }

    @Test
    fun gestureGate_allowsRevealWhenGestureStartsAtTop() {
        val gate = HeroRevealGestureGate()

        gate.onPreScroll(availableY = 10f)

        assertEquals(
            true,
            gate.canReveal(
                consumedY = 0f,
                availableY = 10f,
                isAtTop = true,
            ),
        )
    }

    @Test
    fun gestureGate_rejectsGestureThatReachesTopAfterScrollingAppCard() {
        val gate = HeroRevealGestureGate()

        gate.onPreScroll(availableY = 10f)
        assertEquals(
            false,
            gate.canReveal(
                consumedY = 10f,
                availableY = 0f,
                isAtTop = false,
            ),
        )

        assertEquals(
            false,
            gate.canReveal(
                consumedY = 0f,
                availableY = 10f,
                isAtTop = true,
            ),
        )
    }

    @Test
    fun gestureGate_resetAllowsNextGestureAtTop() {
        val gate = HeroRevealGestureGate()

        gate.onPreScroll(availableY = 10f)
        gate.canReveal(
            consumedY = 10f,
            availableY = 0f,
            isAtTop = false,
        )
        gate.reset()
        gate.onPreScroll(availableY = 10f)

        assertEquals(
            true,
            gate.canReveal(
                consumedY = 0f,
                availableY = 10f,
                isAtTop = true,
            ),
        )
    }
}
