package com.donnclab.kinetiframe.presentation.studio.actorstudio

import android.graphics.Bitmap
import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.library.domain.LibraryArtwork
import com.donnclab.kinetiframe.library.model.PackArtworkRegistration
import com.donnclab.kinetiframe.library.model.PackItemEntry
import com.donnclab.kinetiframe.ui.BaseViewModel
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** [pack] is the pack being browsed, with its [items]; null while the library is shown. */
data class ActorArtworkPickerState(
    val pack: LibraryItemManifest? = null,
    val items: List<PackItemEntry> = emptyList(),
    val busy: Boolean = false,
    val error: String? = null,
) : ViewState

sealed interface ActorArtworkPickerIntent : ViewIntent {
    /** The picker opened: start from the library. */
    data object Open : ActorArtworkPickerIntent
    data class OpenPack(val pack: LibraryItemManifest) : ActorArtworkPickerIntent
    data object BackToLibrary : ActorArtworkPickerIntent
    data class PickItem(val item: LibraryItemManifest) : ActorArtworkPickerIntent
    data class PickPackItem(val entry: PackItemEntry) : ActorArtworkPickerIntent
    data object DismissError : ActorArtworkPickerIntent
}

sealed interface ActorArtworkPickerEffect : ViewEffect {
    /** A drawing was decoded. The receiver owns [bitmap]. */
    data class Selected(val name: String, val bitmap: Bitmap, val registration: PackArtworkRegistration?) :
        ActorArtworkPickerEffect
}

/** Picks a library frame or pack drawing to attach to an actor part, and decodes it. */
class ActorArtworkPickerViewModel(
    private val artwork: LibraryArtwork,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<ActorArtworkPickerState, ActorArtworkPickerIntent, ActorArtworkPickerEffect>(
    ActorArtworkPickerState(),
) {
    override fun handleIntent(intent: ActorArtworkPickerIntent) {
        when (intent) {
            ActorArtworkPickerIntent.Open -> updateState { ActorArtworkPickerState() }

            is ActorArtworkPickerIntent.OpenPack -> openPack(intent.pack)

            ActorArtworkPickerIntent.BackToLibrary -> updateState { it.copy(pack = null, items = emptyList()) }

            is ActorArtworkPickerIntent.PickItem -> pick(intent.item.displayName, null) { artwork.read(intent.item) }

            is ActorArtworkPickerIntent.PickPackItem -> state.value.pack?.let { pack ->
                pick(intent.entry.name, intent.entry.registration) { artwork.readPackItem(pack, intent.entry) }
            }

            ActorArtworkPickerIntent.DismissError -> updateState { it.copy(error = null) }
        }
    }

    /** The file of [entry] in the open pack, for its thumbnail. */
    fun packItemFile(entry: PackItemEntry): File? = state.value.pack?.let { artwork.packItemFile(it, entry) }

    private fun openPack(pack: LibraryItemManifest) {
        updateState { it.copy(pack = pack, items = emptyList()) }
        viewModelScope.launch {
            when (val loaded = withContext(ioDispatcher) { artwork.packItems(pack) }) {
                is StoreResult.Value -> updateState { if (it.pack == pack) it.copy(items = loaded.value) else it }
                is StoreResult.Failure -> updateState { it.copy(error = loaded.message) }
            }
        }
    }

    private fun pick(name: String, registration: PackArtworkRegistration?, decode: () -> Bitmap) {
        if (state.value.busy) return
        updateState { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val bitmap = withContext(ioDispatcher) { decode() }
                sendEffect(ActorArtworkPickerEffect.Selected(name, bitmap, registration))
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Exception) {
                updateState { it.copy(error = failure.message ?: "Could not read artwork") }
            } finally {
                updateState { it.copy(busy = false) }
            }
        }
    }
}
