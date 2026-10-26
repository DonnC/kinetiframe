// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.project.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.actors.render.ActorFrameRenderer
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.ProjectSaveDocument
import com.donnclab.kinetiframe.data.db.ProjectDao
import com.donnclab.kinetiframe.data.db.ProjectEntity
import com.donnclab.kinetiframe.data.project.ProjectFrameDraft
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.studio.camera.CameraPosterFrame
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import com.donnclab.kinetiframe.studio.render.ProjectFrameCompositor
import java.io.File
import java.io.FileOutputStream

/**
 * Writes an editor's prepared project document to disk: the manifest, the frame pixels, a gallery
 * thumbnail and the gallery's Room index. Call off the main thread.
 */
class ProjectSaver(
    private val files: DocumentFileStore,
    private val projects: ProjectRepository,
    private val actors: ActorRepository,
    private val performances: PerformanceRepository,
    private val projectIndex: ProjectDao? = null,
) {
    /**
     * Saves [document] as [projectId] at [time]. When [draftFrames] is set, the frames drawn in
     * that working directory are published to the project first; Pack Creator passes null because
     * it draws straight into the project's frames. [frames] is the editor's frame cache, which
     * the thumbnail is drawn from.
     */
    suspend fun save(
        projectId: String,
        document: ProjectSaveDocument,
        draftFrames: File?,
        time: Long,
        frames: FrameCacheManager,
    ): StoreResult<ProjectManifest> {
        // Re-read the manifest first so a save never clobbers the project's own display name,
        // which the document itself does not carry.
        val existingManifest = when (val loaded = projects.load(projectId)) {
            is StoreResult.Value -> loaded.value.first
            is StoreResult.Failure -> null
        }
        val manifest = (
            existingManifest ?: ProjectManifest(
                projectId = projectId,
                displayName = projectId,
                stage = document.project.stage,
                fps = document.project.fps,
                createdAtEpochMs = time,
                updatedAtEpochMs = time,
            )
            ).copy(
            stage = document.project.stage,
            fps = document.project.fps,
            updatedAtEpochMs = time,
            totalFrames = document.totalFrames,
        )
        try {
            if (draftFrames != null) {
                ProjectFrameDraft.publish(draftFrames, File(files.projectDirectory(projectId), "frames"))
            }
        } catch (failure: Exception) {
            return StoreResult.Failure(failure.message ?: "Could not save frame pixels", failure)
        }
        val saveResult = projects.save(manifest, document)
        if (saveResult is StoreResult.Value) {
            // The thumbnail is regenerated on every save so the gallery always shows the current
            // scene. It costs one composited frame.
            val thumbnailPath = runCatching { writeThumbnail(projectId, document, frames) }.getOrNull()
            // Keep the gallery index in step with an in-editor save as well as with gallery-side
            // create, rename and delete, so it never shows a stale duration.
            runCatching { index(manifest, thumbnailPath) }
        }
        return saveResult
    }

    private suspend fun writeThumbnail(
        projectId: String,
        document: ProjectSaveDocument,
        frames: FrameCacheManager,
    ): String {
        val project = document.project
        val actorRenderer =
            if (project.actorPlacements.isEmpty()) null else ActorFrameRenderer(actors, performances, files)
        val backgroundBitmap = project.backgroundAssetId
            ?.let { project.assets[it]?.sourcePath }
            ?.let { path -> BitmapFactory.decodeFile(path) }
        val compositor = ProjectFrameCompositor(
            project,
            document.frameLayers,
            frames,
            actorRenderer,
            backgroundBitmap,
            includeFades = false,
        )
        try {
            val posterFrame = CameraPosterFrame.select(project.cameraShots, document.totalFrames, project.fps)
            val frame = compositor.composite(posterFrame)
            val thumbnailFile = projects.thumbnailFile(projectId)
            thumbnailFile.parentFile?.mkdirs()
            FileOutputStream(thumbnailFile).use { out -> frame.compress(Bitmap.CompressFormat.PNG, 90, out) }
            // compress() has read every pixel by now, so the compositor's bitmaps can be
            // released. This compositor is never shared with the live canvas.
            return thumbnailFile.absolutePath
        } finally {
            compositor.release()
        }
    }

    private suspend fun index(manifest: ProjectManifest, thumbnailPath: String?) {
        projectIndex?.upsert(
            ProjectEntity(
                projectId = manifest.projectId,
                title = manifest.displayName,
                createdAtEpochMs = manifest.createdAtEpochMs,
                lastModified = manifest.updatedAtEpochMs,
                durationFrames = manifest.totalFrames,
                fps = manifest.fps,
                resolutionWidth = manifest.stage.width,
                resolutionHeight = manifest.stage.height,
                thumbnailPath = thumbnailPath,
            ),
        )
    }
}
