package com.github.jimmy90109.livestatus.ui.home

import kotlin.math.abs

// Keep keyboard/accessibility focus inside the readable area, away from the edge fades.
internal fun railBringIntoViewDistance(
    offset: Float,
    childSize: Float,
    containerSize: Float,
    topFade: Float,
    bottomFade: Float,
): Float {
    if (containerSize <= 0f) return 0f
    val start = topFade.coerceIn(0f, containerSize / 4f)
    val end = containerSize - bottomFade.coerceIn(0f, containerSize / 4f)
    val leading = offset - start
    val trailing = offset + childSize - end
    return when {
        leading >= 0f && trailing <= 0f -> 0f
        leading < 0f && trailing > 0f -> 0f
        abs(leading) < abs(trailing) -> leading
        else -> trailing
    }
}
