package com.github.jimmy90109.livestatus.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsBike
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.DeliveryDining
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material.icons.rounded.Route
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.WideNavigationRail
import androidx.compose.material3.WideNavigationRailItem
import androidx.compose.material3.WideNavigationRailDefaults
import androidx.compose.material3.WideNavigationRailValue
import androidx.compose.material3.rememberWideNavigationRailState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.github.jimmy90109.livestatus.R

internal const val CATEGORY_TRANSIT_CODE = 0
internal const val CATEGORY_DELIVERY = 1
internal const val CATEGORY_RIDE = 2
internal const val CATEGORY_NAVIGATION = 3
internal const val CATEGORY_RENTAL = 4
internal const val CATEGORY_SPORT = 5
internal const val CATEGORY_TOOL = 6
internal const val CATEGORY_MEDIA = 7

internal data class AppCategory(val page: Int, @param:StringRes val labelRes: Int, val icon: ImageVector)

internal val APP_CATEGORIES = listOf(
    AppCategory(CATEGORY_TRANSIT_CODE, R.string.app_category_transit_code, Icons.Rounded.QrCode),
    AppCategory(CATEGORY_DELIVERY, R.string.app_category_delivery, Icons.Rounded.DeliveryDining),
    AppCategory(CATEGORY_RIDE, R.string.app_category_ride, Icons.Rounded.LocalTaxi),
    AppCategory(CATEGORY_NAVIGATION, R.string.app_category_navigation, Icons.Rounded.Route),
    AppCategory(CATEGORY_RENTAL, R.string.app_category_rental, Icons.AutoMirrored.Rounded.DirectionsBike),
    AppCategory(CATEGORY_SPORT, R.string.app_category_sport, Icons.Rounded.FitnessCenter),
    AppCategory(CATEGORY_TOOL, R.string.app_category_tool, Icons.Rounded.Build),
    AppCategory(CATEGORY_MEDIA, R.string.app_category_media, Icons.Rounded.PlayArrow),
)

@Composable
internal fun HomeNavigationRail(
    safeDrawingInsets: WindowInsets,
    paneWidth: Dp? = null,
    topPadding: Dp,
    bottomPadding: Dp,
    settingsSelected: Boolean,
    onOpenSettings: () -> Unit,
    onSelect: (Int) -> Unit,
) {
    val uiState = checkNotNull(LocalHomeUiState.current)
    val railState = rememberWideNavigationRailState(
        WideNavigationRailValue.Expanded,
    )
    val railColors = WideNavigationRailDefaults.colors()
    Box(
        modifier = Modifier.testTag("home_rail").fillMaxHeight()
            .then(if (paneWidth != null) Modifier.width(paneWidth) else Modifier)
            .background(railColors.containerColor),
        contentAlignment = Alignment.TopStart,
    ) {
        Column(Modifier.fillMaxHeight()) {
            Text(
                stringResource(R.string.rail_app_name),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Start))
                    .width(220.dp)
                    .padding(start = 20.dp, end = 20.dp, top = topPadding + 8.dp, bottom = 8.dp)
                    .testTag("rail_header")
                    .semantics { heading() },
            )
            Box(Modifier.weight(1f)) {
                // Material supplies the rail's default width, surface and edge-to-edge background.
                // The controls overlay it so its built-in top space can also be a scroll-under area.
                WideNavigationRail(
                    modifier = Modifier.clearAndSetSemantics {},
                    state = railState,
                    colors = railColors,
                    windowInsets = safeDrawingInsets.only(WindowInsetsSides.Start)
                        .add(WindowInsets(bottom = bottomPadding)),
                ) {
                    Spacer(Modifier.width(192.dp).fillMaxHeight())
                }
                Box(
                    Modifier.matchParentSize()
                        .windowInsetsPadding(safeDrawingInsets.only(WindowInsetsSides.Start))
                        .padding(bottom = bottomPadding),
                ) {
                    Column(
                        Modifier.width(192.dp).fillMaxHeight().selectableGroup()
                            .semantics { isTraversalGroup = true },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        HomeRailCategories(
                            scrollState = uiState.railScroll,
                            selectedPage = uiState.pager.currentPage,
                            settingsSelected = settingsSelected,
                            backgroundColor = railColors.containerColor,
                            onSelect = onSelect,
                            modifier = Modifier.weight(1f),
                        )
                        WideNavigationRailItem(
                            selected = settingsSelected,
                            onClick = rememberHapticAction(action = onOpenSettings),
                            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                            label = { Text(stringResource(R.string.settings_title)) },
                            railExpanded = true,
                            modifier = Modifier.testTag("rail_settings"),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeRailCategories(
    scrollState: ScrollState,
    selectedPage: Int,
    settingsSelected: Boolean,
    backgroundColor: Color,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val topAlpha by animateFloatAsState(
        if (scrollState.canScrollBackward) 1f else 0f,
        tween(180), label = "Rail top fade",
    )
    val bottomAlpha by animateFloatAsState(
        if (scrollState.canScrollForward) 1f else 0f,
        tween(180), label = "Rail bottom fade",
    )
    val density = LocalDensity.current
    val topFadePx = with(density) { RAIL_TOP_FADE_HEIGHT.toPx() }
    val bottomFadePx = with(density) { RAIL_BOTTOM_FADE_HEIGHT.toPx() }
    val bringIntoViewSpec = remember(topFadePx, bottomFadePx) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float) =
                railBringIntoViewDistance(offset, size, containerSize, topFadePx, bottomFadePx)
        }
    }
    Box(
        modifier.fillMaxWidth().testTag("rail_category_viewport").clipToBounds().drawWithContent {
            drawContent()
            val topHeight = topFadePx.coerceAtMost(size.height / 4f)
            val bottomHeight = bottomFadePx.coerceAtMost(size.height / 4f)
            if (topHeight > 0f && topAlpha > 0f) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(backgroundColor, backgroundColor.copy(alpha = 0f)),
                        startY = 0f, endY = topHeight,
                    ),
                    size = Size(size.width, topHeight), alpha = topAlpha,
                )
            }
            if (bottomHeight > 0f && bottomAlpha > 0f) {
                drawRect(
                    Brush.verticalGradient(
                        listOf(backgroundColor.copy(alpha = 0f), backgroundColor),
                        startY = size.height - bottomHeight, endY = size.height,
                    ),
                    topLeft = Offset(0f, size.height - bottomHeight),
                    size = Size(size.width, bottomHeight), alpha = bottomAlpha,
                )
            }
        },
    ) {
        CompositionLocalProvider(LocalBringIntoViewSpec provides bringIntoViewSpec) {
            Column(
                Modifier.fillMaxSize().testTag("rail_categories").verticalScroll(scrollState)
                    .padding(top = RAIL_TOP_FADE_HEIGHT, bottom = RAIL_BOTTOM_FADE_HEIGHT),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                APP_CATEGORIES.forEach { category ->
                    WideNavigationRailItem(
                        selected = !settingsSelected && selectedPage == category.page,
                        onClick = rememberHapticAction(HapticEffect.SELECTION) { onSelect(category.page) },
                        icon = { Icon(category.icon, contentDescription = null) },
                        label = { Text(stringResource(category.labelRes)) },
                        railExpanded = true,
                        modifier = Modifier.testTag("rail_category_${category.page}"),
                    )
                }
            }
        }
    }
}

private val RAIL_TOP_FADE_HEIGHT = 40.dp
private val RAIL_BOTTOM_FADE_HEIGHT = 24.dp
