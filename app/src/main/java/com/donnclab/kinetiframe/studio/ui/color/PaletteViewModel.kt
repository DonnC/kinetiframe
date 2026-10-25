package com.donnclab.kinetiframe.studio.ui.color

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.library.data.PaletteRepository
import com.donnclab.kinetiframe.library.model.PaletteDocument
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
    val error: String? = null,
) : ViewState
sealed interface PaletteIntent : ViewIntent {
    data object Load : PaletteIntent
    data class Select(val id: String) : PaletteIntent
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

            PaletteIntent.Load -> viewModelScope.launch {
                updateState { it.copy(loading = true, error = null) }
                try {
                    val palettes = withContext(ioDispatcher) {
                        repository.installStarters()
                        repository.list()
                    }
                    updateState {
                        it.copy(
                            loading = false,
                            palettes = palettes,
                            selectedId =
                            it.selectedId?.takeIf { id -> palettes.any { p -> p.id == id } }
                                ?: palettes.firstOrNull()?.id,
                        )
                    }
                } catch (
                    cancelled: kotlinx.coroutines.CancellationException,
                ) {
                    throw cancelled
                } catch (error: Exception) {
                    updateState {
                        it.copy(
                            loading = false,
                            error =
                            error.message ?: "Could not load palettes",
                        )
                    }
                }
            }
        }
    }
}
