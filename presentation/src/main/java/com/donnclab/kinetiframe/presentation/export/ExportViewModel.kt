package com.donnclab.kinetiframe.presentation.export

import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.export.domain.VideoExporter
import com.donnclab.kinetiframe.export.model.ExportPreset
import com.donnclab.kinetiframe.export.model.ExportPresets
import com.donnclab.kinetiframe.ui.BaseViewModel
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed interface ExportPhase {
    data object ChoosingPreset : ExportPhase
    data object Rendering : ExportPhase
    data class Done(val file: File) : ExportPhase
    data class Failed(val message: String) : ExportPhase
}

/** [displayName] names the saved and shared video; it is the project ID until the name loads. */
data class ExportState(
    val displayName: String,
    val preset: ExportPreset = ExportPresets.Presets.first(),
    val phase: ExportPhase = ExportPhase.ChoosingPreset,
    val progress: Float = 0f,
) : ViewState

sealed interface ExportIntent : ViewIntent {
    data class ChoosePreset(val preset: ExportPreset) : ExportIntent
    data object Start : ExportIntent
    data object SaveToGallery : ExportIntent
    data object Share : ExportIntent
}

sealed interface ExportEffect : ViewEffect {
    data class ShowMessage(val message: String) : ExportEffect

    /** Offer the exported [file] to other apps through the share sheet, titled [title]. */
    data class Share(val file: File, val title: String) : ExportEffect
}

/**
 * Renders one project to a video at the chosen preset, then saves it to the gallery or shares it.
 * Rendering runs in the ViewModel's scope, so it carries on if the Activity is recreated.
 */
class ExportViewModel(
    private val exporter: VideoExporter,
    private val projectId: String,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<ExportState, ExportIntent, ExportEffect>(ExportState(displayName = projectId)) {
    companion object {
        /** The project to export. */
        val ProjectIdKey = object : CreationExtras.Key<String> {}
    }

    init {
        viewModelScope.launch {
            val name = withContext(ioDispatcher) { exporter.projectName(projectId) }
            if (name != null) updateState { it.copy(displayName = name) }
        }
    }

    override fun handleIntent(intent: ExportIntent) {
        when (intent) {
            is ExportIntent.ChoosePreset -> updateState { it.copy(preset = intent.preset) }

            ExportIntent.Start -> start()

            ExportIntent.SaveToGallery -> saveToGallery()

            ExportIntent.Share -> (state.value.phase as? ExportPhase.Done)?.let {
                sendEffect(ExportEffect.Share(it.file, "Share ${state.value.displayName}"))
            }
        }
    }

    private fun start() {
        if (state.value.phase == ExportPhase.Rendering) return
        updateState { it.copy(phase = ExportPhase.Rendering, progress = 0f) }
        viewModelScope.launch {
            val result = exporter.export(projectId, state.value.preset.dimensions) { fraction ->
                updateState { it.copy(progress = fraction) }
            }
            val phase = when (result) {
                is StoreResult.Value -> ExportPhase.Done(result.value)
                is StoreResult.Failure -> ExportPhase.Failed(result.message)
            }
            updateState { it.copy(phase = phase) }
        }
    }

    private fun saveToGallery() {
        val done = state.value.phase as? ExportPhase.Done ?: return
        viewModelScope.launch {
            val result = withContext(ioDispatcher) { exporter.saveToGallery(done.file, state.value.displayName) }
            val message = when (result) {
                is StoreResult.Value -> "Saved to your gallery"
                is StoreResult.Failure -> result.message
            }
            sendEffect(ExportEffect.ShowMessage(message))
        }
    }
}
