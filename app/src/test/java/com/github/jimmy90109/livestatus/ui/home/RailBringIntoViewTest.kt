package com.github.jimmy90109.livestatus.ui.home

import org.junit.Assert.assertEquals
import org.junit.Test

class RailBringIntoViewTest {
    @Test fun keepsVisibleFocusStillAndMovesCoveredEdgesOutOfFades() {
        assertEquals(0f, railBringIntoViewDistance(80f, 56f, 216f, 40f, 24f), 0.01f)
        assertEquals(-20f, railBringIntoViewDistance(20f, 56f, 216f, 40f, 24f), 0.01f)
        assertEquals(44f, railBringIntoViewDistance(180f, 56f, 216f, 40f, 24f), 0.01f)
    }

    @Test fun oversizedFocusDoesNotOscillateBetweenEdges() {
        assertEquals(0f, railBringIntoViewDistance(20f, 200f, 216f, 40f, 24f), 0.01f)
        assertEquals(0f, railBringIntoViewDistance(0f, 56f, 0f, 40f, 24f), 0.01f)
    }

    @Test fun limitsFadeInsetsWhenTheViewportIsVeryShort() {
        assertEquals(-5f, railBringIntoViewDistance(20f, 30f, 100f, 40f, 24f), 0.01f)
        assertEquals(0f, railBringIntoViewDistance(25f, 30f, 100f, 40f, 24f), 0.01f)
    }
}
