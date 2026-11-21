// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.studio.StudioIntent

/** What the editor's bars, panels and canvas overlays can ask the screen to do. */
internal data class EditorCommands(
    val onIntent: (StudioIntent) -> Unit,
    /** Leaves for the given destination, asking to save first where there is unsaved work. */
    val requestExit: (() -> Unit) -> Unit,
    val saves: EditorSaveActions,
    val actorSaves: ActorStudioSaves,
    /** Prepares a performance off the main thread, then places it in the project. */
    val placeActor: (ActorPlacementReference) -> Unit,
    val openSketch: () -> Unit,
    val pickers: EditorMediaPickers,
    val exports: EditorLibraryExports,
)
