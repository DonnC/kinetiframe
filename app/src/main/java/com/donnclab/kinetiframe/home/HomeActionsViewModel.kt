package com.donnclab.kinetiframe.home

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.mvi.ViewEffect
import com.donnclab.kinetiframe.core.mvi.ViewIntent
import com.donnclab.kinetiframe.core.mvi.ViewState
import com.donnclab.kinetiframe.library.bundled.BundledContent
import com.donnclab.kinetiframe.project.domain.ProjectTransfers
import com.donnclab.kinetiframe.ui.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.InputStream

/** Whether Home should offer to clean up the trial library. */
data class HomeActionsState(val cleanupOffered: Boolean = false) : ViewState

sealed interface HomeActionsIntent : ViewIntent {
    data object DismissCleanup : HomeActionsIntent
    data object CleanUp : HomeActionsIntent
    data class ShareProject(val project: ProjectManifest) : HomeActionsIntent

    /** [open] reads the `.kframe` file the animator picked. */
    data class ImportArchive(val open: () -> InputStream?) : HomeActionsIntent
    data class SaveToLibrary(val project: ProjectManifest) : HomeActionsIntent
    data class ImportFromLibrary(val item: LibraryItemManifest) : HomeActionsIntent
    data class SharePack(val pack: LibraryItemManifest) : HomeActionsIntent
}

sealed interface HomeActionsEffect : ViewEffect {
    /** Offer [file] to other apps through the share sheet, titled [title]. */
    data class Share(val file: File, val title: String) : HomeActionsEffect
    data class ShowMessage(val message: String) : HomeActionsEffect

    /** Projects changed on disk, so the projects list should reload. */
    data object ProjectsChanged : HomeActionsEffect

    /** Projects, actors and packs may all have changed. */
    data object EverythingChanged : HomeActionsEffect
}

/**
 * Home's actions that move content rather than list it: installing the bundled content once,
 * cleaning up the trial library, and sharing, importing or saving projects and packs. The lists
 * themselves belong to the projects, actors and packs ViewModels, which reload on this
 * ViewModel's effects.
 */
class HomeActionsViewModel(
    private val bundled: BundledContent,
    private val transfers: ProjectTransfers,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<HomeActionsState, HomeActionsIntent, HomeActionsEffect>(HomeActionsState()) {
    init {
        installBundledContent()
    }

    override fun handleIntent(intent: HomeActionsIntent) {
        when (intent) {
            HomeActionsIntent.DismissCleanup -> updateState { it.copy(cleanupOffered = false) }

            HomeActionsIntent.CleanUp -> cleanUp()

            is HomeActionsIntent.ShareProject -> share(intent.project.displayName) {
                transfers.projectForSharing(intent.project.projectId)
            }

            is HomeActionsIntent.ImportArchive -> importProject { transfers.importArchive(intent.open) }

            is HomeActionsIntent.SaveToLibrary -> saveToLibrary(intent.project)

            is HomeActionsIntent.ImportFromLibrary -> importProject { transfers.importFromLibrary(intent.item) }

            is HomeActionsIntent.SharePack -> share(intent.pack.displayName) { transfers.packForSharing(intent.pack) }
        }
    }

    private fun installBundledContent() {
        viewModelScope.launch {
            val installed = withContext(ioDispatcher) { bundled.install() }
            val cleanupPending = withContext(ioDispatcher) { bundled.cleanupPending() }
            updateState { it.copy(cleanupOffered = cleanupPending) }
            if (installed.listsChanged) sendEffect(HomeActionsEffect.EverythingChanged)
            if (installed.errors.isNotEmpty()) {
                sendEffect(
                    HomeActionsEffect.ShowMessage(
                        "Some demo resources could not be installed. Your work was not replaced.",
                    ),
                )
            }
        }
    }

    private fun cleanUp() {
        updateState { it.copy(cleanupOffered = false) }
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { bundled.cleanUp() }) {
                is StoreResult.Value -> {
                    sendEffect(HomeActionsEffect.EverythingChanged)
                    sendEffect(HomeActionsEffect.ShowMessage("Trial library archived. Your curated library is ready."))
                }

                is StoreResult.Failure -> sendEffect(HomeActionsEffect.ShowMessage(result.message))
            }
        }
    }

    private fun share(name: String, archive: () -> StoreResult<File>) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { archive() }) {
                is StoreResult.Value -> sendEffect(HomeActionsEffect.Share(result.value, "Share $name"))
                is StoreResult.Failure -> sendEffect(HomeActionsEffect.ShowMessage(result.message))
            }
        }
    }

    private fun importProject(import: () -> StoreResult<String>) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { import() }) {
                is StoreResult.Value -> {
                    sendEffect(HomeActionsEffect.ProjectsChanged)
                    sendEffect(HomeActionsEffect.ShowMessage("Project imported"))
                }

                is StoreResult.Failure -> sendEffect(HomeActionsEffect.ShowMessage(result.message))
            }
        }
    }

    private fun saveToLibrary(project: ProjectManifest) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { transfers.saveToLibrary(project) }) {
                is StoreResult.Value -> sendEffect(
                    HomeActionsEffect.ShowMessage("Saved ${project.displayName} to your library"),
                )

                is StoreResult.Failure -> sendEffect(HomeActionsEffect.ShowMessage(result.message))
            }
        }
    }
}
