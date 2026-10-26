package com.donnclab.kinetiframe.project.ui

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.project.domain.ProjectCatalog
import com.donnclab.kinetiframe.ui.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Gallery-only MVI model, mirroring ActorManagerViewModel exactly -- it never owns a canvas or
 *  timeline. Opening a project just hands its ID off via [ProjectManagerEffect.OpenProject];
 *  EditorScreen is the one that actually loads the project's document. */
class ProjectManagerViewModel(
    private val catalog: ProjectCatalog,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : BaseViewModel<ProjectManagerState, ProjectManagerIntent, ProjectManagerEffect>(ProjectManagerState()) {
    init {
        reload()
    }

    override fun handleIntent(intent: ProjectManagerIntent) = when (intent) {
        ProjectManagerIntent.Reload -> reload()
        is ProjectManagerIntent.Create -> create(intent.name, intent.stage, intent.fps)
        is ProjectManagerIntent.Rename -> rename(intent.projectId, intent.name)
        is ProjectManagerIntent.Delete -> delete(intent.projectId)
        is ProjectManagerIntent.Open -> sendEffect(ProjectManagerEffect.OpenProject(intent.projectId))
    }

    private fun create(name: String, stage: StageDimensions, fps: Int) {
        viewModelScope.launch {
            when (val created = withContext(ioDispatcher) { catalog.create(name, stage, fps) }) {
                is StoreResult.Value -> {
                    reload()
                    sendEffect(ProjectManagerEffect.OpenProject(created.value.projectId))
                }

                is StoreResult.Failure -> sendEffect(ProjectManagerEffect.ShowError(created.message))
            }
        }
    }

    private fun rename(projectId: String, name: String) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { catalog.rename(projectId, name) }) {
                is StoreResult.Value -> reload()
                is StoreResult.Failure -> sendEffect(ProjectManagerEffect.ShowError(result.message))
            }
        }
    }

    private fun delete(projectId: String) {
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { catalog.delete(projectId) }) {
                is StoreResult.Value -> reload()
                is StoreResult.Failure -> sendEffect(ProjectManagerEffect.ShowError(result.message))
            }
        }
    }

    // The gallery shows the files' list first, then brings the search index in line with it.
    private fun reload() {
        updateState { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { catalog.list() }) {
                is StoreResult.Value -> {
                    updateState { it.copy(loading = false, projects = result.value) }
                    withContext(ioDispatcher) { catalog.reconcileIndex(result.value) }
                }

                is StoreResult.Failure -> updateState { it.copy(loading = false, error = result.message) }
            }
        }
    }
}
