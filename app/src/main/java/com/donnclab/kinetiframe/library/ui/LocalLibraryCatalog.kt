package com.donnclab.kinetiframe.library.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.ui.graphics.Color
import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryOrganization
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.ui.packs.PackBrowserIndex
import com.donnclab.kinetiframe.presentation.common.AssetDetails
import com.donnclab.kinetiframe.presentation.common.AssetKind
import com.donnclab.kinetiframe.presentation.common.AssetSummary
import com.donnclab.kinetiframe.presentation.common.AudioTrackSummary
import com.donnclab.kinetiframe.presentation.common.CardThumbnail
import com.donnclab.kinetiframe.presentation.common.PreviewKind
import com.donnclab.kinetiframe.presentation.common.StampAsset
import java.io.File

/** IO-only projection. Working projects/actors and captured reusable items are separate stores.
 * Scope-qualified IDs prevent collisions between project vaults; no bitmap decoding here. */
class LocalLibraryCatalog(private val files: DocumentFileStore, private val library: LibraryRepository) {
    data class Snapshot(
        val projects: List<AssetSummary>,
        val actors: List<AssetSummary>,
        val packs: List<AssetSummary>,
        val audio: List<AudioTrackSummary>,
        val items: Map<String, LibraryItemManifest>,
        val packItemNames: Map<String, List<String>> = emptyMap(),
        val organization: LibraryOrganization = LibraryOrganization(),
    )
    fun organizationStore() = library.organizationStore()

    fun load(): Snapshot {
        val projects = mutableListOf<AssetSummary>()
        val actors = mutableListOf<AssetSummary>()
        val packs = mutableListOf<AssetSummary>()
        val audio = mutableListOf<AudioTrackSummary>()
        val items = linkedMapOf<String, LibraryItemManifest>()
        val packItemNames = mutableMapOf<String, List<String>>()
        val savedProjects = value(ProjectRepository(files).list())
        savedProjects.forEach { project ->
            projects += card(
                "project:${project.projectId}",
                project.displayName,
                AssetKind.PROJECT,
                "${project.fps} FPS · ${project.stage.width}×${project.stage.height}",
                project.thumbnailPath,
            )
            value(PerformanceRepository(files).list(project.projectId)).forEach { performance ->
                actors += card(
                    "performance:${project.projectId}:${performance.performanceId}",
                    performance.displayName,
                    AssetKind.ACTOR_PERFORMANCE,
                    "${project.displayName} · ${performance.durationMicros / 1_000_000f}s",
                )
            }
        }
        value(ActorRepository(files).listWithThumbnails()).forEach { (actor, thumbnail) ->
            actors +=
                card("actor:${actor.actorId}", actor.displayName, AssetKind.ACTOR_RIG, actor.templateId, thumbnail)
        }
        val manifests =
            value(library.list()) + savedProjects.flatMap { value(library.list(LibraryScope.PROJECT, it.projectId)) }
        manifests.forEach { item ->
            // Palettes have their own exact-value browser, not drawing/project cards.
            if (item.kind == LibraryItemKind.PALETTE) return@forEach
            val projectId = if (item.scope == LibraryScope.PROJECT) item.sourceProjectId else ""
            val id = "${item.scope}:$projectId:${item.libraryItemId}"
            items[id] = item
            val directory = if (item.scope == LibraryScope.PERSONAL) {
                files.libraryDirectory(item.libraryItemId)
            } else {
                files.projectVaultDirectory(requireNotNull(item.sourceProjectId), item.libraryItemId)
            }
            val location = if (item.scope == LibraryScope.PERSONAL) {
                "Personal Library"
            } else {
                savedProjects.firstOrNull { it.projectId == item.sourceProjectId }?.displayName ?: "Project Library"
            }
            val kind = when (item.kind) {
                LibraryItemKind.PROJECT -> AssetKind.PROJECT
                LibraryItemKind.BACKGROUND_IMAGE -> AssetKind.BACKGROUND_IMAGE
                LibraryItemKind.FRAME -> AssetKind.FRAME
                LibraryItemKind.ACTOR, LibraryItemKind.BUNDLE -> AssetKind.ACTOR_RIG
                LibraryItemKind.PERFORMANCE -> AssetKind.ACTOR_PERFORMANCE
                LibraryItemKind.PACK -> AssetKind.PACK
                LibraryItemKind.AUDIO -> AssetKind.AUDIO
                LibraryItemKind.PALETTE -> error("Palettes use the colour browser")
            }
            val image = if (kind == AssetKind.FRAME || kind == AssetKind.BACKGROUND_IMAGE) {
                item.payloadFileName?.let { File(directory, "payload/$it").takeIf(File::isFile)?.absolutePath }
            } else {
                null
            }
            val pack = if (kind == AssetKind.PACK) {
                (
                    library.loadPack(
                        item.libraryItemId,
                        item.scope,
                        item.sourceProjectId,
                    ) as? StoreResult.Value
                    )?.value?.second
            } else {
                null
            }
            val packImage = pack?.items?.firstOrNull()?.let {
                library.packItemFile(
                    item.libraryItemId,
                    it.file,
                    item.scope,
                    item.sourceProjectId,
                ).takeIf(File::isFile)?.absolutePath
            }
            if (pack !=
                null
            ) {
                packItemNames[PackBrowserIndex.key(item)] =
                    pack.items.map { it.name + " " + it.desc }
            }
            val summary = card(
                id,
                item.displayName,
                kind,
                if (pack != null) "${pack.items.size} drawings · $location" else location,
                image ?: packImage,
            )
            when (kind) {
                AssetKind.PROJECT, AssetKind.BACKGROUND_IMAGE, AssetKind.FRAME -> projects += summary

                AssetKind.ACTOR_RIG, AssetKind.ACTOR_PERFORMANCE -> actors += summary

                AssetKind.PACK -> packs += summary

                AssetKind.AUDIO -> {
                    val tags = item.tags.map(String::lowercase)
                    val tag = when {
                        "music" in tags -> "MUSIC"
                        "voice" in tags -> "VOICE"
                        else -> "SFX"
                    }
                    val path = item.payloadFileName?.let {
                        File(directory, "payload/$it").takeIf(File::isFile)?.absolutePath
                    }
                    audio += AudioTrackSummary(
                        id,
                        item.displayName,
                        location,
                        tag,
                        0,
                        details = summary.details.copy(
                            audioPath = path,
                            previewKind = if (path !=
                                null
                            ) {
                                PreviewKind.AUDIO_PLAYER
                            } else {
                                PreviewKind.STATIC_SCREENSHOT
                            },
                        ),
                    )
                }
            }
        }
        return Snapshot(projects, actors, packs, audio, items, packItemNames, organizationStore().load())
    }

    fun packContents(item: LibraryItemManifest): List<StampAsset> =
        value(library.loadPack(item.libraryItemId, item.scope, item.sourceProjectId)).second.items.map { drawing ->
            StampAsset(
                drawing.file,
                drawing.name,
                Icons.Filled.Folder,
                Color.Gray,
                library.packItemFile(item.libraryItemId, drawing.file, item.scope, item.sourceProjectId).absolutePath,
            )
        }

    private fun card(id: String, name: String, kind: AssetKind, info: String, image: String? = null): AssetSummary {
        val thumbnail = image?.let { CardThumbnail.LocalImage(it) }
            ?: CardThumbnail.IconOnGradient(
                Color(0xFF677D9A),
                Color(0xFF374C67),
                when (kind) {
                    AssetKind.PROJECT -> Icons.Filled.Movie
                    AssetKind.ACTOR_RIG -> Icons.Filled.Person
                    AssetKind.ACTOR_PERFORMANCE -> Icons.AutoMirrored.Filled.DirectionsRun
                    AssetKind.PACK -> Icons.Filled.ViewModule
                    AssetKind.AUDIO -> Icons.Filled.AudioFile
                    AssetKind.BACKGROUND_IMAGE -> Icons.Filled.Image
                    AssetKind.FRAME -> Icons.Filled.Brush
                },
            )
        // Real local entries never claim the sample UI's automatic live/audio preview.
        val details = AssetDetails(id, kind, PreviewKind.STATIC_SCREENSHOT, name, info, thumbnail)
        return AssetSummary(id, name, info, thumbnail, details = details)
    }

    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
    }
}
