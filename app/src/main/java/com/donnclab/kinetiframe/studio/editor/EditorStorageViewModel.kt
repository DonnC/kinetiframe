// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import androidx.lifecycle.ViewModel
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.ProjectSaveDocument
import com.donnclab.kinetiframe.project.domain.ProjectSaver
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The editor's way to storage. `StudioViewModel` owns the document being edited; this ViewModel
 * writes it out, so the editor's screens never hold a store themselves. Its functions suspend
 * because the editor runs them inside its own save queue (`StudioViewModel.launchSave`).
 */
class EditorStorageViewModel(
    private val saver: ProjectSaver,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ViewModel() {
    /** Saves [document] as [projectId]; see [ProjectSaver.save]. */
    suspend fun saveProject(
        projectId: String,
        document: ProjectSaveDocument,
        draftFrames: File?,
        time: Long,
        frames: FrameCacheManager,
    ): StoreResult<ProjectManifest> = withContext(ioDispatcher) {
        saver.save(projectId, document, draftFrames, time, frames)
    }
}
