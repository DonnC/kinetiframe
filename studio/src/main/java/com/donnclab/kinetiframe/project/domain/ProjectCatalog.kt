package com.donnclab.kinetiframe.project.domain

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.ProjectSaveDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.db.ProjectDao
import com.donnclab.kinetiframe.data.db.ProjectEntity
import com.donnclab.kinetiframe.data.project.ProjectRepository
import java.util.UUID

/**
 * The project gallery's storage: listing, creating, renaming and deleting projects. Project files
 * are the source of truth, and [projectDao] is a search index kept in step with them. The index
 * is optional, so the gallery still works when Room is unavailable, and every index write is
 * best effort. Project names are unique, ignoring case and surrounding spaces.
 *
 * Every function does file work; call them off the main thread.
 */
class ProjectCatalog(
    private val repository: ProjectRepository,
    private val projectDao: ProjectDao? = null,
    private val now: () -> Long = System::currentTimeMillis,
    private val newId: () -> String = { "project-${UUID.randomUUID()}" },
) {
    /** Every project in the gallery, read from its files. */
    fun list(): StoreResult<List<ProjectManifest>> = repository.list()

    /**
     * Brings the index in line with [projects], the result of [list]: each is written, and rows
     * for projects that no longer exist are removed. A missed earlier write must not hide a
     * project just because the index already holds others.
     */
    suspend fun reconcileIndex(projects: List<ProjectManifest>) {
        projects.forEach { index(it) }
        val dao = projectDao ?: return
        runCatching {
            dao.getAll().filter { row -> projects.none { it.projectId == row.projectId } }
                .forEach { dao.deleteById(it.projectId) }
        }
    }

    /** Creates an empty one-frame project with one layer. */
    suspend fun create(name: String, stage: StageDimensions, fps: Int): StoreResult<ProjectManifest> = runCatching {
        requireNameAvailable(name, null)
        val projectId = newId()
        val time = now()
        val mainLayer = Layer(name = "Main Layer", id = "main")
        val document = ProjectSaveDocument(
            project = ProjectDocument(id = projectId, stage = stage, fps = fps, layers = listOf(mainLayer)),
            frameLayers = mapOf(0 to listOf(mainLayer)),
            totalFrames = 1,
        )
        val manifest = ProjectManifest(
            projectId = projectId,
            displayName = name.trim(),
            stage = stage,
            fps = fps,
            createdAtEpochMs = time,
            updatedAtEpochMs = time,
            totalFrames = document.totalFrames,
        )
        value(repository.save(manifest, document)).also { index(it) }
    }.fold({ StoreResult.Value(it) }, { StoreResult.Failure(it.message ?: "Could not create project", it) })

    suspend fun rename(projectId: String, name: String): StoreResult<ProjectManifest> = runCatching {
        requireNameAvailable(name, projectId)
        value(repository.rename(projectId, name, now())).also { index(it) }
    }.fold({ StoreResult.Value(it) }, { StoreResult.Failure(it.message ?: "Could not rename project", it) })

    suspend fun delete(projectId: String): StoreResult<Unit> = repository.delete(projectId).also {
        if (it is StoreResult.Value) runCatching { projectDao?.deleteById(projectId) }
    }

    private fun requireNameAvailable(name: String, excludingProjectId: String?) {
        require(value(repository.isDisplayNameAvailable(name, excludingProjectId))) {
            "A project named '${name.trim()}' already exists"
        }
    }

    // The manifest's thumbnail path was already checked against disk by the repository.
    private suspend fun index(manifest: ProjectManifest) {
        val dao = projectDao ?: return
        runCatching {
            dao.upsert(
                ProjectEntity(
                    projectId = manifest.projectId,
                    title = manifest.displayName,
                    createdAtEpochMs = manifest.createdAtEpochMs,
                    lastModified = manifest.updatedAtEpochMs,
                    durationFrames = manifest.totalFrames,
                    fps = manifest.fps,
                    resolutionWidth = manifest.stage.width,
                    resolutionHeight = manifest.stage.height,
                    thumbnailPath = manifest.thumbnailPath,
                ),
            )
        }
    }

    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
    }
}
