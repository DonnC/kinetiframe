package com.donnclab.kinetiframe.studio

import androidx.compose.ui.geometry.Rect
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

@OptIn(ExperimentalCoroutinesApi::class)
class SelectionInteractionModeTest {
    @Test fun `mode changes close context menu without changing artwork or pivot`(@TempDir directory: Path) = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val initial = StudioState(
                selection = SelectionState(
                    active = true,
                    bounds = Rect(10f, 10f, 14f, 14f),
                    contextMenuVisible = true,
                ),
            )
            val editor = StudioViewModel(
                directory.toFile(),
                engineDispatcher = dispatcher,
                shouldInitialize = false,
                initialEditorState = initial,
            )
            editor.handleIntent(StudioIntent.Selection.SetInteractionMode(SelectionInteractionMode.MOVE))
            val moved = editor.state.value
            assertEquals(SelectionInteractionMode.MOVE, moved.selection.interactionMode)
            assertFalse(moved.selection.contextMenuVisible)
            assertEquals(initial.project, moved.project)
            assertEquals(initial.selection.bounds, moved.selection.bounds)
            assertEquals(initial.selection.pivotOffset, moved.selection.pivotOffset)
            editor.handleIntent(StudioIntent.Selection.SetInteractionMode(SelectionInteractionMode.TRANSFORM))
            assertEquals(SelectionInteractionMode.TRANSFORM, editor.state.value.selection.interactionMode)
        } finally {
            Dispatchers.resetMain()
        }
    }
}
