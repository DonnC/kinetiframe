package com.donnclab.kinetiframe.library.domain

import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.model.LibraryOrganization
import java.io.File

/**
 * Everything the library screen lists, read from disk: saved projects and their performances,
 * actors, and library items from the personal library and every project's vault. Actors and
 * performances are scanned from their files rather than the Room index, so the library shows what
 * is on disk. No bitmaps are decoded here. Call off the main thread.
 */
class LibraryCatalog(
    private val files: DocumentFileStore,
    private val library: LibraryRepository,
    private val projects: ProjectRepository,
    private val actors: ActorRepository,
    private val performances: PerformanceRepository,
) {
    data class ProjectEntry(
        val id: String,
        val name: String,
        val fps: Int,
        val width: Int,
        val height: Int,
        val thumbnailPath: String?,
    )

    data class PerformanceEntry(val id: String, val name: String, val projectName: String, val durationMicros: Long)

    data class ActorEntry(val id: String, val name: String, val templateId: String, val thumbnailPath: String?)

    /**
     * A library item. [id] is qualified by scope and project, since project vaults can reuse item
     * IDs. [projectName] names the vault's project, or is null for a personal item or a project
     * that no longer exists. [imagePath] is a background or frame's file, or a pack's first
     * drawing; [drawingCount] is set for packs.
     */
    data class ItemEntry(
        val id: String,
        val item: LibraryItemManifest,
        val projectName: String?,
        val imagePath: String?,
        val audioPath: String?,
        val drawingCount: Int?,
    )

    /** One drawing of a pack: its file name, its name and its file on disk. */
    data class PackDrawing(val fileName: String, val name: String, val path: String)

    /** [packItemNames] holds each pack's drawing names and descriptions, for search, by pack key. */
    data class Snapshot(
        val projects: List<ProjectEntry>,
        val performances: List<PerformanceEntry>,
        val actors: List<ActorEntry>,
        val items: List<ItemEntry>,
        val packItemNames: Map<String, List<String>>,
        val organization: LibraryOrganization,
    )

    fun organizationStore() = library.organizationStore()

    /** Reads the whole library. [packKey] names a pack in [Snapshot.packItemNames]. */
    fun load(packKey: (LibraryItemManifest) -> String): Snapshot {
        val savedProjects = value(projects.list())
        val projectEntries = mutableListOf<ProjectEntry>()
        val performanceEntries = mutableListOf<PerformanceEntry>()
        savedProjects.forEach { project ->
            projectEntries += ProjectEntry(
                "project:${project.projectId}",
                project.displayName,
                project.fps,
                project.stage.width,
                project.stage.height,
                project.thumbnailPath,
            )
            value(performances.list(project.projectId)).forEach { performance ->
                performanceEntries += PerformanceEntry(
                    "performance:${project.projectId}:${performance.performanceId}",
                    performance.displayName,
                    project.displayName,
                    performance.durationMicros,
                )
            }
        }
        val actorEntries = value(actors.listWithThumbnails()).map { (actor, thumbnail) ->
            ActorEntry("actor:${actor.actorId}", actor.displayName, actor.templateId, thumbnail)
        }
        val itemEntries = mutableListOf<ItemEntry>()
        val packItemNames = mutableMapOf<String, List<String>>()
        val manifests =
            value(library.list()) + savedProjects.flatMap { value(library.list(LibraryScope.PROJECT, it.projectId)) }
        manifests.forEach { item ->
            // Palettes have their own exact-value browser, not drawing or project cards.
            if (item.kind == LibraryItemKind.PALETTE) return@forEach
            val personal = item.scope == LibraryScope.PERSONAL
            val directory = if (personal) {
                files.libraryDirectory(item.libraryItemId)
            } else {
                files.projectVaultDirectory(requireNotNull(item.sourceProjectId), item.libraryItemId)
            }
            val payload = item.payloadFileName?.let {
                File(directory, "payload/$it").takeIf(File::isFile)?.absolutePath
            }
            val pack = if (item.kind == LibraryItemKind.PACK) {
                (library.loadPack(item.libraryItemId, item.scope, item.sourceProjectId) as? StoreResult.Value)
                    ?.value?.second
            } else {
                null
            }
            val packImage = pack?.items?.firstOrNull()?.let {
                library.packItemFile(item.libraryItemId, it.file, item.scope, item.sourceProjectId)
                    .takeIf(File::isFile)?.absolutePath
            }
            if (pack != null) packItemNames[packKey(item)] = pack.items.map { it.name + " " + it.desc }
            val pictured = item.kind == LibraryItemKind.FRAME || item.kind == LibraryItemKind.BACKGROUND_IMAGE
            itemEntries += ItemEntry(
                id = "${item.scope}:${if (personal) "" else item.sourceProjectId}:${item.libraryItemId}",
                item = item,
                projectName = savedProjects.takeUnless { personal }
                    ?.firstOrNull { it.projectId == item.sourceProjectId }?.displayName,
                imagePath = (if (pictured) payload else null) ?: packImage,
                audioPath = if (item.kind == LibraryItemKind.AUDIO) payload else null,
                drawingCount = pack?.items?.size,
            )
        }
        return Snapshot(
            projectEntries,
            performanceEntries,
            actorEntries,
            itemEntries,
            packItemNames,
            organizationStore().load(),
        )
    }

    fun packContents(item: LibraryItemManifest): List<PackDrawing> =
        value(library.loadPack(item.libraryItemId, item.scope, item.sourceProjectId)).second.items.map { drawing ->
            PackDrawing(
                drawing.file,
                drawing.name,
                library.packItemFile(item.libraryItemId, drawing.file, item.scope, item.sourceProjectId).absolutePath,
            )
        }

    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
    }
}
