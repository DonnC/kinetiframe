// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.donnclab.kinetiframe.data.db.AppDatabase
import com.donnclab.kinetiframe.data.db.ProjectEntity
import com.donnclab.kinetiframe.data.project.ProjectFrameDraft
import com.donnclab.kinetiframe.data.project.ProjectManifest
import com.donnclab.kinetiframe.data.project.ProjectSaveDocument
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.studio.camera.CameraPosterFrame
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import com.donnclab.kinetiframe.studio.render.ProjectFrameCompositor
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Writes a prepared project document to disk: the manifest, the frame pixels, a gallery thumbnail
 * and the Room gallery index.
 */
internal class ProjectSaveWriter(
    private val context: Context,
    private val repositories: EditorRepositories,
    private val cacheManager: FrameCacheManager,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    /**
     * Saves [document] as [projectId]. When [draftFrames] is set, the frames drawn in that working
     * directory are published to the project first. Pack Creator passes null because it draws
     * straight into the project's frames.
     */
    suspend fun write(
        projectId: String,
        document: ProjectSaveDocument,
        draftFrames: File?,
        time: Long,
    ): StoreResult<ProjectManifest> = withContext(ioDispatcher) {
        val projects = repositories.projects
        // Re-read the manifest first so a save never clobbers the project's own
        // display name, which the document itself does not carry.
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
                ProjectFrameDraft.publish(
                    draftFrames,
                    File(repositories.actorStore.projectDirectory(projectId), "frames"),
                )
            }
        } catch (failure: Exception) {
            return@withContext StoreResult.Failure(failure.message ?: "Could not save frame pixels", failure)
        }
        val saveResult = projects.save(manifest, document)
        if (saveResult is StoreResult.Value) {
            // The thumbnail is regenerated on every save so the gallery always shows the
            // current scene. It costs one composited frame.
            val thumbnailPath = runCatching { writeThumbnail(projectId, document) }.getOrNull()
            // Keep the Room gallery index in step with an in-editor save as well as with
            // gallery-side create, rename and delete, so it never shows a stale duration.
            runCatching { indexProject(manifest, thumbnailPath) }
        }
        saveResult
    }

    private suspend fun writeThumbnail(projectId: String, document: ProjectSaveDocument): String {
        val project = document.project
        val actorRenderer = if (project.actorPlacements.isEmpty()) null else repositories.actorFrameRenderer()
        val backgroundBitmap = project.backgroundAssetId
            ?.let { project.assets[it]?.sourcePath }
            ?.let { path -> BitmapFactory.decodeFile(path) }
        val compositor = ProjectFrameCompositor(
            project,
            document.frameLayers,
            cacheManager,
            actorRenderer,
            backgroundBitmap,
            includeFades = false,
        )
        try {
            val posterFrame = CameraPosterFrame.select(project.cameraShots, document.totalFrames, project.fps)
            val frame = compositor.composite(posterFrame)
            val thumbnailFile = repositories.projects.thumbnailFile(projectId)
            thumbnailFile.parentFile?.mkdirs()
            FileOutputStream(thumbnailFile).use { out -> frame.compress(Bitmap.CompressFormat.PNG, 90, out) }
            // compress() has read every pixel by now, so the compositor's bitmaps can be
            // released. This compositor is never shared with the live canvas.
            return thumbnailFile.absolutePath
        } finally {
            compositor.release()
        }
    }

    private suspend fun indexProject(manifest: ProjectManifest, thumbnailPath: String?) {
        AppDatabase.getInstance(context).projectDao().upsert(
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
