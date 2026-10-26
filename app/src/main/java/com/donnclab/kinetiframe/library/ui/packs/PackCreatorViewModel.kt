package com.donnclab.kinetiframe.library.ui.packs

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.library.domain.PackDrafts
import com.donnclab.kinetiframe.library.model.PackFrameTag
import com.donnclab.kinetiframe.ui.BaseViewModel
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Pack Creator's state. [canvasDimension] stays null while a new pack's size is being chosen;
 * [draftId] is set once its draft exists and the editor can open it.
 */
data class PackCreatorState(
    val form: AddPackFormState = AddPackFormState(),
    val packName: String = "",
    val canvasDimension: Int? = null,
    val draftId: String? = null,
    val draftError: String? = null,
    val tags: Map<Int, PackFrameTag> = emptyMap(),
    val saving: Boolean = false,
    val saveError: String? = null,
) : ViewState

sealed interface PackCreatorIntent : ViewIntent {
    data class EditForm(val form: AddPackFormState) : PackCreatorIntent

    /** Creates the draft for a new pack from the form's name and size. */
    data object Create : PackCreatorIntent
    data class SetTag(val frameIndex: Int, val tag: PackFrameTag) : PackCreatorIntent
    data class ReplaceTags(val tags: Map<Int, PackFrameTag>) : PackCreatorIntent

    /** Saves the draft's first [totalFrames] frames, the editor's live timeline length. */
    data class Save(val totalFrames: Int) : PackCreatorIntent
    data object DismissSaveError : PackCreatorIntent
}

sealed interface PackCreatorEffect : ViewEffect {
    /** The pack was saved and its draft deleted, so the Pack Creator can close. */
    data object Saved : PackCreatorEffect
}

/** The saved pack the Pack Creator reopens for editing. */
data class PackToEdit(val itemId: String, val scope: LibraryScope, val sourceProjectId: String?)

/**
 * Runs the Pack Creator: a new pack starts from a size and name, and a saved pack ([editing]) is
 * reopened as a draft. Unfinished drafts are not kept: when the Pack Creator closes without
 * saving, [onCleared] deletes the draft, unless its last save failed. Because the ViewModel
 * outlives configuration changes, rotating the screen keeps the draft.
 */
class PackCreatorViewModel(
    private val drafts: PackDrafts,
    editing: PackToEdit? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<PackCreatorState, PackCreatorIntent, PackCreatorEffect>(PackCreatorState()) {
    companion object {
        /** The saved pack to edit; absent for a new pack. */
        val EditKey = object : CreationExtras.Key<PackToEdit> {}
    }

    // The pack being edited, whose library item a save replaces.
    private var editingPack: LibraryItemManifest? = null

    init {
        editing?.let(::open)
    }

    override fun handleIntent(intent: PackCreatorIntent) {
        when (intent) {
            is PackCreatorIntent.EditForm -> updateState { it.copy(form = intent.form) }
            PackCreatorIntent.Create -> create()
            is PackCreatorIntent.SetTag -> updateState { it.copy(tags = it.tags + (intent.frameIndex to intent.tag)) }
            is PackCreatorIntent.ReplaceTags -> updateState { it.copy(tags = intent.tags) }
            is PackCreatorIntent.Save -> save(intent.totalFrames)
            PackCreatorIntent.DismissSaveError -> updateState { it.copy(saveError = null) }
        }
    }

    override fun onCleared() {
        val current = state.value
        current.draftId?.let { if (current.saveError == null) drafts.discard(it) }
        super.onCleared()
    }

    private fun create() {
        val form = state.value.form
        updateState { it.copy(packName = form.name, canvasDimension = form.dimension) }
        viewModelScope.launch {
            when (val created = withContext(ioDispatcher) { drafts.create(form.dimension) }) {
                is StoreResult.Value -> updateState { it.copy(draftId = created.value) }
                is StoreResult.Failure -> updateState { it.copy(draftError = created.message) }
            }
        }
    }

    private fun open(pack: PackToEdit) {
        viewModelScope.launch {
            val opened = withContext(ioDispatcher) { drafts.openForEdit(pack.itemId, pack.scope, pack.sourceProjectId) }
            when (opened) {
                is StoreResult.Value -> {
                    val draft = opened.value
                    editingPack = draft.pack
                    updateState {
                        it.copy(
                            packName = draft.pack.displayName,
                            tags = draft.tags,
                            canvasDimension = draft.canvasDimension,
                            draftId = draft.draftId,
                        )
                    }
                }

                is StoreResult.Failure -> updateState { it.copy(draftError = opened.message) }
            }
        }
    }

    private fun save(totalFrames: Int) {
        val current = state.value
        val draftId = current.draftId ?: return
        val dimension = current.canvasDimension ?: return
        updateState { it.copy(saving = true) }
        viewModelScope.launch {
            val saved = withContext(ioDispatcher) {
                drafts.save(draftId, dimension, current.packName, totalFrames, current.tags, editingPack)
            }
            when (saved) {
                is StoreResult.Value -> {
                    updateState { it.copy(saving = false, draftId = null) }
                    sendEffect(PackCreatorEffect.Saved)
                }

                is StoreResult.Failure -> updateState { it.copy(saving = false, saveError = saved.message) }
            }
        }
    }
}
