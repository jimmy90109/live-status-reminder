package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import org.junit.Assert.assertEquals
import org.junit.Test

class HapticFeedbackTest {
    @Test
    fun effectsMapToSemanticFeedbackTypes() {
        assertEquals(HapticFeedbackType.VirtualKey, HapticEffect.PRESS.toFeedbackType())
        assertEquals(HapticFeedbackType.Confirm, HapticEffect.CONFIRM.toFeedbackType())
        assertEquals(HapticFeedbackType.SegmentTick, HapticEffect.SELECTION.toFeedbackType())
        assertEquals(HapticFeedbackType.ToggleOn, HapticEffect.TOGGLE_ON.toFeedbackType())
        assertEquals(HapticFeedbackType.ToggleOff, HapticEffect.TOGGLE_OFF.toFeedbackType())
        assertEquals(
            HapticFeedbackType.SegmentFrequentTick,
            HapticEffect.DRAG_LIGHT_TICK.toFeedbackType(),
        )
        assertEquals(
            HapticFeedbackType.Confirm,
            HapticEffect.DRAG_THRESHOLD.toFeedbackType(),
        )
    }

    @Test
    fun toggleEffectUsesResultingState() {
        assertEquals(HapticEffect.TOGGLE_ON, toggleHapticEffect(true))
        assertEquals(HapticEffect.TOGGLE_OFF, toggleHapticEffect(false))
    }

    @Test
    fun hapticActionPerformsFeedbackAndCallbackOnce() {
        val hapticFeedback = RecordingHapticFeedback()
        var actionCount = 0

        performHapticAction(hapticFeedback, HapticEffect.CONFIRM) {
            actionCount += 1
        }

        assertEquals(listOf(HapticFeedbackType.Confirm), hapticFeedback.feedbackTypes)
        assertEquals(1, actionCount)
    }

    @Test
    fun actionCanRunWithoutHapticFeedback() {
        val hapticFeedback = RecordingHapticFeedback()
        var actionCount = 0

        performHapticAction(hapticFeedback, null) {
            actionCount += 1
        }

        assertEquals(emptyList<HapticFeedbackType>(), hapticFeedback.feedbackTypes)
        assertEquals(1, actionCount)
    }
}

private class RecordingHapticFeedback : HapticFeedback {
    val feedbackTypes = mutableListOf<HapticFeedbackType>()

    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        feedbackTypes += hapticFeedbackType
    }
}
