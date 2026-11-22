package com.github.jimmy90109.livestatus.ui.home

import android.content.res.Configuration
import android.os.SystemClock
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SettingsRecreationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    @Test fun settingsSurviveActivityRecreation() {
        val wide = compose.onAllNodesWithTag("home_rail").fetchSemanticsNodes().isNotEmpty()
        if (wide) {
            compose.onNodeWithTag("rail_category_1").performScrollTo().performClick()
        }
        openSettings()
        compose.onNodeWithTag("settings_page").assertIsDisplayed()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("settings_page").assertIsDisplayed()
        if (wide) compose.onNodeWithText("LiveStatus").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithTag("settings_page").assertDoesNotExist()
    }

    @Test(timeout = 60_000) fun settingsSurviveFoldUnfoldAndRotation() {
        val states = shell("cmd device_state print-states")
        assumeTrue(states.contains("identifier=0, name='CLOSED'"))
        assumeTrue(states.contains("identifier=2, name='OPENED'"))
        openSettings()
        try {
            shell("cmd device_state state 0")
            waitForLayout("home_single_pane")
            compose.onNodeWithTag("settings_page").assertIsDisplayed()
            shell("cmd device_state state 2")
            waitForLayout("home_two_pane")
            compose.onNodeWithTag("settings_page").assertIsDisplayed()
            shell("wm user-rotation lock 1")
            compose.waitUntil(20_000) {
                runCatching {
                    compose.activity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
                }.getOrDefault(false)
            }
            compose.onNodeWithTag("settings_page").assertIsDisplayed()
            compose.onNodeWithContentDescription("返回").performClick()
            compose.onNodeWithTag("settings_page").assertDoesNotExist()
        } finally {
            shell("wm user-rotation free")
            shell("cmd device_state state reset")
        }
    }

    @Test(timeout = 60_000) fun halfOpenedBookPostureKeepsSettingsBesideHinge() {
        val states = shell("cmd device_state print-states")
        assumeTrue(states.contains("identifier=1, name='HALF_OPENED'"))
        try {
            shell("cmd device_state state 2")
            shell("wm user-rotation lock 0")
            waitForPortraitWindow()
            waitForLayout("home_two_pane")
            // Establish the selected category before changing posture, rather than racing its animation.
            compose.onNodeWithTag("rail_category_1").performScrollTo().performClick().assertIsSelected()
            shell("cmd device_state state 1")
            waitForHinge()
            compose.onNodeWithTag("rail_category_1").assertIsSelected()
            val rail = compose.onNodeWithTag("home_rail").fetchSemanticsNode().boundsInRoot
            val window = compose.onNodeWithTag("home_two_pane").fetchSemanticsNode().boundsInRoot
            val content = compose.onNodeWithTag("home_content_pane").fetchSemanticsNode().boundsInRoot
            assertTrue(rail.width / window.width in 0.35f..0.65f)
            assertTrue(content.left >= rail.right)
            openSettings()
            val privacyLabel = InstrumentationRegistry.getInstrumentation().targetContext
                .getString(com.github.jimmy90109.livestatus.R.string.privacy_policy_link)
            compose.onNodeWithText(privacyLabel).performScrollTo()
            compose.activityRule.scenario.recreate()
            waitForHinge()
            compose.onNodeWithText(privacyLabel).assertIsDisplayed()
            val restoredRail = compose.onNodeWithTag("home_rail").fetchSemanticsNode().boundsInRoot
            val settings = compose.onNodeWithTag("settings_page").fetchSemanticsNode().boundsInRoot
            assertTrue(settings.left >= restoredRail.right)
            compose.onNodeWithContentDescription("返回").performClick()
            compose.onNodeWithTag("rail_category_1").assertIsSelected()
        } finally {
            shell("cmd device_state state reset")
            shell("wm user-rotation free")
        }
    }

    private fun waitForPortraitWindow() {
        // Changing posture can restore a per-posture rotation before the explicit lock applies.
        // Wait for that configuration sequence to settle without touching a retiring Compose root.
        var previous: Triple<Int, Int, Int>? = null
        var stableSince = SystemClock.uptimeMillis()
        compose.waitUntil(15_000) {
            val configuration = InstrumentationRegistry.getInstrumentation().targetContext
                .resources.configuration
            val current = Triple(configuration.orientation, configuration.screenWidthDp, configuration.screenHeightDp)
            val now = SystemClock.uptimeMillis()
            if (current != previous || configuration.orientation != Configuration.ORIENTATION_PORTRAIT) {
                previous = current
                stableSince = now
            }
            configuration.orientation == Configuration.ORIENTATION_PORTRAIT && now - stableSince >= 1_000
        }
    }

    private fun waitForHinge() {
        compose.waitUntil(15_000) {
            compose.onAllNodesWithTag("home_hinge_gap").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun openSettings() {
        if (compose.onAllNodesWithTag("rail_settings").fetchSemanticsNodes().isNotEmpty()) {
            compose.onNodeWithTag("rail_settings").performClick()
        } else {
            compose.onNodeWithContentDescription("設定").performScrollTo().performClick()
        }
    }

    private fun waitForLayout(tag: String) {
        // Wait for resources before querying semantics: the old root may be detached during folding.
        val wide = tag == "home_two_pane"
        compose.waitUntil(15_000) {
            val width = InstrumentationRegistry.getInstrumentation().targetContext
                .resources.configuration.screenWidthDp
            (width >= 840) == wide
        }
        compose.onNodeWithTag(tag).assertIsDisplayed()
    }

    private fun shell(command: String): String {
        val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand(command)
        return android.os.ParcelFileDescriptor.AutoCloseInputStream(descriptor)
            .bufferedReader().use { it.readText() }
    }
}
