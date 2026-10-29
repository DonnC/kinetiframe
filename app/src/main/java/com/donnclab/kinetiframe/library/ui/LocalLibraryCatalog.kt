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
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.library.domain.LibraryCatalog
import com.donnclab.kinetiframe.library.model.LibraryOrganization
import com.donnclab.kinetiframe.library.ui.packs.PackBrowserIndex
import com.donnclab.kinetiframe.presentation.common.AssetDetails
import com.donnclab.kinetiframe.presentation.common.AssetKind
import com.donnclab.kinetiframe.presentation.common.AssetSummary
import com.donnclab.kinetiframe.presentation.common.AudioTrackSummary
import com.donnclab.kinetiframe.presentation.common.CardThumbnail
import com.donnclab.kinetiframe.presentation.common.PreviewKind
import com.donnclab.kinetiframe.presentation.common.StampAsset

/**
 * Turns the library's contents, read by [source], into the cards the library screen shows: their
 * names, details, thumbnails and icons. No bitmaps are decoded here; call off the main thread.
 */
class LocalLibraryCatalog(val source: LibraryCatalog) {
    data class Snapshot(
        val projects: List<AssetSummary>,
        val actors: List<AssetSummary>,
        val packs: List<AssetSummary>,
        val audio: List<AudioTrackSummary>,
        val items: Map<String, LibraryItemManifest>,
        val packItemNames: Map<String, List<String>> = emptyMap(),
        val organization: LibraryOrganization = LibraryOrganization(),
    )

    fun load(): Snapshot {
        val contents = source.load(PackBrowserIndex::key)
        val projects = mutableListOf<AssetSummary>()
        val actors = mutableListOf<AssetSummary>()
        val packs = mutableListOf<AssetSummary>()
        val audio = mutableListOf<AudioTrackSummary>()
        contents.projects.forEach { project ->
            projects += card(
                project.id,
                project.name,
                AssetKind.PROJECT,
                "${project.fps} FPS · ${project.width}×${project.height}",
                project.thumbnailPath,
            )
        }
        contents.performances.forEach { performance ->
            actors += card(
                performance.id,
                performance.name,
                AssetKind.ACTOR_PERFORMANCE,
                "${performance.projectName} · ${performance.durationMicros / 1_000_000f}s",
            )
        }
        contents.actors.forEach { actor ->
            actors += card(actor.id, actor.name, AssetKind.ACTOR_RIG, actor.templateId, actor.thumbnailPath)
        }
        contents.items.forEach { entry ->
            val item = entry.item
            val location = when {
                item.scope == LibraryScope.PERSONAL -> "Personal Library"
                else -> entry.projectName ?: "Project Library"
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
            val info = entry.drawingCount?.let { "$it drawings · $location" } ?: location
            val summary = card(entry.id, item.displayName, kind, info, entry.imagePath)
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
                    val path = entry.audioPath
                    audio += AudioTrackSummary(
                        entry.id,
                        item.displayName,
                        location,
                        tag,
                        0,
                        details = summary.details.copy(
                            audioPath = path,
                            previewKind = if (path != null) PreviewKind.AUDIO_PLAYER else PreviewKind.STATIC_SCREENSHOT,
                        ),
                    )
                }
            }
        }
        val items = contents.items.associate { it.id to it.item }
        return Snapshot(projects, actors, packs, audio, items, contents.packItemNames, contents.organization)
    }

    fun packContents(item: LibraryItemManifest): List<StampAsset> = source.packContents(item).map { drawing ->
        StampAsset(drawing.fileName, drawing.name, Icons.Filled.Folder, Color.Gray, drawing.path)
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
}
