package com.donnclab.kinetiframe.project.domain

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ProjectSaveDocument
import com.donnclab.kinetiframe.data.project.ProjectFrameDraft
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import java.io.File

/** Opens a saved project for the editor. Call off the main thread. */
class ProjectOpener(private val files: DocumentFileStore, private val projects: ProjectRepository) {
    /** The folder that holds [projectId]'s document, frames and assets. */
    fun projectDirectory(projectId: String): File = files.projectDirectory(projectId)

    /**
     * Loads [projectId]'s document. When [draftFrames] is set, the saved frames are first copied
     * into that working directory, so the editor draws on a draft and only a save publishes it.
     */
    fun open(projectId: String, draftFrames: File?): StoreResult<ProjectSaveDocument> {
        val loaded = projects.load(projectId)
        if (loaded !is StoreResult.Value) return loaded as StoreResult.Failure
        if (draftFrames != null) {
            try {
                ProjectFrameDraft.open(File(projectDirectory(projectId), "frames"), draftFrames)
            } catch (failure: Exception) {
                return StoreResult.Failure(failure.message ?: "Could not open frame draft", failure)
            }
        }
        return StoreResult.Value(loaded.value.second)
    }
}
