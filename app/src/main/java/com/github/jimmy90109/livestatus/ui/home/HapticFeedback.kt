package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

internal enum class HapticEffect {
    PRESS,
    CONFIRM,
    SELECTION,
    TOGGLE_ON,
    TOGGLE_OFF,
    DRAG_LIGHT_TICK,
    DRAG_THRESHOLD,
}

internal fun HapticEffect.toFeedbackType(): HapticFeedbackType = when (this) {
    HapticEffect.PRESS -> HapticFeedbackType.VirtualKey
    HapticEffect.CONFIRM -> HapticFeedbackType.Confirm
    HapticEffect.SELECTION -> HapticFeedbackType.SegmentTick
    HapticEffect.TOGGLE_ON -> HapticFeedbackType.ToggleOn
    HapticEffect.TOGGLE_OFF -> HapticFeedbackType.ToggleOff
    HapticEffect.DRAG_LIGHT_TICK -> HapticFeedbackType.SegmentFrequentTick
    HapticEffect.DRAG_THRESHOLD -> HapticFeedbackType.Confirm
}

internal fun toggleHapticEffect(enabled: Boolean): HapticEffect = if (enabled) {
    HapticEffect.TOGGLE_ON
} else {
    HapticEffect.TOGGLE_OFF
}

internal inline fun performHapticAction(
    hapticFeedback: HapticFeedback,
    effect: HapticEffect?,
    action: () -> Unit,
) {
    effect?.let { hapticFeedback.performHapticFeedback(it.toFeedbackType()) }
    action()
}

@Composable
internal fun rememberHapticAction(
    effect: HapticEffect? = HapticEffect.PRESS,
    action: () -> Unit,
): () -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    val currentAction by rememberUpdatedState(action)
    return remember(hapticFeedback, effect) {
        {
            performHapticAction(hapticFeedback, effect, currentAction)
        }
    }
}

@Composable
internal fun rememberHapticToggleAction(
    action: (Boolean) -> Unit,
): (Boolean) -> Unit {
    val hapticFeedback = LocalHapticFeedback.current
    val currentAction by rememberUpdatedState(action)
    return remember(hapticFeedback) {
        { enabled ->
            performHapticAction(hapticFeedback, toggleHapticEffect(enabled)) {
                currentAction(enabled)
            }
        }
    }
}

@Composable
internal fun Modifier.hapticClickable(
    enabled: Boolean = true,
    role: Role? = null,
    effect: HapticEffect? = HapticEffect.PRESS,
    onClick: () -> Unit,
): Modifier = clickable(
    enabled = enabled,
    role = role,
    onClick = rememberHapticAction(effect, onClick),
)
