package com.donnclab.kinetiframe.presentation.studio.color

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.library.data.PaletteRepository
import com.donnclab.kinetiframe.library.model.PaletteDocument
import com.donnclab.kinetiframe.library.model.PaletteEdits
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.UiText
import com.donnclab.kinetiframe.presentation.common.toUiText
import com.donnclab.kinetiframe.ui.BaseViewModel
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class PaletteState(
    val loading: Boolean = true,
    val palettes: List<PaletteDocument> = emptyList(),
    val selectedId: String? = null,
    val error: UiText? = null,
    /** Colours used most recently, newest first. Kept while the app runs. */
    val recent: List<Long> = emptyList(),
) : ViewState {
    /** The palette the "+" in the colour picker saves into, once it has a colour. */
    val myColours: PaletteDocument? get() = palettes.firstOrNull { it.id == PaletteRepository.MY_COLOURS_ID }

    /** Whether the user made [palette], so they can add to it, remove from it and delete it. */
    fun canEdit(palette: PaletteDocument) = !PaletteRepository.isStarter(palette.id)

    /** Whether [palette] is My colours, which shows its name in the user's language. */
    fun isMyColours(palette: PaletteDocument) = palette.id == PaletteRepository.MY_COLOURS_ID
}

sealed interface PaletteIntent : ViewIntent {
    data object Load : PaletteIntent
    data class Select(val id: String) : PaletteIntent

    /** A colour was drawn with, so it joins the recent colours. */
    data class Used(val argb: Long) : PaletteIntent

    /** Saves [argb] into [paletteId], or into My colours when it's null. */
    data class Add(val argb: Long, val paletteId: String? = null) : PaletteIntent
    data class Remove(val paletteId: String, val swatchName: String) : PaletteIntent

    /** Makes a palette of the user's own named [name], holding [colours] in order. */
    data class Create(val name: String, val colours: List<Long>) : PaletteIntent
    data class Rename(val paletteId: String, val name: String) : PaletteIntent

    /** Copies a palette, such as one that comes with the app, into one the user can edit. */
    data class Duplicate(val paletteId: String, val name: String) : PaletteIntent
    data class Delete(val paletteId: String) : PaletteIntent
}

sealed interface PaletteEffect : ViewEffect

class PaletteViewModel(
    private val repository: PaletteRepository,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<PaletteState, PaletteIntent, PaletteEffect>(PaletteState()) {
    init {
        handleIntent(PaletteIntent.Load)
    }

    override fun handleIntent(intent: PaletteIntent) {
        when (intent) {
            is PaletteIntent.Select -> updateState { it.copy(selectedId = intent.id) }

            PaletteIntent.Load -> load()

            is PaletteIntent.Used -> updateState {
                it.copy(recent = (listOf(intent.argb) + it.recent.filterNot { c -> c == intent.argb }).take(RECENT))
            }

            is PaletteIntent.Add -> edit(R.string.palette_save_failed) {
                val id = intent.paletteId ?: PaletteRepository.MY_COLOURS_ID
                val palette = currentState.palettes.firstOrNull { it.id == id }
                repository.update(
                    palette?.let { PaletteEdits.add(it, intent.argb) }
                        ?: PaletteEdits.create(MY_COLOURS_NAME, intent.argb, id),
                )
                id
            }

            is PaletteIntent.Remove -> edit(R.string.palette_save_failed) {
                val palette = currentState.palettes.firstOrNull { it.id == intent.paletteId } ?: return@edit null
                val left = PaletteEdits.remove(palette, intent.swatchName)
                if (left == null) repository.delete(palette.id) else repository.update(left)
                left?.id
            }

            is PaletteIntent.Create -> edit(R.string.palette_save_failed) {
                repository.update(PaletteEdits.create(intent.name, intent.colours)).id
            }

            is PaletteIntent.Rename -> edit(R.string.palette_save_failed) {
                val palette = currentState.palettes.firstOrNull { it.id == intent.paletteId } ?: return@edit null
                repository.update(PaletteEdits.rename(palette, intent.name)).id
            }

            is PaletteIntent.Duplicate -> edit(R.string.palette_save_failed) {
                val palette = currentState.palettes.firstOrNull { it.id == intent.paletteId } ?: return@edit null
                repository.update(PaletteEdits.duplicate(palette, intent.name)).id
            }

            is PaletteIntent.Delete -> edit(R.string.palette_save_failed) {
                repository.delete(intent.paletteId)
                null
            }
        }
    }

    /** Runs a palette change on IO, then reloads, selecting the palette [change] returns. */
    private fun edit(failure: Int, change: () -> String?) {
        viewModelScope.launch {
            try {
                val selected = withContext(ioDispatcher) { change() }
                selected?.let { id -> updateState { it.copy(selectedId = id) } }
                load()
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateState { it.copy(error = error.toUiText(failure)) }
            }
        }
    }

    private fun load() {
        viewModelScope.launch {
            updateState { it.copy(loading = it.palettes.isEmpty(), error = null) }
            try {
                val palettes = withContext(ioDispatcher) {
                    repository.installStarters()
                    // The user's own palettes come first, My colours at the top.
                    repository.list().sortedWith(
                        compareBy({ PaletteRepository.isStarter(it.id) }, { it.id != PaletteRepository.MY_COLOURS_ID }),
                    )
                }
                updateState {
                    it.copy(
                        loading = false,
                        palettes = palettes,
                        selectedId = it.selectedId?.takeIf { id -> palettes.any { p -> p.id == id } }
                            ?: palettes.firstOrNull()?.id,
                    )
                }
            } catch (cancelled: kotlinx.coroutines.CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                updateState { it.copy(loading = false, error = error.toUiText(R.string.palette_load_failed)) }
            }
        }
    }

    private companion object {
        const val RECENT = 8
        const val MY_COLOURS_NAME = "My colours"
    }
}
