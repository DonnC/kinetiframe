package com.donnclab.kinetiframe.library.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.project.ProjectSaveDocument
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.model.PackFrameTag
import com.donnclab.kinetiframe.library.model.PackItemEntry
import com.donnclab.kinetiframe.library.model.PackManifest
import com.donnclab.kinetiframe.studio.render.FrameCacheManager
import kotlinx.coroutines.coroutineScope
import java.io.File
import java.util.UUID

/**
 * Pack drafts: the scratch projects the Pack Creator draws in. A draft is an ordinary project
 * directory with [ProjectManifest.isPackDraft] set, which keeps it out of the gallery, so the
 * normal editor opens it. Saving flattens its tagged frames into a PACK library item and deletes
 * the draft. Editing a saved pack copies its drawings and tags into a new draft, and saving
 * replaces that same library item, so the pack keeps its identity.
 *
 * Every function does file work; call them off the main thread.
 */
class PackDrafts(
    private val files: DocumentFileStore,
    private val projects: ProjectRepository,
    private val library: LibraryRepository,
    private val cacheDir: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** A saved [pack] opened as the draft [draftId], with its frames' [tags] by frame index. */
    data class EditDraft(
        val draftId: String,
        val pack: LibraryItemManifest,
        val tags: Map<Int, PackFrameTag>,
        val canvasDimension: Int,
    )

    /** Creates an empty one-frame draft with a square canvas, and returns its ID. */
    fun create(canvasDimension: Int): StoreResult<String> {
        val id = newDraftId()
        return when (val saved = saveDraft(id, canvasDimension, totalFrames = 1)) {
            is StoreResult.Value -> StoreResult.Value(id)
            is StoreResult.Failure -> saved
        }
    }

    /**
     * Opens the saved pack [itemId] as a new draft: its drawings become the draft's frames, in
     * order, on the "main" layer the editor expects, and their names become the frames' tags.
     */
    suspend fun openForEdit(itemId: String, scope: LibraryScope, projectId: String?): StoreResult<EditDraft> =
        runCatching {
            val (manifest, pack) = value(library.loadPack(itemId, scope, projectId))
            val items = pack.items.sortedBy { it.index }
            val totalFrames = items.size.coerceAtLeast(1)
            val id = newDraftId()
            value(saveDraft(id, pack.canvasDimension, totalFrames))
            val tags = mutableMapOf<Int, PackFrameTag>()
            coroutineScope {
                val frames = FrameCacheManager(cacheDir = framesOf(id), scope = this)
                items.forEachIndexed { index, entry ->
                    val source = library.packItemFile(itemId, entry.file, scope, projectId)
                    val bitmap = BitmapFactory.decodeFile(source.absolutePath)
                        ?: throw IllegalStateException("One of this pack's drawings could not be read")
                    frames.putFrame(MAIN_LAYER, index, bitmap)
                    tags[index] = PackFrameTag(entry.name, entry.desc)
                }
                // putFrame only guarantees the memory cache. The editor may read the draft from disk
                // before it ever sees this cache, so every frame is written out now.
                frames.persistAllToDisk(listOf(MAIN_LAYER), totalFrames)
            }
            EditDraft(id, manifest, tags, pack.canvasDimension)
        }.fold({ StoreResult.Value(it) }, { StoreResult.Failure(it.message ?: "Could not open this pack", it) })

    /**
     * Saves the first [totalFrames] frames of [draftId] as a pack named [name], then deletes the
     * draft. Every frame needs a tag with a name. [editing] is the pack the draft was opened from:
     * that item is replaced and keeps its ID, scope and creation time; without it a new personal
     * pack is created. [totalFrames] comes from the editor, since frames may have been added or
     * removed since the draft was written.
     */
    suspend fun save(
        draftId: String,
        canvasDimension: Int,
        name: String,
        totalFrames: Int,
        tags: Map<Int, PackFrameTag>,
        editing: LibraryItemManifest?,
    ): StoreResult<Unit> = runCatching {
        require(totalFrames > 0) { "A pack must contain at least one frame" }
        val untagged = (0 until totalFrames).filter { tags[it]?.name.isNullOrBlank() }
        require(untagged.isEmpty()) {
            "Every frame needs a name before you can save this pack (frame ${untagged.first() + 1} is untagged)"
        }
        val staging = File(cacheDir, "pack-bundle-${UUID.randomUUID()}").apply { mkdirs() }
        try {
            val frameFiles = mutableMapOf<String, File>()
            val items = mutableListOf<PackItemEntry>()
            coroutineScope {
                val frames = FrameCacheManager(cacheDir = framesOf(draftId), scope = this)
                for (index in 0 until totalFrames) {
                    val bitmap = frames.getFrame(MAIN_LAYER, index)
                        ?: throw IllegalStateException("Frame ${index + 1} has no drawing yet")
                    val fileName = "frame_%03d.png".format(index + 1)
                    val frameFile = File(staging, fileName)
                    frameFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    frameFiles[fileName] = frameFile
                    val tag = tags.getValue(index)
                    items += PackItemEntry(file = fileName, index = index, name = tag.name, desc = tag.description)
                }
            }
            val pack = PackManifest(
                packName = name,
                totalItems = items.size,
                canvasDimension = canvasDimension,
                items = items,
            ).validate()
            // The payload directory is overwritten in place. Drawings a smaller pack no longer
            // names stay on disk unreferenced, and only the files the manifest lists are read.
            value(
                library.exportPack(
                    itemId = editing?.libraryItemId ?: "library-${UUID.randomUUID()}",
                    displayName = name,
                    packManifest = pack,
                    frameFiles = frameFiles,
                    scope = editing?.scope ?: LibraryScope.PERSONAL,
                    projectId = editing?.sourceProjectId,
                    tags = editing?.tags ?: emptySet(),
                    nowEpochMs = editing?.createdAtEpochMs ?: now(),
                ),
            )
            projects.delete(draftId)
            Unit
        } finally {
            staging.deleteRecursively()
        }
    }.fold({ StoreResult.Value(it) }, { StoreResult.Failure(it.message ?: "Could not save this pack", it) })

    /** Deletes [draftId]. Synchronous, so it is safe to call while a screen or ViewModel ends. */
    fun discard(draftId: String) {
        files.projectDirectory(draftId).deleteRecursively()
    }

    private fun saveDraft(id: String, canvasDimension: Int, totalFrames: Int): StoreResult<ProjectManifest> {
        val time = now()
        val stage = StageDimensions(canvasDimension, canvasDimension)
        val layer = Layer(name = "Main Layer", id = MAIN_LAYER)
        val document = ProjectSaveDocument(
            project = ProjectDocument(id = id, stage = stage, fps = DRAFT_FPS, layers = listOf(layer)),
            frameLayers = mapOf(0 to listOf(layer)),
            totalFrames = totalFrames,
        )
        val manifest = ProjectManifest(
            projectId = id,
            displayName = "Pack Draft",
            stage = stage,
            fps = DRAFT_FPS,
            createdAtEpochMs = time,
            updatedAtEpochMs = time,
            totalFrames = totalFrames,
            isPackDraft = true,
        )
        return projects.save(manifest, document)
    }

    private fun framesOf(draftId: String) = File(files.projectDirectory(draftId), "frames")

    private fun newDraftId() = "pack-draft-${UUID.randomUUID()}"

    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
    }

    private companion object {
        const val MAIN_LAYER = "main"
        const val DRAFT_FPS = 12
    }
}
