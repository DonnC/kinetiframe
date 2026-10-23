// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import com.donnclab.kinetiframe.actors.ui.hasActorStudioChanges
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioViewModel
import com.donnclab.kinetiframe.studio.activeActor
import com.donnclab.kinetiframe.studio.actorDefinition
import com.donnclab.kinetiframe.studio.actorPreviewDuration
import kotlinx.coroutines.CancellationException

/**
 * Saves made from Actor Studio's canvas. Each one commits the canvas first and reads the current
 * state when it runs, so a save started before an edit never stores an older snapshot.
 */
internal class ActorStudioSaves(
    private val viewModel: StudioViewModel,
    private val messages: EditorMessages,
    private val host: EditorHost,
) {
    /** Saves the actor revision. Null when the host doesn't save actors. */
    val saveActor: (() -> Unit)? = host.onSaveActorRevision?.let { save ->
        {
            viewModel.launchSave {
                try {
                    viewModel.prepareProjectSave()
                    viewModel.state.value.actorDefinition()?.let { save(it, viewModel.rigAssetManager) }
                } catch (error: Exception) {
                    if (error is CancellationException) throw error
                    messages.projectSaveMessage = error.message ?: "Actor was not saved"
                }
            }
        }
    }

    /** Keeps any take in progress, then saves the performance or the actor revision being authored. */
    fun saveOnExit() {
        viewModel.launchSave {
            try {
                keepTake()
                viewModel.prepareProjectSave()
                val current = viewModel.state.value
                if (current.actor.isPerformanceAuthoring) {
                    val instance = requireNotNull(current.activeActor())
                    requireNotNull(host.onSavePerformance)(instance, current.actorPreviewDuration())
                } else {
                    requireNotNull(host.onSaveActorRevision)(
                        requireNotNull(current.actorDefinition()),
                        viewModel.rigAssetManager,
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                messages.projectSaveMessage = error.message ?: "Studio work was not saved"
                host.onActorSavePreparationFailed?.invoke()
            }
        }
    }

    /** Keeps any take in progress, reports whether there is unsaved work, then leaves Actor Studio. */
    fun exit() {
        viewModel.launchSave {
            try {
                keepTake()
                viewModel.prepareProjectSave()
                host.onActorStudioDirtyChanged?.invoke(
                    hasActorStudioChanges(host.initialEditorState, viewModel.state.value),
                )
                host.onExitActorStudio?.invoke()
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                messages.projectSaveMessage = error.message ?: "Could not prepare studio exit"
            }
        }
    }

    private fun keepTake() {
        viewModel.handleIntent(StudioIntent.ActorMotion.StopTake)
        viewModel.handleIntent(StudioIntent.ActorMotion.KeepTake)
    }
}
