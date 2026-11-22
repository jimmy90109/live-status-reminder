package com.github.jimmy90109.livestatus.ui.home

import android.graphics.Bitmap
import android.content.res.Configuration
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import com.github.jimmy90109.livestatus.ui.theme.LiveStatusTheme
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AdaptiveHomeTest {
    @get:Rule val compose = createComposeRule()

    private val width = mutableStateOf(839.dp)
    private val height = mutableStateOf(700.dp)
    private val dark = mutableStateOf(false)
    private val brandWarning = mutableStateOf<BrandWarning?>(null)
    private val brandWarningDismissed = mutableStateOf(false)
    private val notificationAccess = mutableStateOf(true)
    private val notificationPermission = mutableStateOf(true)
    private val fontScale = mutableStateOf(1f)
    private val horizontalInset = mutableStateOf(0)
    private val verticalHinge = mutableStateOf<HomeVerticalHinge?>(null)
    private lateinit var inputModeManager: InputModeManager
    private lateinit var state: HomeUiState

    private fun showHome(ready: Boolean = true): StateRestorationTester {
        notificationAccess.value = ready
        notificationPermission.value = ready
        val restoration = StateRestorationTester(compose)
        restoration.setContent {
            val configuration = Configuration(LocalConfiguration.current).apply {
                uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                    if (dark.value) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
            }
            CompositionLocalProvider(
                LocalConfiguration provides configuration,
                LocalDensity provides Density(1f, fontScale.value),
            ) {
                LiveStatusTheme {
                    val uiState = rememberHomeUiState(notificationAccess.value && notificationPermission.value)
                    val currentInputModeManager = LocalInputModeManager.current
                    // Fixed pixel density and insets make the dp viewport deterministic on every AVD.
                    val safeInsets = WindowInsets(left = horizontalInset.value, top = 24, right = horizontalInset.value, bottom = 24)
                    SideEffect {
                        state = uiState
                        inputModeManager = currentInputModeManager
                    }
                    Box(Modifier.requiredSize(width.value, height.value)) {
                        MainScreen(
                            status = StatusSnapshot(
                                brandWarning = brandWarning.value,
                                brandWarningDismissed = brandWarningDismissed.value,
                                notificationAccess = notificationAccess.value,
                                notificationPermission = notificationPermission.value,
                            ),
                            onOpenNotificationAccess = {},
                            onRequestNotificationPermission = {},
                            onOpenSamsungNowBarGuide = {},
                            onOpenAppSettings = {},
                            onOpenPrivacyPolicy = {},
                            onDismissBrandWarning = { brandWarningDismissed.value = true },
                            onAppEnabledChange = { _, _ -> },
                            uiState = uiState,
                            safeDrawingInsets = safeInsets,
                            verticalHinge = verticalHinge.value,
                        )
                    }
                }
            }
        }
        return restoration
    }

    @Test fun narrowPushPermissionGrantInitializesHeroBeforeRendering() {
        val restoration = showHome(ready = false)
        compose.onNodeWithContentDescription("設定").assertIsDisplayed()
        compose.runOnIdle { notificationAccess.value = true }
        compose.onNodeWithContentDescription("設定").assertIsDisplayed()
        compose.runOnIdle { notificationPermission.value = true }
        compose.onNodeWithTag("home_single_pane").assertIsDisplayed()
        compose.runOnIdle { assertEquals(HeroRevealValue.Hidden, state.heroValue) }
        restoration.emulateSavedInstanceStateRestore()
        val actions = compose.onNodeWithTag("home_intro_scroll").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
        compose.runOnIdle { assertTrue(actions.single().action()) }
        compose.onNodeWithContentDescription("設定").assertIsDisplayed()
    }

    @Test fun narrowAccessGrantAndRegrantInitializeHeroBeforeRendering() {
        showHome(ready = false)
        compose.onNodeWithContentDescription("設定").assertIsDisplayed()
        compose.runOnIdle { notificationPermission.value = true }
        compose.onNodeWithContentDescription("設定").assertIsDisplayed()
        compose.runOnIdle { notificationAccess.value = true }
        compose.onNodeWithTag("home_single_pane").assertIsDisplayed()
        compose.runOnIdle { assertEquals(HeroRevealValue.Hidden, state.heroValue) }
        compose.runOnIdle { notificationAccess.value = false }
        compose.onNodeWithContentDescription("設定").assertIsDisplayed()
        compose.runOnIdle { notificationAccess.value = true }
        compose.onNodeWithTag("home_single_pane").assertIsDisplayed()
        compose.runOnIdle { assertEquals(HeroRevealValue.Hidden, state.heroValue) }
    }

    @Test fun widthBoundaryAndRestorationKeepCategoryCardAndScroll() {
        val restoration = showHome()
        compose.onNodeWithTag("home_single_pane").assertWidthIsEqualTo(720.dp)
        compose.onAllNodesWithText("外送")[0].performClick()
        compose.waitForIdle()
        val card = hasAnyAncestor(hasTestTag("app_card_com.global.foodpanda.android"))
        compose.onNode(hasText("試用即時通知功能") and card).performScrollTo().performClick()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, state.pager.currentPage)
            assertTrue(state.expandedPackages.contains("com.global.foodpanda.android"))
            runBlocking { state.pageScrolls[1].scrollTo(120) }
        }
        var originalScroll = 0
        compose.runOnIdle { originalScroll = state.pageScrolls[1].value }
        assertTrue(originalScroll > 0)
        compose.runOnIdle { width.value = 840.dp }
        compose.onNodeWithTag("home_two_pane").assertWidthIsEqualTo(840.dp)
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(1, state.pager.currentPage)
            assertTrue(state.expandedPackages.contains("com.global.foodpanda.android"))
            assertEquals(originalScroll, state.pageScrolls[1].value)
            width.value = 839.dp
        }
        compose.onNodeWithTag("home_single_pane").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(1, state.pager.currentPage)
            assertTrue(state.expandedPackages.contains("com.global.foodpanda.android"))
            assertEquals(originalScroll, state.pageScrolls[1].value)
        }
    }

    @Test fun openSettingsRestoresVisibleAndCanReturn() {
        val restoration = showHome(ready = false)
        compose.onNodeWithContentDescription("設定").performClick()
        compose.onNodeWithTag("settings_page").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("settings_page").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithTag("settings_page").assertDoesNotExist()
    }

    @Test fun settingsOpeningAnimationRestoresToVisiblePage() {
        val restoration = showHome(ready = false)
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("設定").performClick()
        compose.mainClock.advanceTimeBy(64)
        restoration.emulateSavedInstanceStateRestore()
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("settings_page").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithTag("settings_page").assertDoesNotExist()
    }

    @Test fun heroChoiceSurvivesLayoutSwitchAndRestoration() {
        val restoration = showHome()
        val actions = compose.onNodeWithTag("home_intro_scroll").fetchSemanticsNode()
            .config[androidx.compose.ui.semantics.SemanticsActions.CustomActions]
        compose.runOnIdle { assertTrue(actions.single().action()) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(HeroRevealValue.Expanded, state.heroValue)
            width.value = 840.dp
        }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { width.value = 839.dp }
        compose.onNodeWithContentDescription("設定").assertIsDisplayed()
        compose.runOnIdle { assertEquals(HeroRevealValue.Expanded, state.heroValue) }
    }

    @Test fun largeTextSettingsRemainUsable() {
        showHome(ready = false)
        compose.runOnIdle { fontScale.value = 2f }
        compose.onNodeWithContentDescription("設定").performScrollTo().performClick()
        compose.onNodeWithTag("settings_page").assertIsDisplayed()
        compose.onNodeWithContentDescription("返回").assertIsDisplayed()
        compose.onNodeWithText(InstrumentationRegistry.getInstrumentation().targetContext
            .getString(com.github.jimmy90109.livestatus.R.string.privacy_policy_link)).performScrollTo().assertIsDisplayed()
        saveScreenshot("settings-font-200")
    }

    @Test fun keyboardAndMouseCanSelectCategories() {
        width.value = 1000.dp
        showHome()
        compose.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        compose.onNodeWithTag("rail_category_1").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
        ) { assertTrue(it()) }
        compose.onNodeWithTag("rail_category_1").performKeyInput { pressKey(Key.Enter) }
        compose.runOnIdle { assertEquals(1, state.pager.currentPage) }
        compose.onNodeWithTag("rail_category_2").performMouseInput { click() }
        compose.runOnIdle { assertEquals(2, state.pager.currentPage) }
    }

    @Test fun screenshotMatrixUsesFixedData() {
        showHome(ready = false)
        val sizes = listOf(360 to 700, 800 to 360, 839 to 700, 840 to 700, 1200 to 800)
        for ((w, h) in sizes) {
            compose.runOnIdle { width.value = w.dp; height.value = h.dp }
            compose.onNodeWithTag(if (w >= 840) "home_two_pane" else "home_single_pane")
                .assertIsDisplayed()
            saveScreenshot("home-${w}x$h-light")
        }
        compose.runOnIdle { dark.value = true }
        saveScreenshot("home-1200x800-dark")
        compose.onNodeWithTag("rail_settings").performClick()
        saveScreenshot("settings-dark")
    }

    @Test fun railAlwaysShowsLabelsAcrossResizeAndRestoration() {
        width.value = 840.dp
        val restoration = showHome()
        compose.onNodeWithTag("home_rail").assertWidthIsEqualTo(220.dp)
        compose.onNodeWithTag("home_hero_card").assertDoesNotExist()
        compose.onNodeWithTag("app_tabs").assertDoesNotExist()
        compose.onNodeWithTag("rail_toggle").assertDoesNotExist()
        compose.onNodeWithText("LiveStatus").assertIsDisplayed()
        saveScreenshot("rail-fixed-expanded-light")
        compose.runOnIdle { width.value = 839.dp }
        compose.onNodeWithTag("app_tabs").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle { width.value = 840.dp }
        compose.onNodeWithTag("home_rail").assertWidthIsEqualTo(220.dp)
        compose.onNodeWithText("LiveStatus").assertIsDisplayed()
    }

    @Test fun allRailCategoriesAndPagerSwipeStaySynchronized() {
        width.value = 1000.dp
        showHome()
        for (page in 0..7) {
            compose.onNodeWithTag("rail_category_$page").performScrollTo().performClick()
            compose.onNodeWithTag("rail_category_$page").assertIsSelected()
            compose.runOnIdle { assertEquals(page, state.pager.currentPage) }
        }
        compose.onNodeWithTag("rail_category_0").performScrollTo().performClick()
        compose.onNodeWithTag("app_pager").performTouchInput { swipeLeft() }
        compose.onNodeWithTag("rail_category_1").assertIsSelected()
    }

    @Test fun wideSettingsKeepRailAndReturnToOriginalCategory() {
        width.value = 1000.dp
        val restoration = showHome()
        compose.onNodeWithTag("rail_category_2").performScrollTo().performClick()
        compose.onNodeWithTag("rail_settings").performClick().assertIsSelected()
        compose.onNodeWithTag("home_rail").assertIsDisplayed()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("rail_settings").assertIsSelected()
        androidx.test.espresso.Espresso.pressBack()
        compose.onNodeWithTag("rail_category_2").assertIsSelected()
        compose.onNodeWithTag("rail_settings").performClick()
        compose.runOnIdle { width.value = 839.dp }
        compose.onNodeWithTag("home_rail").assertDoesNotExist()
        compose.onNodeWithTag("settings_page").assertIsDisplayed()
        compose.runOnIdle { width.value = 1000.dp }
        compose.onNodeWithTag("rail_category_1").performScrollTo().performClick()
        compose.onNodeWithTag("settings_page").assertDoesNotExist()
        compose.onNodeWithTag("rail_category_1").assertIsSelected()
    }

    @Test fun permissionChangesKeepCategoryAndOnlyClampScroll() {
        width.value = 840.dp
        height.value = 360.dp
        val restoration = showHome(ready = false)
        compose.onNode(hasText("必要設定") and hasAnyAncestor(hasTestTag("category_scroll_0")))
            .assertIsDisplayed()
        compose.onNodeWithTag("rail_category_1").performScrollTo().performClick()
        compose.runOnIdle {
            runBlocking { state.pageScrolls[1].scrollTo(120) }
            notificationAccess.value = true
        }
        compose.runOnIdle { assertEquals(120, state.pageScrolls[1].value) }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(120, state.pageScrolls[1].value)
            notificationPermission.value = true
        }
        compose.runOnIdle {
            assertEquals(1, state.pager.currentPage)
            assertEquals(120.coerceAtMost(state.pageScrolls[1].maxValue), state.pageScrolls[1].value)
        }
        compose.onAllNodesWithText("必要設定").assertCountEquals(0)
        compose.runOnIdle { notificationAccess.value = false }
        compose.onNodeWithTag("rail_settings").assertIsDisplayed().performClick()
        compose.onNodeWithTag("settings_page").assertIsDisplayed()
    }

    @Test fun shortRailLargeTextDarkThemeKeepsNavigationReachable() {
        width.value = 840.dp
        height.value = 360.dp
        fontScale.value = 2f
        dark.value = true
        showHome(ready = false)
        compose.onNodeWithTag("rail_settings").assertIsDisplayed()
        compose.onNodeWithTag("rail_category_7").performScrollTo()
        compose.onNodeWithTag("rail_category_7").performClick()
        compose.runOnIdle { assertEquals(7, state.pager.currentPage) }
        compose.onNodeWithTag("rail_category_7").assertIsSelected()
        saveScreenshot("rail-short-dark-font-200")
        compose.onNodeWithTag("rail_category_0").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("rail_settings").assertIsDisplayed()
        saveScreenshot("rail-fixed-expanded-short-dark-font-200")
    }

    @Test fun brandHintScrollsWithCardsWhileRailSettingsStayFixed() {
        width.value = 1000.dp
        height.value = 360.dp
        brandWarning.value = BrandWarning.ASUS_LIVE_UPDATES
        showHome(ready = false)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        compose.onNode(
            hasText(context.getString(com.github.jimmy90109.livestatus.R.string.asus_live_updates_warning_title)) and
                hasAnyAncestor(hasTestTag("category_scroll_0")),
        ).performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("rail_settings").assertIsDisplayed()
        saveScreenshot("rail-brand-hint-short")
        compose.onNodeWithTag("app_card_com.ipass.ipassmoney").performScrollTo()
        compose.onNodeWithTag("rail_settings").assertIsDisplayed()
        compose.runOnIdle { assertTrue(state.pageScrolls[0].value > 0) }
    }

    @Test fun railBackgroundReachesSystemEdgesWhileControlsStaySafe() {
        width.value = 887.dp
        horizontalInset.value = 24
        showHome()
        compose.onNodeWithTag("home_single_pane").assertIsDisplayed()
        compose.runOnIdle { width.value = 888.dp }
        compose.onNodeWithTag("home_rail").assertIsDisplayed()
        val rail = compose.onNodeWithTag("home_rail").fetchSemanticsNode().boundsInRoot
        val header = compose.onNodeWithTag("rail_header").fetchSemanticsNode().boundsInRoot
        val settings = compose.onNodeWithTag("rail_settings").fetchSemanticsNode().boundsInRoot
        assertEquals(0f, rail.left, 0.1f)
        assertEquals(0f, rail.top, 0.1f)
        assertEquals(700f, rail.bottom, 0.1f)
        assertTrue(header.left >= 24f)
        assertEquals(32f, header.top, 0.1f)
        assertTrue(settings.bottom <= 676f)
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        assertEquals(bitmap.getPixel(4, 350), bitmap.getPixel(4, 0))
        assertEquals(bitmap.getPixel(4, 350), bitmap.getPixel(4, 699))
        saveScreenshot("rail-edge-to-edge-cutout-light")
        compose.runOnIdle {
            state.heroValue = HeroRevealValue.Expanded
            width.value = 887.dp
        }
        compose.onNodeWithContentDescription("設定").performScrollTo().performClick()
        val settingsPage = compose.onNodeWithTag("settings_page").fetchSemanticsNode().boundsInRoot
        assertTrue(settingsPage.left >= 24f && settingsPage.right <= 863f)
    }

    @Test fun wideBrandDismissalAnimatesWarningSpacingAndFollowingCards() {
        width.value = 1000.dp
        height.value = 1200.dp
        brandWarning.value = BrandWarning.ASUS_LIVE_UPDATES
        showHome()
        val page = hasAnyAncestor(hasTestTag("category_scroll_0"))
        val warning = hasTestTag("brand_warning_visibility") and page
        val card = compose.onNodeWithTag("app_card_com.ipass.ipassmoney")
        val initialHeight = compose.onNode(warning).fetchSemanticsNode().boundsInRoot.height
        val initialCardTop = card.fetchSemanticsNode().boundsInRoot.top
        compose.mainClock.autoAdvance = false
        compose.onNode(hasContentDescription("關閉品牌提示") and page).performClick()
        compose.mainClock.advanceTimeBy(112)
        val halfwayHeight = compose.onNode(warning).fetchSemanticsNode().boundsInRoot.height
        val halfwayCardTop = card.fetchSemanticsNode().boundsInRoot.top
        assertTrue(halfwayHeight > 0f && halfwayHeight < initialHeight)
        assertTrue(halfwayCardTop < initialCardTop)
        saveScreenshot("brand-dismiss-wide-halfway")
        compose.mainClock.advanceTimeBy(400)
        compose.onNode(warning).assertDoesNotExist()
        val finalCardTop = card.fetchSemanticsNode().boundsInRoot.top
        assertTrue(finalCardTop < halfwayCardTop)
        assertEquals(initialHeight, initialCardTop - finalCardTop, 1f)
        compose.mainClock.autoAdvance = true
        compose.runOnIdle {
            assertEquals(0, state.pager.currentPage)
            assertEquals(0, state.pageScrolls[0].value)
        }
        saveScreenshot("brand-dismiss-wide-complete")
    }

    @Test fun narrowBrandDismissalAlsoUsesAnimatedCollapse() {
        width.value = 600.dp
        height.value = 1200.dp
        brandWarning.value = BrandWarning.ASUS_LIVE_UPDATES
        showHome()
        val warning = compose.onNodeWithTag("brand_warning_visibility")
        val initialHeight = warning.fetchSemanticsNode().boundsInRoot.height
        compose.mainClock.autoAdvance = false
        compose.onNodeWithContentDescription("關閉品牌提示").performClick()
        compose.mainClock.advanceTimeBy(112)
        val halfwayHeight = warning.fetchSemanticsNode().boundsInRoot.height
        assertTrue(halfwayHeight > 0f && halfwayHeight < initialHeight)
        compose.mainClock.advanceTimeBy(400)
        warning.assertDoesNotExist()
        compose.mainClock.autoAdvance = true
        compose.onNodeWithTag("home_single_pane").assertIsDisplayed()
    }

    @Test fun bookPostureUsesActualHingeAndKeepsSettingsAndCategory() {
        width.value = 1000.dp
        height.value = 800.dp
        val restoration = showHome()
        compose.onNodeWithTag("rail_category_1").performScrollTo().performClick()
        val controlTags = listOf("rail_header", "rail_category_1", "rail_settings")
        val originalLefts = controlTags.associateWith {
            compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.left
        }
        compose.runOnIdle { verticalHinge.value = HomeVerticalHinge(500, 0, 520, 800) }
        compose.onNodeWithTag("home_rail").assertWidthIsEqualTo(500.dp)
        controlTags.forEach {
            assertEquals(originalLefts.getValue(it), compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.left, 0.1f)
        }
        assertEquals(520f, compose.onNodeWithTag("home_content_pane").fetchSemanticsNode().boundsInRoot.left, 0.1f)
        compose.onNodeWithTag("home_hinge_gap").assertWidthIsEqualTo(20.dp)
        var savedScroll = 0
        compose.runOnIdle {
            runBlocking { state.pageScrolls[1].scrollTo(80) }
            savedScroll = state.pageScrolls[1].value
            assertTrue(savedScroll > 0)
        }
        compose.onNodeWithTag("rail_settings").performClick().assertIsSelected()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithTag("home_rail").assertWidthIsEqualTo(500.dp)
        compose.runOnIdle { assertEquals(savedScroll, state.pageScrolls[1].value) }
        assertEquals(520f, compose.onNodeWithTag("settings_page").fetchSemanticsNode().boundsInRoot.left, 0.1f)
        saveScreenshot("book-hinge-settings-light")
        compose.onNodeWithContentDescription("返回").performClick()
        compose.onNodeWithTag("rail_category_1").assertIsSelected()
        compose.runOnIdle { verticalHinge.value = HomeVerticalHinge(420, 0, 430, 800) }
        compose.onNodeWithTag("home_rail").assertWidthIsEqualTo(420.dp)
        controlTags.forEach {
            assertEquals(originalLefts.getValue(it), compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot.left, 0.1f)
        }
        assertEquals(430f, compose.onNodeWithTag("home_content_pane").fetchSemanticsNode().boundsInRoot.left, 0.1f)
        saveScreenshot("book-offset-hinge-light")
        compose.runOnIdle {
            dark.value = true
            verticalHinge.value = HomeVerticalHinge(500, 0, 500, 800)
        }
        compose.onNodeWithTag("home_rail").assertWidthIsEqualTo(500.dp)
        saveScreenshot("book-zero-width-fold-dark")
        compose.runOnIdle { verticalHinge.value = null }
        compose.onNodeWithTag("home_rail").assertWidthIsEqualTo(220.dp)
        compose.onNodeWithTag("rail_category_1").assertIsSelected()
    }

    @Test fun shortRailScrollsUnderBothFadesAndKeepsSettingsFixed() {
        width.value = 840.dp
        height.value = 360.dp
        val restoration = showHome()
        val headerBounds = compose.onNodeWithTag("rail_header").fetchSemanticsNode().boundsInRoot
        val settingsBounds = compose.onNodeWithTag("rail_settings").fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("rail_category_viewport").getUnclippedBoundsInRoot()
        compose.runOnIdle {
            assertTrue(!state.railScroll.canScrollBackward)
            assertTrue(state.railScroll.canScrollForward)
            runBlocking { state.railScroll.scrollTo(20) }
        }
        val first = compose.onNodeWithTag("rail_category_0").getUnclippedBoundsInRoot()
        assertTrue(first.top > viewport.top && first.top < viewport.top + 40.dp)
        compose.runOnIdle {
            assertTrue(state.railScroll.canScrollBackward && state.railScroll.canScrollForward)
            runBlocking { state.railScroll.scrollTo(100) }
        }
        saveScreenshot("rail-fades-light-middle")
        compose.runOnIdle { runBlocking { state.railScroll.scrollTo(20) } }
        restoration.emulateSavedInstanceStateRestore()
        compose.runOnIdle {
            assertEquals(20, state.railScroll.value)
            width.value = 839.dp
        }
        compose.onNodeWithTag("home_single_pane").assertIsDisplayed()
        compose.runOnIdle { width.value = 840.dp }
        compose.runOnIdle {
            assertEquals(20, state.railScroll.value)
            runBlocking { state.railScroll.scrollTo(state.railScroll.maxValue) }
        }
        val last = compose.onNodeWithTag("rail_category_7").getUnclippedBoundsInRoot()
        assertTrue(last.bottom <= viewport.bottom - 24.dp)
        compose.runOnIdle {
            assertTrue(state.railScroll.canScrollBackward && !state.railScroll.canScrollForward)
        }
        compose.onNodeWithTag("rail_category_7").performClick().assertIsSelected()
        assertEquals(headerBounds, compose.onNodeWithTag("rail_header").fetchSemanticsNode().boundsInRoot)
        assertEquals(settingsBounds, compose.onNodeWithTag("rail_settings").fetchSemanticsNode().boundsInRoot)
        compose.onNodeWithTag("rail_settings").assertIsDisplayed()
        saveScreenshot("rail-fades-light-bottom")
        compose.runOnIdle {
            dark.value = true
            fontScale.value = 2f
            runBlocking { state.railScroll.scrollTo(100) }
        }
        saveScreenshot("rail-fades-dark-font-200")
    }

    @Test fun keyboardFocusAvoidsRailFadeAreas() {
        width.value = 840.dp
        height.value = 360.dp
        showHome()
        compose.runOnIdle { assertTrue(inputModeManager.requestInputMode(InputMode.Keyboard)) }
        compose.onNodeWithTag("rail_category_7").performScrollTo().performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
        ) { assertTrue(it()) }
        val viewport = compose.onNodeWithTag("rail_category_viewport").getUnclippedBoundsInRoot()
        val last = compose.onNodeWithTag("rail_category_7").getUnclippedBoundsInRoot()
        assertTrue(last.bottom <= viewport.bottom - 24.dp)
        compose.onNodeWithTag("rail_category_0").performSemanticsAction(
            androidx.compose.ui.semantics.SemanticsActions.RequestFocus,
        ) { assertTrue(it()) }
        val first = compose.onNodeWithTag("rail_category_0").getUnclippedBoundsInRoot()
        assertTrue(first.top >= viewport.top + 40.dp)
        compose.onNodeWithTag("rail_category_0").performKeyInput { pressKey(Key.Enter) }
        compose.onNodeWithTag("rail_category_0").assertIsSelected()
    }

    private fun saveScreenshot(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val outputPath = InstrumentationRegistry.getArguments().getString("additionalTestOutputDir")
        val outputDirectory = outputPath?.let(::File)
            ?: File(context.externalMediaDirs.first(), "additional_test_output")
        val folder = File(outputDirectory, "adaptive-test-screenshots")
        folder.mkdirs()
        File(folder, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
}
