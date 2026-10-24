// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.donnclab.kinetiframe.actors.data.LibraryItemKind
import com.donnclab.kinetiframe.actors.data.LibraryItemManifest
import com.donnclab.kinetiframe.actors.data.LibraryScope
import com.donnclab.kinetiframe.actors.data.PerformanceDocument
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.document.AudioClipPlacement
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.library.ui.packs.PackTrayItem
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

/** A library item fetched to local disk and ready to place in the open project. */
internal sealed interface LibraryPick {
    data class Audio(val path: String, val durationMicros: Long, val label: String) : LibraryPick
    data class Background(val bitmap: Bitmap) : LibraryPick
    data class Stamp(val bitmap: Bitmap) : LibraryPick
    data class Actor(val displayName: String) : LibraryPick
    data class Performance(val document: PerformanceDocument) : LibraryPick
}

/**
 * What the editor's "From Library" item can pick from. PROJECT is left out because a project is
 * imported from the gallery, not into another open project. BUNDLE is left out because no screen
 * creates bundles yet. A PACK opens the pack tray instead of being placed.
 */
internal val EditorLibraryKinds = setOf(
    LibraryItemKind.ACTOR,
    LibraryItemKind.PERFORMANCE,
    LibraryItemKind.AUDIO,
    LibraryItemKind.BACKGROUND_IMAGE,
    LibraryItemKind.FRAME,
    LibraryItemKind.PACK,
)

/** Moves items between the library and the open project. Every call does its file work on [ioDispatcher]. */
internal class EditorLibraryActions(
    private val context: Context,
    private val repositories: EditorRepositories,
    private val media: EditorMediaImporter,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val library get() = repositories.library

    suspend fun pick(manifest: LibraryItemManifest, projectId: String, time: Long): Result<LibraryPick> =
        withContext(ioDispatcher) {
            runCatching {
                when (manifest.kind) {
                    LibraryItemKind.AUDIO -> {
                        val destination = File(media.audioDirectory, "audio-${UUID.randomUUID()}.audio")
                        fetchPayload(manifest, destination, projectId)
                        val durationMicros = media.readAudioDurationMicros(destination.absolutePath)
                        LibraryPick.Audio(destination.absolutePath, durationMicros, manifest.displayName)
                    }

                    LibraryItemKind.BACKGROUND_IMAGE -> {
                        val destination = File(context.cacheDir, "library-bg-${UUID.randomUUID()}")
                        fetchPayload(manifest, destination, projectId)
                        LibraryPick.Background(decode(destination, "Could not read that image"))
                    }

                    LibraryItemKind.FRAME -> {
                        val destination = File(context.cacheDir, "library-frame-stamp-${UUID.randomUUID()}.png")
                        fetchPayload(manifest, destination, projectId)
                        LibraryPick.Stamp(decode(destination, "Could not read that frame"))
                    }

                    LibraryItemKind.ACTOR -> {
                        val newActorId = "actor-${UUID.randomUUID()}"
                        val newRevisionId = "revision-${UUID.randomUUID()}"
                        val imported = repositories.bundles.importActor(
                            manifest.libraryItemId,
                            newActorId,
                            newRevisionId,
                            time,
                        )
                        LibraryPick.Actor(imported.orThrow().displayName)
                    }

                    LibraryItemKind.PERFORMANCE -> {
                        val newPerformanceId = "performance-${UUID.randomUUID()}"
                        val imported = repositories.bundles.importPerformance(
                            manifest.libraryItemId,
                            projectId,
                            newPerformanceId,
                            time,
                        )
                        LibraryPick.Performance(imported.orThrow())
                    }

                    else -> throw IllegalStateException("This item can't be added from here yet")
                }
            }
        }

    /** Copies and decodes every drawing in a pack once, so the tray can drag them without reading disk again. */
    suspend fun loadPackTray(manifest: LibraryItemManifest, projectId: String): Result<List<PackTrayItem>> =
        withContext(ioDispatcher) {
            runCatching {
                val projectIdForScope = projectId.takeIf { manifest.scope == LibraryScope.PROJECT }
                val (_, packManifest) = library.loadPack(manifest.libraryItemId, manifest.scope, projectIdForScope)
                    .orThrow()
                packManifest.items.map { entry ->
                    val destination = File(context.cacheDir, "pack-tray-${UUID.randomUUID()}.png")
                    library.copyPackItemTo(
                        manifest.libraryItemId,
                        entry.file,
                        destination,
                        manifest.scope,
                        projectIdForScope,
                    ).orThrow()
                    PackTrayItem(entry.file, entry.name, decode(destination, "Could not read \"${entry.name}\""))
                }
            }
        }

    /** Adds the project's background image to the library and returns the message to show. */
    suspend fun exportBackground(project: ProjectDocument, scope: LibraryScope): String {
        val fallback = "Could not add the background to your library"
        val result = exportAsset(LibraryItemKind.BACKGROUND_IMAGE, "Background", scope, project.id, fallback) {
            val sourcePath = project.backgroundAssetId
                ?.let { project.assets[it]?.sourcePath }
                ?: throw IllegalStateException("No background image is set")
            File(sourcePath)
        }
        return result.message("Added background image to your library")
    }

    /** Adds one drawn frame to the library and returns the message to show. */
    suspend fun exportFrame(frame: Bitmap, displayName: String, scope: LibraryScope, projectId: String): String {
        val fallback = "Could not add that frame to your library"
        val result = exportAsset(LibraryItemKind.FRAME, displayName, scope, projectId, fallback) {
            val tempFile = File(context.cacheDir, "library-frame-${UUID.randomUUID()}.png")
            tempFile.outputStream().use { out -> frame.compress(Bitmap.CompressFormat.PNG, 100, out) }
            tempFile
        }
        return result.message("Added frame to your library")
    }

    /** Adds an audio clip's source file to the library and returns the message to show. */
    suspend fun exportAudioClip(clip: AudioClipPlacement, project: ProjectDocument, scope: LibraryScope): String {
        val fallback = "Could not add that clip to your library"
        val result = exportAsset(LibraryItemKind.AUDIO, clip.label, scope, project.id, fallback) {
            val sourcePath = project.assets[clip.assetId]?.sourcePath
                ?: throw IllegalStateException("Could not find that audio clip's file")
            File(sourcePath)
        }
        return result.message("Added \"${clip.label}\" to your library")
    }

    /** Adds a placed performance to the library and returns the message to show. */
    suspend fun exportPerformance(projectId: String, placement: ActorPlacementReference): String {
        val result = withContext(ioDispatcher) {
            repositories.bundles.exportPerformance(
                projectId,
                placement.performanceId,
                "library-${UUID.randomUUID()}",
                placement.label,
                System.currentTimeMillis(),
            )
        }
        return when (result) {
            is StoreResult.Value -> "Added ${placement.label} to your library"
            is StoreResult.Failure -> result.message
        }
    }

    private suspend fun exportAsset(
        kind: LibraryItemKind,
        displayName: String,
        scope: LibraryScope,
        projectId: String,
        fallback: String,
        source: () -> File,
    ): StoreResult<LibraryItemManifest> = withContext(ioDispatcher) {
        runCatching {
            library.exportAsset(
                kind = kind,
                itemId = "library-${UUID.randomUUID()}",
                displayName = displayName,
                sourceFile = source(),
                scope = scope,
                projectId = projectId,
                nowEpochMs = System.currentTimeMillis(),
            )
        }.getOrElse { StoreResult.Failure(it.message ?: fallback, it) }
    }

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
}

private fun <T> StoreResult<T>.orThrow(): T = when (this) {
    is StoreResult.Value -> value
    is StoreResult.Failure -> throw IllegalStateException(message, cause)
}

private fun StoreResult<*>.message(success: String): String = when (this) {
    is StoreResult.Value -> success
    is StoreResult.Failure -> message
}
