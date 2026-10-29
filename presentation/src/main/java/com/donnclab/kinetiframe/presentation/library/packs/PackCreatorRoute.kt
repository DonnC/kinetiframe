package com.donnclab.kinetiframe.presentation.library.packs

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.presentation.common.FullScreenDialog
import com.donnclab.kinetiframe.presentation.studio.EditorScreen
import com.donnclab.kinetiframe.ui.appViewModel

/**
 * The Pack Creator: the ordinary [EditorScreen] opened on a pack draft, with [PackCreatorConfig]
 * engaged, so animators draw pack frames in the workspace they already know. `PackDrafts`
 * describes what a draft is and how saving turns it into a library pack.
 *
 * A new pack ([editingItemId] null) starts by choosing a square canvas size and a name. A saved
 * pack ([editingItemId] set) skips that and opens with its own drawings and tags, and saving
 * replaces that same library item, so anything already shared as a `.kpack` stays a snapshot of
 * the earlier version.
 *
 * This route only saves; sharing happens from Home's Packs tab, so a pack stays private until the
 * animator chooses to send it. Closing without saving discards the draft (see
 * [PackCreatorViewModel]).
 */
@Composable
fun PackCreatorRoute(
    onExit: () -> Unit,
    editingItemId: String? = null,
    editingScope: LibraryScope = LibraryScope.PERSONAL,
    sourceProjectId: String? = null,
    onHome: (() -> Unit)? = null,
) {
    val model: PackCreatorViewModel = appViewModel {
        editingItemId?.let { set(PackCreatorViewModel.EditKey, PackToEdit(it, editingScope, sourceProjectId)) }
    }
    val state by model.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(model) {
        model.effect.collect { effect ->
            when (effect) {
                PackCreatorEffect.Saved -> onExit()
            }
        }
    }

    val draftId = state.draftId
    when {
        state.canvasDimension == null && editingItemId == null -> FullScreenDialog(onDismiss = onExit) {
            AddPackScreen(
                state = state.form,
                callbacks = AddPackCallbacks(
                    onNameChange = { model.handleIntent(PackCreatorIntent.EditForm(state.form.copy(name = it))) },
                    onDimensionChange = {
                        model.handleIntent(PackCreatorIntent.EditForm(state.form.copy(dimension = it)))
                    },
                    onBack = onExit,
                    onCreate = { model.handleIntent(PackCreatorIntent.Create) },
                ),
            )
        }

        state.draftError != null -> AlertDialog(
            onDismissRequest = onExit,
            title = { Text("Pack Creator") },
            text = { Text(state.draftError.orEmpty()) },
            confirmButton = { TextButton(onClick = onExit) { Text("OK") } },
        )

        draftId == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }

        else -> Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { padding ->
            Box(Modifier.padding(padding)) {
                EditorScreen(
                    onBackClick = onExit,
                    onHomeClick = onHome,
                    projectId = draftId,
                    packCreatorConfig = PackCreatorConfig(
                        canvasDimension = state.canvasDimension ?: 512,
                        tags = state.tags,
                        onSetTag = { frameIndex, tag -> model.handleIntent(PackCreatorIntent.SetTag(frameIndex, tag)) },
                        onSavePack = { totalFrames -> model.handleIntent(PackCreatorIntent.Save(totalFrames)) },
                        onReplaceTags = { model.handleIntent(PackCreatorIntent.ReplaceTags(it)) },
                    ),
                )
            }
        }
    }

    if (state.saving) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Saving pack...") },
            text = { CircularProgressIndicator() },
            confirmButton = {},
        )
    }
    state.saveError?.let { message ->
        AlertDialog(
            onDismissRequest = { model.handleIntent(PackCreatorIntent.DismissSaveError) },
            title = { Text("Could not save this pack") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { model.handleIntent(PackCreatorIntent.DismissSaveError) }) { Text("OK") }
            },
        )
    }
}
