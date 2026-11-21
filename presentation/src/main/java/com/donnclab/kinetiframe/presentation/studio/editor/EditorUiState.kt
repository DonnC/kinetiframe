// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember

/** The editor screen's own UI state: everything it shows that is not part of the document. */
@Stable
internal class EditorUiState(
    val drawerState: DrawerState,
    /** Kept here so the frame strip keeps its position while focus mode removes the dock. */
    val timelineScrollState: LazyListState,
) {
    val panels = EditorPanels()
    val messages = EditorMessages()
    val artwork = ActorArtworkState()
    val tray = PackTrayState()
    val exit = EditorExitState()
}

@Composable
internal fun rememberEditorUiState(): EditorUiState {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val timelineScrollState = rememberLazyListState()
    return remember(drawerState, timelineScrollState) { EditorUiState(drawerState, timelineScrollState) }
}
