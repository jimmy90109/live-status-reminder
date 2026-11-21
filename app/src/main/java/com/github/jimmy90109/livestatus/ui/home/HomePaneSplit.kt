package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize

// Window-relative physical bounds, supplied only for a vertical separating folding feature.
internal data class HomeVerticalHinge(val left: Int, val top: Int, val right: Int, val bottom: Int)
internal data class HomePaneSplit(val leadingWidth: Dp, val hingeWidth: Dp)

internal fun resolveHomePaneSplit(
    hinge: HomeVerticalHinge?,
    windowOrigin: Offset,
    size: IntSize,
    density: Density,
): HomePaneSplit? {
    if (hinge == null) return null
    val left = hinge.left - windowOrigin.x
    val right = hinge.right - windowOrigin.x
    val top = hinge.top - windowOrigin.y
    val bottom = hinge.bottom - windowOrigin.y
    if (left <= 0f || right < left || right >= size.width || bottom <= top || bottom <= 0f || top >= size.height) {
        return null
    }
    return with(density) { HomePaneSplit(left.toDp(), (right - left).toDp()) }
}
