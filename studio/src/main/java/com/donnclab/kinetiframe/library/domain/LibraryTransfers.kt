// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.library.domain

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.donnclab.kinetiframe.actors.data.ActorBundleRepository
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.model.PackTrayItem
import com.donnclab.kinetiframe.media.audio.AudioDurations
import java.io.File
import java.io.IOException
import java.util.UUID

/** A library item fetched to local disk and ready to place in the open project. */
sealed interface LibraryPick {
    data class Audio(val path: String, val durationMicros: Long, val label: String) : LibraryPick
    data class Background(val bitmap: Bitmap) : LibraryPick
    data class Stamp(val bitmap: Bitmap) : LibraryPick
    data class Actor(val displayName: String) : LibraryPick
    data class Performance(val document: PerformanceDocument) : LibraryPick
}

/**
 * Moves items between the library and a project open in the editor: fetching an item to place
 * in it, opening a pack as the drawing tray, and adding the project's background, frames, audio
 * clips and performances to the library. Temporary copies go to [cacheDir]. Every function does
 * file work; call them off the main thread.
 */
class LibraryTransfers(
    private val library: LibraryRepository,
    private val bundles: ActorBundleRepository,
    private val cacheDir: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /**
     * Fetches [manifest] for the project [projectId]. Audio is copied into [audioDirectory], the
     * project's own audio folder; actors and performances are imported at [time].
     */
    fun pick(manifest: LibraryItemManifest, projectId: String, time: Long, audioDirectory: File): Result<LibraryPick> =
        runCatching {
            when (manifest.kind) {
                LibraryItemKind.AUDIO -> {
                    val destination = File(audioDirectory, "audio-${UUID.randomUUID()}.audio")
                    fetchPayload(manifest, destination, projectId)
                    val durationMicros = AudioDurations.readMicros(destination.absolutePath)
                    LibraryPick.Audio(destination.absolutePath, durationMicros, manifest.displayName)
                }

                LibraryItemKind.BACKGROUND_IMAGE -> {
                    val destination = File(cacheDir, "library-bg-${UUID.randomUUID()}")
                    fetchPayload(manifest, destination, projectId)
                    LibraryPick.Background(decode(destination, "Could not read that image"))
                }

                LibraryItemKind.FRAME -> {
                    val destination = File(cacheDir, "library-frame-stamp-${UUID.randomUUID()}.png")
                    fetchPayload(manifest, destination, projectId)
                    LibraryPick.Stamp(decode(destination, "Could not read that frame"))
                }

                LibraryItemKind.ACTOR -> {
                    val imported = bundles.importActor(
                        manifest.libraryItemId,
                        "actor-${UUID.randomUUID()}",
                        "revision-${UUID.randomUUID()}",
                        time,
                    )
                    LibraryPick.Actor(imported.orThrow().displayName)
                }

                LibraryItemKind.PERFORMANCE -> {
                    val imported = bundles.importPerformance(
                        manifest.libraryItemId,
                        projectId,
                        "performance-${UUID.randomUUID()}",
                        time,
                    )
                    LibraryPick.Performance(imported.orThrow())
                }

                else -> throw IllegalStateException("This item can't be added from here yet")
            }
        }

    /** Copies and decodes every drawing in a pack once, so the tray can drag them without reading disk again. */
    fun loadPackTray(manifest: LibraryItemManifest, projectId: String): Result<List<PackTrayItem>> = runCatching {
        val projectIdForScope = projectId.takeIf { manifest.scope == LibraryScope.PROJECT }
        val (_, packManifest) = library.loadPack(manifest.libraryItemId, manifest.scope, projectIdForScope).orThrow()
        packManifest.items.map { entry ->
            val destination = File(cacheDir, "pack-tray-${UUID.randomUUID()}.png")
            library.copyPackItemTo(manifest.libraryItemId, entry.file, destination, manifest.scope, projectIdForScope)
                .orThrow()
            PackTrayItem(entry.file, entry.name, decode(destination, "Could not read \"${entry.name}\""))
        }
    }

    /** Adds the project's background image to the library. */
    fun exportBackground(project: ProjectDocument, scope: LibraryScope): StoreResult<LibraryItemManifest> =
        exportAsset(LibraryItemKind.BACKGROUND_IMAGE, "Background", scope, project.id) {
            val sourcePath = project.backgroundAssetId
                ?.let { project.assets[it]?.sourcePath }
                ?: throw IllegalStateException("No background image is set")
            File(sourcePath)
        }

    /** Adds one drawn [frame] to the library as [displayName]. */
    fun exportFrame(
        frame: Bitmap,
        displayName: String,
        scope: LibraryScope,
        projectId: String,
    ): StoreResult<LibraryItemManifest> = exportAsset(LibraryItemKind.FRAME, displayName, scope, projectId) {
        val tempFile = File(cacheDir, "library-frame-${UUID.randomUUID()}.png")
        tempFile.outputStream().use { out -> frame.compress(Bitmap.CompressFormat.PNG, 100, out) }
        tempFile
    }

    /** Adds an audio clip's source file to the library. */
    fun exportAudioClip(
        clip: AudioClipPlacement,
        project: ProjectDocument,
        scope: LibraryScope,
    ): StoreResult<LibraryItemManifest> = exportAsset(LibraryItemKind.AUDIO, clip.label, scope, project.id) {
        val sourcePath = project.assets[clip.assetId]?.sourcePath
            ?: throw IllegalStateException("Could not find that audio clip's file")
        File(sourcePath)
    }

    /** Adds a placed performance to the library. */
    fun exportPerformance(projectId: String, placement: ActorPlacementReference): StoreResult<*> =
        bundles.exportPerformance(
            projectId,
            placement.performanceId,
            "library-${UUID.randomUUID()}",
            placement.label,
            now(),
        )

    private fun exportAsset(
        kind: LibraryItemKind,
        displayName: String,
        scope: LibraryScope,
        projectId: String,
        source: () -> File,
    ): StoreResult<LibraryItemManifest> = runCatching {
        library.exportAsset(
            kind = kind,
            itemId = "library-${UUID.randomUUID()}",
            displayName = displayName,
            sourceFile = source(),
            scope = scope,
            projectId = projectId,
            nowEpochMs = now(),
        )
    }.getOrElse { StoreResult.Failure(it.message ?: "Could not add that to your library", it) }

    /** Copies a raw-asset library item's payload to [destination]. */
    private fun fetchPayload(manifest: LibraryItemManifest, destination: File, projectId: String) {
        destination.parentFile?.mkdirs()
        // The picker browses only the personal library and the open project's own vault, so an
        // item whose scope is PROJECT always belongs to this project.
        val projectIdForScope = projectId.takeIf { manifest.scope == LibraryScope.PROJECT }
        library.copyAssetTo(manifest.libraryItemId, destination, manifest.scope, projectIdForScope).orThrow()
    }

    private fun decode(file: File, failure: String): Bitmap =
        BitmapFactory.decodeFile(file.absolutePath) ?: throw IOException(failure)

    private fun <T> StoreResult<T>.orThrow(): T = when (this) {
        is StoreResult.Value -> value
        is StoreResult.Failure -> throw IllegalStateException(message, cause)
    }
}
