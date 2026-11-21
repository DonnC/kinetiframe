package com.donnclab.kinetiframe.presentation.project

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.donnclab.kinetiframe.presentation.common.FullScreenDialog

/**
 * Hosts Home's "New Project" flow as its own full screen: [AddProjectFormState] lives here for
 * the flow's lifetime, and [showCanvasSize] swaps in [AddProjectCanvasSizeScreen] for the size
 * picker without adding a NavKey destination for what is really one short-lived overlay off Home.
 */
@Composable
fun AddProjectRoute(
    onDismiss: () -> Unit,
    onCreate: (name: String, canvas: CanvasSizeSelection, fps: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var formState by remember { mutableStateOf(AddProjectFormState()) }
    var showCanvasSize by remember { mutableStateOf(false) }

    FullScreenDialog(onDismiss = onDismiss) {
        if (showCanvasSize) {
            AddProjectCanvasSizeScreen(
                selection = formState.canvasSelection,
                onChange = { formState = formState.copy(canvasSelection = it) },
                onBack = { showCanvasSize = false },
                modifier = modifier,
            )
        } else {
            AddProjectScreen(
                state = formState,
                callbacks = AddProjectCallbacks(
                    onStateChange = { formState = it },
                    onOpenCanvasSize = { showCanvasSize = true },
                    onBack = onDismiss,
                    onCreate = { onCreate(formState.name, formState.canvasSelection, formState.fps) },
                ),
                modifier = modifier,
            )
        }
    }
}
