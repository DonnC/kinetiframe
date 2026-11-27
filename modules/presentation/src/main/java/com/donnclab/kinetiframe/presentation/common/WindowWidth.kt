// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import com.donnclab.kinetiframe.designsystem.theme.Dimens

/**
 * How much width the window has, by the Material breakpoints (600 and 840 dp). Layouts follow the
 * window, not the device: a foldable, a tablet in split screen and a phone in landscape each get
 * the layout that fits the room they have.
 */
enum class WindowWidthClass {
    /** Phones in portrait. */
    COMPACT,

    /** Foldables unfolded, small tablets in portrait, phones in landscape. */
    MEDIUM,

    /** Tablets in landscape and large tablets. */
    EXPANDED,
    ;

    val isCompact: Boolean get() = this == COMPACT
}

private val MEDIUM_FROM = Dimens.windowMediumFrom
private val EXPANDED_FROM = Dimens.windowExpandedFrom

/** The current window's [WindowWidthClass]. */
@Composable
fun windowWidthClass(): WindowWidthClass {
    val width: Dp = with(LocalDensity.current) { LocalWindowInfo.current.containerSize.width.toDp() }
    return when {
        width >= EXPANDED_FROM -> WindowWidthClass.EXPANDED
        width >= MEDIUM_FROM -> WindowWidthClass.MEDIUM
        else -> WindowWidthClass.COMPACT
    }
}

/**
 * Columns for the browsing grids (projects, actors, packs): two on a phone, as before, and as many
 * cards of a comfortable width as fit on anything wider, instead of two stretched ones.
 */
@Composable
fun browseGridCells(): GridCells =
    if (windowWidthClass().isCompact) GridCells.Fixed(Dimens.gridColumns) else GridCells.Adaptive(Dimens.gridCellWide)
