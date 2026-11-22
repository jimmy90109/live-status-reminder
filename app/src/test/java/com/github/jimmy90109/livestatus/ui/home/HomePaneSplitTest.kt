package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HomePaneSplitTest {
    @Test fun translatesPhysicalWindowBoundsToLocalDpWithoutAssumingHalf() {
        val split = resolveHomePaneSplit(
            HomeVerticalHinge(900, 100, 940, 1700), Offset(100f, 100f),
            IntSize(2000, 1600), Density(2f),
        )!!
        assertEquals(400.dp, split.leadingWidth)
        assertEquals(20.dp, split.hingeWidth)
    }

    @Test fun zeroWidthCreaseStillSeparatesPanes() {
        val split = resolveHomePaneSplit(
            HomeVerticalHinge(500, 0, 500, 800), Offset.Zero,
            IntSize(1000, 800), Density(1f),
        )!!
        assertEquals(500.dp, split.leadingWidth)
        assertEquals(0.dp, split.hingeWidth)
    }

    @Test fun ignoresAbsentOrNonIntersectingFeatures() {
        val size = IntSize(1000, 800)
        for (hinge in listOf(null, HomeVerticalHinge(0, 0, 0, 800),
            HomeVerticalHinge(1000, 0, 1010, 800), HomeVerticalHinge(500, 800, 510, 900),
            HomeVerticalHinge(500, -100, 510, 0), HomeVerticalHinge(510, 0, 500, 800), HomeVerticalHinge(500, 500, 510, 400))) {
            assertNull(resolveHomePaneSplit(hinge, Offset.Zero, size, Density(1f)))
        }
    }
}
