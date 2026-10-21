package com.donnclab.kinetiframe.data.library

import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.LibraryItemManifest
import com.donnclab.kinetiframe.data.actor.LibraryScope
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * The `.kpack` file format, implemented exactly like
 * [com.donnclab.kinetiframe.data.project.KframeArchiver] is for whole projects: a portable,
 * shareable ZIP that exists purely as an export/import *action* on top of a pack's
 * already-durable internal storage (a [LibraryRepository] item's own `payload/` directory), never
 * as the pack's live storage format. Because [PackManifest]'s internal `payload/meta.json` is
 * already, byte for byte, the exact `.kpack` `meta.json` schema, this archiver
 * does no format translation at all -- it just flattens `payload/` into a zip root on export, and
 * reverses that on import.
 */
object PackArchiver {
    private val json = Json { ignoreUnknownKeys = true }

    /** Zips a pack's `payload/` directory (its `meta.json` plus every `frame_NNN.png`) into
     *  [destination]. The `payload/` prefix is dropped so those files sit at the archive root,
     *  which is the `.kpack` layout. */
    fun export(
        files: ActorFileStore,
        library: LibraryRepository,
        itemId: String,
        destination: File,
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
    ): ActorStoreResult<File> = runCatching {
        when (val loaded = library.loadPack(itemId, scope, projectId)) {
            is ActorStoreResult.Failure -> throw IllegalStateException(loaded.message, loaded.cause)
            is ActorStoreResult.Value -> Unit
        }
        val itemDirectory = when (scope) {
            LibraryScope.PERSONAL -> files.libraryDirectory(itemId)

            LibraryScope.PROJECT -> files.projectVaultDirectory(
                requireNotNull(projectId) {
                    "A project-scoped pack needs a project id"
                },
                itemId,
            )
        }
        val payloadDirectory = library.packPayloadDirectory(itemId, scope, projectId)
        require(payloadDirectory.isDirectory) { "Pack payload is missing" }
        destination.parentFile?.mkdirs()
        ZipOutputStream(destination.outputStream().buffered()).use { zip ->
            payloadDirectory.listFiles().orEmpty().filter(File::isFile).forEach { file ->
                zip.putNextEntry(ZipEntry(file.name))
                file.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }
        }
        destination
    }.fold(::value, ::failure)

    /** Unzips [source] and hands its `meta.json` + frame files to [LibraryRepository.exportPack],
     *  which re-validates the pack the same way any locally-created pack would be. [itemId] is a
     *  freshly minted local ID -- imported packs never reuse the sharer's own item ID, since two
     *  people's exports of "my_mouth_pack.kpack" are otherwise indistinguishable collisions. */
    fun import(
        files: ActorFileStore,
        library: LibraryRepository,
        source: File,
        itemId: String,
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
        tags: Set<String> = emptySet(),
        nowEpochMs: Long,
    ): ActorStoreResult<LibraryItemManifest> = runCatching {
        require(source.isFile) { "Pack file not found" }
        val stagingDir = File(files.libraryRoot(), "_pack-import-${UUID.randomUUID()}")
        require(stagingDir.mkdirs()) { "Could not prepare a location to unpack this pack" }
        try {
            ZipInputStream(source.inputStream().buffered()).use { zip ->
                generateSequence { zip.nextEntry }.forEach { entry ->
                    val outFile = File(stagingDir, entry.name)
                    require(outFile.canonicalPath.startsWith(stagingDir.canonicalPath + File.separator)) {
                        "Pack contains an unsafe file path"
                    }
                    if (!entry.isDirectory) {
                        outFile.parentFile?.mkdirs()
                        outFile.outputStream().use { zip.copyTo(it) }
                    }
                    zip.closeEntry()
                }
            }
            val metaFile = File(stagingDir, "meta.json")
            require(metaFile.isFile) { "This file is not a valid pack (.kpack) archive" }
            val packManifest = json.decodeFromString<PackManifest>(metaFile.readText()).validate()
            val frameFiles = packManifest.items.associate { it.file to File(stagingDir, it.file) }
            when (
                val exported = library.exportPack(
                    itemId = itemId,
                    displayName = packManifest.packName,
                    packManifest = packManifest,
                    frameFiles = frameFiles,
                    scope = scope,
                    projectId = projectId,
                    tags = tags,
                    nowEpochMs = nowEpochMs,
                )
            ) {
                is ActorStoreResult.Value -> exported.value
                is ActorStoreResult.Failure -> throw IllegalStateException(exported.message, exported.cause)
            }
        } finally {
            stagingDir.deleteRecursively()
        }
    }.fold(::value, ::failure)

    private fun <T> value(value: T): ActorStoreResult<T> = ActorStoreResult.Value(value)
    private fun failure(error: Throwable): ActorStoreResult.Failure =
        ActorStoreResult.Failure(error.message ?: "Pack archive operation failed", error)
}
