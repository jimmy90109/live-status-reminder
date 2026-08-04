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
    fun visualState_staysScaledAndFadedBelowThreshold() {
        val hidden = heroRevealVisualState(visibleHeightPx = 0f, fullHeightPx = 100f)
        val justBelowThreshold = heroRevealVisualState(
            visibleHeightPx = 49.9f,
            fullHeightPx = 100f,
        )

        assertEquals(0.96f, hidden.scale, 0.001f)
        assertEquals(0.72f, hidden.alpha, 0.001f)
        assertEquals(0.96f, justBelowThreshold.scale, 0.001f)
        assertEquals(0.72f, justBelowThreshold.alpha, 0.001f)
    }

    @Test
    fun visualState_isFullyRestoredAtAndBeyondThreshold() {
        val atThreshold = heroRevealVisualState(visibleHeightPx = 50f, fullHeightPx = 100f)
        val beyondThreshold = heroRevealVisualState(visibleHeightPx = 80f, fullHeightPx = 100f)
        val unknownHeight = heroRevealVisualState(visibleHeightPx = 0f, fullHeightPx = 0f)

        listOf(atThreshold, beyondThreshold, unknownHeight).forEach { visualState ->
            assertEquals(1f, visualState.scale, 0.001f)
            assertEquals(1f, visualState.alpha, 0.001f)
        }
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

    @Test
    fun gestureGate_allowsReversingAnActiveHeroDrag() {
        val gate = HeroRevealGestureGate()

        gate.onPreScroll(availableY = -10f)
        gate.onHeroDragConsumed(consumedY = -10f)

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
    fun gestureGate_resetClearsActiveHeroDrag() {
        val gate = HeroRevealGestureGate()

        gate.onPreScroll(availableY = -10f)
        gate.onHeroDragConsumed(consumedY = -10f)
        gate.reset()
        gate.onPreScroll(availableY = -10f)

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
    fun hapticTracker_usesLightFeedbackUntilExpandedThreshold() {
        val tracker = HeroRevealHapticTracker()

        assertEquals(
            HapticEffect.DRAG_LIGHT_TICK,
            tracker.onDrag(HeroRevealValue.Hidden, 0f, 5f, 100f),
        )
        assertEquals(null, tracker.onDrag(HeroRevealValue.Hidden, 5f, 9f, 100f))
        assertEquals(
            HapticEffect.DRAG_LIGHT_TICK,
            tracker.onDrag(HeroRevealValue.Hidden, 9f, 25f, 100f),
        )
        assertEquals(
            HapticEffect.DRAG_LIGHT_TICK,
            tracker.onDrag(HeroRevealValue.Hidden, 25f, 30f, 100f),
        )
        assertEquals(
            HapticEffect.DRAG_LIGHT_TICK,
            tracker.onDrag(HeroRevealValue.Hidden, 30f, 45f, 100f),
        )
        assertEquals(
            HapticEffect.DRAG_THRESHOLD,
            tracker.onDrag(HeroRevealValue.Hidden, 45f, 50f, 100f),
        )
        assertEquals(null, tracker.onDrag(HeroRevealValue.Hidden, 50f, 80f, 100f))
    }

    @Test
    fun hapticTracker_isSymmetricWhenHidingExpandedHero() {
        val tracker = HeroRevealHapticTracker()

        assertEquals(
            HapticEffect.DRAG_LIGHT_TICK,
            tracker.onDrag(HeroRevealValue.Expanded, 100f, 95f, 100f),
        )
        assertEquals(
            HapticEffect.DRAG_LIGHT_TICK,
            tracker.onDrag(HeroRevealValue.Expanded, 75f, 70f, 100f),
        )
        assertEquals(
            HapticEffect.DRAG_THRESHOLD,
            tracker.onDrag(HeroRevealValue.Expanded, 55f, 50f, 100f),
        )
    }

    @Test
    fun hapticTracker_usesHysteresisAndTicksWhenDraggingBack() {
        val tracker = HeroRevealHapticTracker()

        tracker.onDrag(HeroRevealValue.Hidden, 0f, 50f, 100f)

        assertEquals(null, tracker.onDrag(HeroRevealValue.Hidden, 50f, 49.5f, 100f))
        assertEquals(
            HapticEffect.DRAG_THRESHOLD,
            tracker.onDrag(HeroRevealValue.Hidden, 49.5f, 48.9f, 100f),
        )
        assertEquals(
            HapticEffect.DRAG_LIGHT_TICK,
            tracker.onDrag(HeroRevealValue.Hidden, 48.9f, 43.9f, 100f),
        )
    }

    @Test
    fun hapticTracker_fastAcceptedFlingAddsOneThresholdFeedback() {
        val tracker = HeroRevealHapticTracker()

        tracker.onDrag(HeroRevealValue.Hidden, 0f, 10f, 100f)

        assertEquals(HapticEffect.DRAG_THRESHOLD, tracker.onRelease(HeroRevealValue.Expanded))
        assertEquals(null, tracker.onRelease(HeroRevealValue.Expanded))
    }

    @Test
    fun hapticTracker_doesNotRepeatThresholdFeedbackOnRelease() {
        val tracker = HeroRevealHapticTracker()

        tracker.onDrag(HeroRevealValue.Hidden, 0f, 50f, 100f)

        assertEquals(null, tracker.onRelease(HeroRevealValue.Expanded))
    }

    @Test
    fun hapticTracker_doesNotConfirmUnsuccessfulFling() {
        val tracker = HeroRevealHapticTracker()

        tracker.onDrag(HeroRevealValue.Hidden, 0f, 20f, 100f)

        assertEquals(null, tracker.onRelease(HeroRevealValue.Hidden))
    }

    @Test
    fun hapticTracker_resetStartsANewGesture() {
        val tracker = HeroRevealHapticTracker()

        assertEquals(
            HapticEffect.DRAG_LIGHT_TICK,
            tracker.onDrag(HeroRevealValue.Hidden, 0f, 5f, 100f),
        )
        tracker.reset()
        assertEquals(
            HapticEffect.DRAG_LIGHT_TICK,
            tracker.onDrag(HeroRevealValue.Hidden, 0f, 5f, 100f),
        )
    }
}
