// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import com.donnclab.kinetiframe.actors.domain.PerformanceChoices
import com.donnclab.kinetiframe.actors.render.ActorRendering
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.ProjectSaveDocument
import com.donnclab.kinetiframe.library.domain.LibraryPick
import com.donnclab.kinetiframe.library.domain.LibraryTransfers
import com.donnclab.kinetiframe.library.model.PackTrayItem
import com.donnclab.kinetiframe.project.domain.ProjectSaver
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The editor's way to storage. `StudioViewModel` owns the document being edited; this ViewModel
 * writes it out, moves items between it and the library, and draws its actors, so the editor's
 * screens never hold a store themselves. Its functions suspend because the editor runs them
 * inside its own save queue (`StudioViewModel.launchSave`).
 */
class EditorStorageViewModel(
    private val saver: ProjectSaver,
    private val transfers: LibraryTransfers,
    /** Draws the project's actors; each screen asks it for its own renderer. */
    val actorRendering: ActorRendering,
    /** The actors and performances the timeline can place. */
    val performanceChoices: PerformanceChoices,
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

    /** Fetches a library item to place in the project; see [LibraryTransfers.pick]. */
    suspend fun pickFromLibrary(
        manifest: LibraryItemManifest,
        projectId: String,
        time: Long,
        audioDirectory: File,
    ): Result<LibraryPick> = withContext(ioDispatcher) { transfers.pick(manifest, projectId, time, audioDirectory) }

    suspend fun loadPackTray(manifest: LibraryItemManifest, projectId: String): Result<List<PackTrayItem>> =
        withContext(ioDispatcher) { transfers.loadPackTray(manifest, projectId) }

    suspend fun exportBackground(project: ProjectDocument, scope: LibraryScope): StoreResult<LibraryItemManifest> =
        withContext(ioDispatcher) { transfers.exportBackground(project, scope) }

    suspend fun exportFrame(
        frame: Bitmap,
        displayName: String,
        scope: LibraryScope,
        projectId: String,
    ): StoreResult<LibraryItemManifest> = withContext(ioDispatcher) {
        transfers.exportFrame(frame, displayName, scope, projectId)
    }

    suspend fun exportAudioClip(
        clip: AudioClipPlacement,
        project: ProjectDocument,
        scope: LibraryScope,
    ): StoreResult<LibraryItemManifest> = withContext(ioDispatcher) { transfers.exportAudioClip(clip, project, scope) }

    suspend fun exportPerformance(projectId: String, placement: ActorPlacementReference): StoreResult<*> =
        withContext(ioDispatcher) { transfers.exportPerformance(projectId, placement) }
}
