// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.workspace

import androidx.compose.runtime.Composable
import com.donnclab.kinetiframe.presentation.common.WindowWidthClass
import com.donnclab.kinetiframe.presentation.common.windowWidthClass
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.StudioState

/**
 * Whether the drawing studio lays out as the tablet workspace: fixed tool and layer panels around
 * the canvas, a labelled top bar and docked settings. Only on expanded windows, where the panels
 * still leave a large canvas, and only for drawing; Actor Studio and Pack Creator keep their own.
 */
@Composable
internal fun usesWorkspace(state: StudioState, isActorStudio: Boolean, isPackCreator: Boolean): Boolean =
    windowWidthClass() == WindowWidthClass.EXPANDED && !isActorStudio && !isPackCreator &&
        state.scene.studioMode == StudioMode.RASTER_DRAWING
