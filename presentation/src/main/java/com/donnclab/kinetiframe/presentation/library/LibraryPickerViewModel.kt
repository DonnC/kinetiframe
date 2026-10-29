package com.donnclab.kinetiframe.presentation.library

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.ui.BaseViewModel
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** An audio item being previewed before it is inserted. [path] is null until its file is found. */
data class AudioItemPreview(val item: LibraryItemManifest, val path: String? = null, val error: String? = null)

data class LibraryPickerState(
    val scope: LibraryScope = LibraryScope.PERSONAL,
    val kindFilter: LibraryItemKind? = null,
    val items: List<LibraryItemManifest> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
    val preview: AudioItemPreview? = null,
) : ViewState

sealed interface LibraryPickerIntent : ViewIntent {
    /** The picker opened: start from the personal library with no filter. */
    data object Open : LibraryPickerIntent
    data class SelectScope(val scope: LibraryScope) : LibraryPickerIntent
    data class Filter(val kind: LibraryItemKind?) : LibraryPickerIntent
    data class Preview(val item: LibraryItemManifest) : LibraryPickerIntent
    data object ClosePreview : LibraryPickerIntent
}

/** The picker reports picks through its callbacks, so it sends no effects. */
sealed interface LibraryPickerEffect : ViewEffect

/**
 * Lists the library for the "add from Library" picker, in the personal or the current project's
 * scope, and finds the files its cards and audio preview show.
 */
class LibraryPickerViewModel(
    private val library: LibraryRepository,
    private val projectId: String? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<LibraryPickerState, LibraryPickerIntent, LibraryPickerEffect>(LibraryPickerState()) {
    companion object {
        /** The project whose own library the picker can switch to. */
        val ProjectIdKey = object : CreationExtras.Key<String> {}
    }

    private var listJob: Job? = null

    override fun handleIntent(intent: LibraryPickerIntent) {
        when (intent) {
            LibraryPickerIntent.Open -> {
                updateState { LibraryPickerState() }
                list(LibraryScope.PERSONAL)
            }

            is LibraryPickerIntent.SelectScope -> {
                updateState { it.copy(scope = intent.scope, kindFilter = null) }
                list(intent.scope)
            }

            is LibraryPickerIntent.Filter -> updateState { it.copy(kindFilter = intent.kind) }

            is LibraryPickerIntent.Preview -> preview(intent.item)

            LibraryPickerIntent.ClosePreview -> updateState { it.copy(preview = null) }
        }
    }

    /** The image a card shows for [item], or null. Call from a background thread. */
    fun previewImage(item: LibraryItemManifest): File? = library.previewImage(item)

    private fun list(scope: LibraryScope) {
        listJob?.cancel()
        updateState { it.copy(loading = true, error = null) }
        listJob = viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { library.list(scope, projectId) }) {
                is StoreResult.Value -> updateState { it.copy(items = result.value, loading = false) }
                is StoreResult.Failure -> updateState { it.copy(error = result.message, loading = false) }
            }
        }
    }

    private fun preview(item: LibraryItemManifest) {
        updateState { it.copy(preview = AudioItemPreview(item)) }
        viewModelScope.launch {
            val found = when (val result = withContext(ioDispatcher) { library.assetFile(item) }) {
                is StoreResult.Value -> AudioItemPreview(item, path = result.value.absolutePath)
                is StoreResult.Failure -> AudioItemPreview(item, error = result.message)
            }
            updateState { state -> if (state.preview?.item == item) state.copy(preview = found) else state }
        }
    }
}
