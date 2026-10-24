// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.studio.StudioViewModel
import com.donnclab.kinetiframe.studio.ui.dialogs.SaveBeforeExitDialog
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.io.File

/** The save-before-leaving flow. */
@Stable
internal class EditorExitState {
    /** True while a save started from the exit prompt runs; the editor leaves when it succeeds. */
    var returnHomeAfterSave by mutableStateOf(false)
    var showConfirmation by mutableStateOf(false)
    var destination by mutableStateOf<(() -> Unit)?>(null)

    /** Frame count of a prepared Pack Creator save, waiting for the animator to confirm its tags. */
    var pendingPackSaveFrames by mutableStateOf<Int?>(null)
}

/** Where a gallery project is saved. */
internal class ProjectSaveTarget(
    val projectId: String,
    val writer: ProjectSaveWriter,
    /** The working frame directory to publish, or null when the editor draws into the project directly. */
    val draftFrames: File?,
    val nodeDensity: Float,
)

/** Saves gallery projects and Pack Creator drafts, and asks before leaving with unsaved work. */
internal class EditorSaveActions(
    private val viewModel: StudioViewModel,
    private val messages: EditorMessages,
    private val exit: EditorExitState,
    private val scope: CoroutineScope,
    project: ProjectSaveTarget?,
) {
    /** Null when the editor has no gallery project to save into. */
    val saveProject: (() -> Unit)? = project?.let { target -> { writeProject(target) } }

    fun savePack() {
        scope.launch {
            // Pack Creator exports the timeline the animator is looking at, including frames
            // added in this session, not the scratch document's saved frame count.
            exit.pendingPackSaveFrames = try {
                viewModel.prepareProjectSave().totalFrames
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                messages.projectSaveMessage = error.message ?: "Pack was not saved"
                return@launch
            }
        }
    }

    fun confirmExit(destination: () -> Unit) {
        exit.destination = destination
        exit.showConfirmation = true
    }

    private fun writeProject(target: ProjectSaveTarget) {
        // Capture intent before queuing behind another save. An earlier toolbar save
        // must not satisfy a later Home request with an older document snapshot.
        val leaveWhenSaved = exit.returnHomeAfterSave
        viewModel.launchSave {
            val time = System.currentTimeMillis()
            val document = try {
                viewModel.prepareProjectSave(target.nodeDensity)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                messages.projectSaveMessage = error.message ?: "Project was not saved"
                if (leaveWhenSaved) exit.returnHomeAfterSave = false
                return@launchSave
            }
            val result = target.writer.write(target.projectId, document, target.draftFrames, time)
            messages.projectSaveMessage = when (result) {
                is StoreResult.Value -> {
                    viewModel.frameReferences.clear()
                    if (leaveWhenSaved) {
                        exit.returnHomeAfterSave = false
                        exit.destination?.invoke()
                    }
                    "Project saved"
                }

                is StoreResult.Failure -> {
                    if (leaveWhenSaved) exit.returnHomeAfterSave = false
                    result.message
                }
            }
        }
    }
}

/** The "save before leaving?" prompt, and the progress dialog while that save runs. */
@Composable
internal fun EditorExitDialogs(exit: EditorExitState, saves: EditorSaveActions, isPackCreator: Boolean) {
    if (exit.returnHomeAfterSave) {
        AlertDialog(onDismissRequest = {}, text = { Text("Saving project…") }, confirmButton = {})
    }
    if (!exit.showConfirmation) return
    SaveBeforeExitDialog(
        onSave = {
            exit.showConfirmation = false
            if (isPackCreator) {
                saves.savePack()
            } else {
                exit.returnHomeAfterSave = true
                saves.saveProject?.invoke()
            }
        },
        onDiscard = {
            exit.showConfirmation = false
            exit.returnHomeAfterSave = false
            exit.destination?.invoke()
        },
        onCancel = {
            exit.showConfirmation = false
            exit.destination = null
        },
        canSave = isPackCreator || saves.saveProject != null,
    )
}
