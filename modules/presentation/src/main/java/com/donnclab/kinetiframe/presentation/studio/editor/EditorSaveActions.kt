// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.editor

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.UiText
import com.donnclab.kinetiframe.presentation.common.toUiText
import com.donnclab.kinetiframe.presentation.studio.StudioViewModel
import com.donnclab.kinetiframe.presentation.studio.dialogs.SaveBeforeExitDialog
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
    val storage: EditorStorageViewModel,
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
    private val project: ProjectSaveTarget?,
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
                messages.projectSaveMessage = error.toUiText(R.string.editor_pack_not_saved)
                return@launch
            }
        }
    }

    /**
     * Saves when anything changed, then opens [destination], which reads the saved project, such
     * as export. Nothing to save opens it straight away.
     */
    fun saveThen(destination: () -> Unit) {
        val save = saveProject ?: return destination()
        scope.launch {
            if (viewModel.hasUnsavedChanges()) {
                exit.destination = destination
                exit.returnHomeAfterSave = true
                save()
            } else {
                destination()
            }
        }
    }

    /** Leaves straight away when nothing changed since the last save; otherwise asks first. */
    fun confirmExit(destination: () -> Unit) {
        scope.launch {
            if (viewModel.hasUnsavedChanges()) {
                exit.destination = destination
                exit.showConfirmation = true
            } else {
                destination()
            }
        }
    }

    /**
     * Autosave: saves quietly when something changed. It never leaves the editor, shows no
     * notice and keeps temporary frame references. It stands aside while a save to leave is
     * underway.
     */
    fun autosave() {
        val target = project ?: return
        if (exit.returnHomeAfterSave) return
        scope.launch {
            if (!exit.returnHomeAfterSave && viewModel.hasUnsavedChanges()) writeProject(target, quiet = true)
        }
    }

    private fun writeProject(target: ProjectSaveTarget, quiet: Boolean = false) {
        // Capture intent before queuing behind another save. An earlier toolbar save
        // must not satisfy a later Home request with an older document snapshot.
        val leaveWhenSaved = !quiet && exit.returnHomeAfterSave
        viewModel.launchSave {
            val time = System.currentTimeMillis()
            val document = try {
                viewModel.prepareProjectSave(target.nodeDensity)
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                messages.projectSaveMessage = error.toUiText(R.string.editor_project_not_saved)
                if (leaveWhenSaved) exit.returnHomeAfterSave = false
                return@launchSave
            }
            val result = target.storage.saveProject(
                target.projectId,
                document,
                target.draftFrames,
                time,
                viewModel.cacheManager,
            )
            when (result) {
                is StoreResult.Value -> {
                    viewModel.markSaved(document)
                    if (quiet) return@launchSave
                    viewModel.frameReferences.clear()
                    if (leaveWhenSaved) {
                        exit.returnHomeAfterSave = false
                        exit.destination?.invoke()
                    }
                    // Success is a brief notice; only a failure needs the animator to respond.
                    messages.notice = UiText.Resource(R.string.editor_project_saved)
                }

                is StoreResult.Failure -> {
                    if (leaveWhenSaved) exit.returnHomeAfterSave = false
                    messages.projectSaveMessage = result.toUiText()
                }
            }
        }
    }
}

/** The "save before leaving?" prompt, and the progress dialog while that save runs. */
@Composable
internal fun EditorExitDialogs(exit: EditorExitState, saves: EditorSaveActions, isPackCreator: Boolean) {
    if (exit.returnHomeAfterSave) {
        AlertDialog(onDismissRequest = {
        }, text = { Text(stringResource(R.string.editor_saving_project)) }, confirmButton = {})
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
