// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.presentation.common.LocalAppPreferences
import com.donnclab.kinetiframe.presentation.common.LocalRememberPenColour
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import com.donnclab.kinetiframe.studio.StudioIntent
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map

/** Dragging the colour wheel picks many colours in a row; only the one it settles on is saved. */
private const val PEN_COLOUR_PAUSE_MS = 600L

/**
 * The studio opens with the last colour picked instead of black (R3-14). Every tool shares the
 * colour (R3-21), so the pen's colour stands for all of them.
 */
@Composable
@OptIn(FlowPreview::class)
internal fun EditorPenColourMemory(viewModel: StudioViewModel) {
    val saved = LocalAppPreferences.current.penColor
    val remember by rememberUpdatedState(LocalRememberPenColour.current)
    LaunchedEffect(viewModel) {
        saved?.let { viewModel.handleIntent(StudioIntent.Draw.SetBrushColor(it)) }
        viewModel.state
            .map { it.tool.configs[DrawingTool.BRUSH]?.color }
            .distinctUntilChanged()
            .drop(1) // The colour the studio opened with is not a new pick.
            .debounce(PEN_COLOUR_PAUSE_MS)
            .collect { colour -> colour?.let(remember) }
    }
}
