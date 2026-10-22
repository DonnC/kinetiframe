package com.donnclab.kinetiframe.ui.projects

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.project.ProjectManifest
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.project.ProjectSaveDocument
import com.donnclab.kinetiframe.data.project.db.ProjectDao
import com.donnclab.kinetiframe.data.project.db.ProjectEntity
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.models.Layer
import com.donnclab.kinetiframe.ui.BaseViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Gallery-only MVI model, mirroring ActorManagerViewModel exactly -- it never owns a canvas or
 *  timeline. Opening a project just hands its ID off via [ProjectManagerEffect.OpenProject];
 *  EditorScreen is the one that actually loads the project's document. */
class ProjectManagerViewModel(
    private val repository: ProjectRepository,
    // Nullable so this ViewModel still works (e.g. in tests, or if Room init ever fails) without
    // an index -- reload() below falls back to a plain filesystem scan whenever it's null or empty.
    private val projectDao: ProjectDao? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val id: () -> String = { "project-${UUID.randomUUID()}" },
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
        val projectId = id()
        val time = now()
        viewModelScope.launch {
            val result = withContext(ioDispatcher) {
                runCatching {
                    requireNameAvailable(name, null)
                    val mainLayer = Layer(name = "Main Layer", id = "main")
                    val document = ProjectSaveDocument(
                        project = ProjectDocument(id = projectId, stage = stage, fps = fps, layers = listOf(mainLayer)),
                        frameLayers = mapOf(0 to listOf(mainLayer)),
                        totalFrames = 1,
                    )
                    val manifest =
                        ProjectManifest(
                            projectId = projectId,
                            displayName = name.trim(),
                            stage = stage,
                            fps = fps,
                            createdAtEpochMs = time,
                            updatedAtEpochMs = time,
                            totalFrames = document.totalFrames,
                        )
                    when (val saved = repository.save(manifest, document)) {
                        is ActorStoreResult.Value -> {
                            indexInRoom(saved.value)
                            saved.value
                        }

                        is ActorStoreResult.Failure -> throw IllegalStateException(saved.message, saved.cause)
                    }
                }
            }
            result.fold(
                onSuccess = {
                    reload()
                    sendEffect(ProjectManagerEffect.OpenProject(projectId))
                },
                onFailure = { sendEffect(ProjectManagerEffect.ShowError(it.message ?: "Could not create project")) },
            )
        }
    }

    private fun rename(projectId: String, name: String) {
        viewModelScope.launch {
            when (
                val result = withContext(ioDispatcher) {
                    requireNameAvailable(name, projectId)
                    repository.rename(projectId, name, now()).also {
                        if (it is ActorStoreResult.Value) indexInRoom(it.value)
                    }
                }
            ) {
                is ActorStoreResult.Value -> reload()
                is ActorStoreResult.Failure -> sendEffect(ProjectManagerEffect.ShowError(result.message))
            }
        }
    }

    private fun delete(projectId: String) {
        viewModelScope.launch {
            when (
                val result = withContext(ioDispatcher) {
                    repository.delete(projectId).also {
                        if (it is ActorStoreResult.Value) runCatching { projectDao?.deleteById(projectId) }
                    }
                }
            ) {
                is ActorStoreResult.Value -> reload()
                is ActorStoreResult.Failure -> sendEffect(ProjectManagerEffect.ShowError(result.message))
            }
        }
    }

    private fun requireNameAvailable(name: String, excludingProjectId: String?) {
        require(nameIsAvailable(name, excludingProjectId)) { "A project named '${name.trim()}' already exists" }
    }

    private fun nameIsAvailable(name: String, excludingProjectId: String?): Boolean =
        when (val result = repository.isDisplayNameAvailable(name, excludingProjectId)) {
            is ActorStoreResult.Value -> result.value
            is ActorStoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
        }

    /**
     * Reconcile Room against committed files on every gallery reload. A missed indexing write
     * must not hide a project just because another project already populated the database.
     */
    private fun reload() {
        updateState { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (val result = withContext(ioDispatcher) { repository.list() }) {
                is ActorStoreResult.Value -> {
                    updateState { it.copy(loading = false, projects = result.value) }
                    result.value.forEach { manifest -> indexInRoom(manifest) }
                    withContext(ioDispatcher) {
                        runCatching {
                            projectDao?.getAll()?.filter { row ->
                                result.value.none {
                                    it.projectId ==
                                        row.projectId
                                }
                            }
                                ?.forEach { projectDao.deleteById(it.projectId) }
                        }
                    }
                }

                is ActorStoreResult.Failure -> updateState { it.copy(loading = false, error = result.message) }
            }
        }
    }

    private fun ProjectEntity.toManifest() = ProjectManifest(
        projectId = projectId,
        displayName = title,
        stage = com.donnclab.kinetiframe.engine.model.StageDimensions(resolutionWidth, resolutionHeight),
        fps = fps,
        createdAtEpochMs = createdAtEpochMs,
        updatedAtEpochMs = lastModified,
        totalFrames = durationFrames,
        thumbnailPath = thumbnailPath,
    )

    private fun List<ProjectEntity>.toManifests() = map { it.toManifest() }

    /** Suspend, and does its own write directly rather than launching a detached coroutine --
     *  every caller is already running inside a viewModelScope coroutine (create/rename/reload's
     *  backfill). A fire-and-forget launch would race with the reload() each caller issues right
     *  after, and a just-written project could be missing from the next gallery read. */
    private suspend fun indexInRoom(manifest: ProjectManifest) {
        val dao = projectDao ?: return
        withContext(ioDispatcher) {
            runCatching {
                // ProjectRepository.list() has already checked thumbnailPath against disk, so it is
                // stored as-is without re-checking the file.
                dao.upsert(
                    ProjectEntity(
                        projectId = manifest.projectId, title = manifest.displayName,
                        createdAtEpochMs = manifest.createdAtEpochMs, lastModified = manifest.updatedAtEpochMs,
                        durationFrames = manifest.totalFrames, fps = manifest.fps,
                        resolutionWidth = manifest.stage.width, resolutionHeight = manifest.stage.height,
                        thumbnailPath = manifest.thumbnailPath,
                    ),
                )
            }
        }
    }
}
