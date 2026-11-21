package com.github.jimmy90109.livestatus.ui.home

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal val APP_CATEGORY_PAGE_COUNT = APP_CATEGORIES.size
internal val HOME_TWO_PANE_MIN_WIDTH = 840.dp
internal val HOME_SINGLE_PANE_MAX_WIDTH = 720.dp

internal fun useTwoPaneHome(availableWidth: Dp): Boolean =
    availableWidth >= HOME_TWO_PANE_MIN_WIDTH

// Owned above the layout branches so resizing never disposes user interaction state.
// Only package names and UI choices are saved, never notification contents or PINs.
internal class HomeUiState(
    val pager: PagerState,
    val pageScrolls: List<ScrollState>,
    val tabsScroll: ScrollState,
    val railScroll: ScrollState,
    val homeScroll: ScrollState,
    val settingsScroll: ScrollState,
    expandedPackages: MutableState<List<String>>,
    heroValue: MutableState<HeroRevealValue>,
) {
    var expandedPackages by expandedPackages
    var heroValue by heroValue

    fun setCardExpanded(packageName: String, expanded: Boolean) {
        expandedPackages = if (expanded) {
            (expandedPackages + packageName).distinct()
        } else {
            expandedPackages - packageName
        }
    }
}

internal val LocalHomeUiState = staticCompositionLocalOf<HomeUiState?> { null }

@Composable
internal fun rememberHomeUiState(requiredSettingsComplete: Boolean): HomeUiState {
    val pager = rememberPagerState { APP_CATEGORY_PAGE_COUNT }
    val pageScrolls = List(APP_CATEGORY_PAGE_COUNT) { rememberScrollState() }
    val tabsScroll = rememberScrollState()
    val railScroll = rememberScrollState()
    val homeScroll = rememberScrollState()
    val settingsScroll = rememberScrollState()
    val expandedPackages = rememberSaveable { mutableStateOf(emptyList<String>()) }
    val heroValue = rememberSaveable(requiredSettingsComplete) {
        mutableStateOf(initialHeroRevealValue(requiredSettingsComplete))
    }
    return remember(pager, heroValue) {
        HomeUiState(
            pager, pageScrolls, tabsScroll, railScroll, homeScroll, settingsScroll,
            expandedPackages, heroValue,
        )
    }
}
