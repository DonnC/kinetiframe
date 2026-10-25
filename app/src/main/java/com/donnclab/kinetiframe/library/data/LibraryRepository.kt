package com.donnclab.kinetiframe.library.data

import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The generic half of this app's local library: listing and deleting any item regardless of kind
 * (every item is just a `<root>/<itemId>/manifest.json`, whichever repository wrote it), plus
 * export/import for the four "raw asset" kinds in [LibraryItemManifest.RAW_ASSET_KINDS] -- audio,
 * a background image, a flattened frame PNG, or a whole-project `.kframe` archive -- and for PACK,
 * a named collection of several stamp-ready drawings (see [PackManifest]). ACTOR/PERFORMANCE/
 * BUNDLE items keep using [com.donnclab.kinetiframe.actors.data.ActorBundleRepository] for their structured
 * export/import (they need actor-domain dependencies this class deliberately has none of), but
 * every repository writes into the same manifest shape under [rootDirectory], so a single [list]
 * here surfaces everything for a shared browse UI no matter which repository created it.
 *
 * Every operation is scope-aware ([LibraryScope]): PERSONAL items live under the shared
 * `library/` root (available to every project); PROJECT items live nested inside one specific
 * project's own working directory (`projects/<id>/vault/`), which is why every PROJECT-scoped
 * method requires a [projectId]. Like [com.donnclab.kinetiframe.data.project.ProjectRepository], it
 * lists and deletes over a directory of manifests.
 */
class LibraryRepository(
    private val files: DocumentFileStore,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    },
    /** Rebuildable search index; list reconciles it with committed files even when non-empty. */
    private val dao: com.donnclab.kinetiframe.data.db.LibraryItemDao? = null,
) {
    private val packCommits = java.util.concurrent.ConcurrentHashMap<String, PackCommit>()
    fun organizationStore() =
        LibraryOrganizationStore(File(files.libraryRoot().parentFile, "library-organisation.json"))
    private fun rootDirectory(scope: LibraryScope, projectId: String?): File = when (scope) {
        LibraryScope.PERSONAL -> files.libraryRoot()

        LibraryScope.PROJECT -> files.projectVaultRoot(
            requireNotNull(projectId) {
                "A project-scoped item needs a project id"
            },
        )
    }

    private fun itemDirectory(scope: LibraryScope, projectId: String?, itemId: String): File = when (scope) {
        LibraryScope.PERSONAL -> files.libraryDirectory(itemId)

        LibraryScope.PROJECT -> files.projectVaultDirectory(
            requireNotNull(projectId) {
                "A project-scoped item needs a project id"
            },
            itemId,
        )
    }

    fun list(
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
    ): StoreResult<List<LibraryItemManifest>> = runCatching {
        val queryProjectId = if (scope == LibraryScope.PROJECT) projectId else null
        val cached = dao?.let { d -> runCatching { d.list(scope.name, queryProjectId) }.getOrNull() }
        val fromDisk = rootDirectory(
            scope,
            projectId,
        ).listFiles().orEmpty().filter(File::isDirectory).mapNotNull { directory ->
            runCatching {
                (
                    readPackCommit(directory, refresh = true)?.manifest
                        ?: files.readOrNull(File(directory, MANIFEST))?.let {
                            json.decodeFromString<LibraryItemManifest>(it).validate()
                        }
                    )
                    ?.takeIf { it.libraryItemId == directory.name }
            }.getOrNull()
        }.sortedByDescending { it.createdAtEpochMs }
        dao?.let { d ->
            cached.orEmpty().filter { row -> fromDisk.none { it.libraryItemId == row.libraryItemId } }
                .forEach { runCatching { d.deleteById(it.libraryItemId) } }
            fromDisk.forEach { manifest -> runCatching { d.upsert(toEntity(manifest)) } }
        }
        fromDisk
    }.fold(::value, ::failure)

    fun delete(
        itemId: String,
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
    ): StoreResult<Result<Unit>?> = runCatching {
        val directory = itemDirectory(scope, projectId, itemId)
        require(directory.isDirectory) { "Library item not found" }
        require(directory.deleteRecursively()) { "Could not delete this library item" }
        dao?.let { d -> runCatching { d.deleteById(itemId) } }
    }.fold(::value, ::failure)

    /**
     * Copies [sourceFile] into the library as a new self-contained item -- self-contained meaning
     * the library keeps its own copy, so the item survives the source file later moving, being
     * overwritten, or deleted. Only valid for [LibraryItemManifest.RAW_ASSET_KINDS]; ACTOR/
     * PERFORMANCE/BUNDLE go through [com.donnclab.kinetiframe.actors.data.ActorBundleRepository] instead
     * (structured multi-file payloads, not one raw file), and PACK goes through [exportPack].
     */
    fun exportAsset(
        kind: LibraryItemKind,
        itemId: String,
        displayName: String,
        sourceFile: File,
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
        tags: Set<String> = emptySet(),
        nowEpochMs: Long,
    ): StoreResult<LibraryItemManifest> = runCatching {
        require(kind in LibraryItemManifest.RAW_ASSET_KINDS) { "$kind is not a raw-asset library kind" }
        require(sourceFile.isFile) { "Source file not found" }
        val payloadFileName = "asset.${sourceFile.extension.ifBlank { "bin" }}"
        val manifest = LibraryItemManifest(
            libraryItemId = itemId,
            kind = kind,
            displayName = displayName,
            sourceProjectId = projectId,
            scope = scope,
            payloadFileName = payloadFileName,
            tags = tags,
            createdAtEpochMs = nowEpochMs,
        ).validate()
        val directory = itemDirectory(scope, projectId, itemId)
        val payloadFile = File(directory, "payload/$payloadFileName")
        require(!File(directory, MANIFEST).exists()) { "Library item ID already exists" }
        com.donnclab.kinetiframe.data.storage.DurableFile.copy(sourceFile, payloadFile)
        files.writeAtomically(File(directory, MANIFEST), json.encodeToString(manifest))
        dao?.let { d -> runCatching { d.upsert(toEntity(manifest)) } }
        manifest
    }.fold(::value, ::failure)

    /** Read-only preview access. The validated manifest supplies a contained payload path. */
    fun assetFile(item: LibraryItemManifest): StoreResult<File> = runCatching {
        item.validate()
        require(item.kind in LibraryItemManifest.RAW_ASSET_KINDS) { "Not a raw asset" }
        val directory = itemDirectory(item.scope, item.sourceProjectId, item.libraryItemId)
        val source = com.donnclab.kinetiframe.data.storage.ContainedPaths.resolveWithin(
            directory,
            "payload/${requireNotNull(item.payloadFileName)}",
            "Library payload",
        )
        require(source.isFile) { "Library item payload is missing" }
        source
    }.fold(::value, ::failure)

    /**
     * Copies a raw-asset library item's payload out to [destination]. This repository never
     * interprets the file or places it on a timeline. Each track's own import logic (audio,
     * backgrounds, `.kframe` projects) does that from the returned copy, as
     * [com.donnclab.kinetiframe.actors.data.ActorBundleRepository] does for actors and performances.
     */
    fun copyAssetTo(
        itemId: String,
        destination: File,
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
    ): StoreResult<Pair<LibraryItemManifest, File>> = runCatching {
        val directory = itemDirectory(scope, projectId, itemId)
        val manifest = json.decodeFromString<LibraryItemManifest>(
            requireNotNull(files.readOrNull(File(directory, MANIFEST))) { "Library item not found" },
        ).validate()
        val payloadFileName = requireNotNull(manifest.payloadFileName) { "This library item has no raw asset payload" }
        val source = File(directory, "payload/$payloadFileName")
        require(source.isFile) { "Library item payload is missing" }
        destination.parentFile?.mkdirs()
        source.copyTo(destination, overwrite = true)
        manifest to destination
    }.fold(::value, ::failure)

    /**
     * Writes a PACK item: [packManifest] (validated) becomes `payload/meta.json`, and every entry
     * in [frameFiles] (keyed by the exact file name [packManifest] references, e.g.
     * "frame_001.png") is copied into `payload/` under that same name -- so this item's payload
     * directory is, byte for byte, what [PackArchiver.export] later zips into a `.kpack`.
     */
    fun exportPack(
        itemId: String,
        displayName: String,
        packManifest: PackManifest,
        frameFiles: Map<String, File>,
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
        tags: Set<String> = emptySet(),
        nowEpochMs: Long,
    ): StoreResult<LibraryItemManifest> = runCatching {
        val validPack = packManifest.validate()
        val expectedFiles = validPack.items.map { it.file }.toSet()
        require(frameFiles.keys == expectedFiles) { "Pack frame files do not match its own item list" }
        frameFiles.values.forEach { require(it.isFile) { "A pack frame file is missing" } }
        val manifest = LibraryItemManifest(
            libraryItemId = itemId,
            kind = LibraryItemKind.PACK,
            displayName = displayName,
            sourceProjectId = projectId,
            scope = scope,
            tags = tags,
            createdAtEpochMs = nowEpochMs,
        ).validate()
        val directory = itemDirectory(scope, projectId, itemId)
        val previous = files.readOrNull(File(directory, MANIFEST))?.let {
            json.decodeFromString<LibraryItemManifest>(it).validate()
        }
        require(previous == null || previous.kind == LibraryItemKind.PACK) {
            "Library item ID belongs to another resource"
        }
        val payload = if (previous == null) "payload" else "pack-versions/${java.util.UUID.randomUUID()}"
        files.writeAtomically(File(directory, "$payload/$PACK_META"), json.encodeToString(validPack))
        frameFiles.forEach { (fileName, sourceFile) ->
            val target = File(directory, "$payload/$fileName")
            com.donnclab.kinetiframe.data.storage.DurableFile.copy(sourceFile, target)
        }
        files.writeAtomically(File(directory, MANIFEST), json.encodeToString(manifest))
        files.writeAtomically(
            File(directory, PACK_COMMIT),
            json.encodeToString(PackCommit(manifest, validPack, payload)),
        )
        packCommits[directory.absolutePath] = PackCommit(manifest, validPack, payload)
        dao?.let { d -> runCatching { d.upsert(toEntity(manifest)) } }
        manifest
    }.fold(::value, ::failure)

    /** Reads a PACK item's own index without copying any of its drawings -- used to populate the
     *  "which item in this pack do you want to stamp" drill-down grid before [copyPackItemTo]. */
    fun loadPack(
        itemId: String,
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
    ): StoreResult<Pair<LibraryItemManifest, PackManifest>> = runCatching {
        val directory = itemDirectory(scope, projectId, itemId)
        readPackCommit(directory, refresh = true)?.let { committed ->
            committed.manifest.validate()
            committed.pack.validate()
            require(committed.manifest.libraryItemId == itemId && committed.manifest.kind == LibraryItemKind.PACK)
            val payload = com.donnclab.kinetiframe.data.storage.ContainedPaths.resolveWithin(
                directory,
                committed.payloadDirectory,
                "Pack payload",
            )
            committed.pack.items.forEach {
                require(File(payload, it.file).isFile) { "Pack drawing is missing: ${it.file}" }
            }
            return@runCatching committed.manifest to committed.pack
        }
        val manifest = json.decodeFromString<LibraryItemManifest>(
            requireNotNull(files.readOrNull(File(directory, MANIFEST))) { "Library item not found" },
        ).validate()
        require(manifest.kind == LibraryItemKind.PACK) { "This library item is not a pack" }
        val pack = json.decodeFromString<PackManifest>(
            requireNotNull(files.readOrNull(File(directory, "payload/$PACK_META"))) { "Pack index is missing" },
        ).validate()
        pack.items.forEach {
            require(File(directory, "payload/${it.file}").isFile) { "Pack drawing is missing: ${it.file}" }
        }
        manifest to pack
    }.fold(::value, ::failure)

    /** Copies one specific drawing out of a pack (identified by its `file` name from
     *  [PackItemEntry]) to [destination] -- the actual "stamp" copy the caller pastes onto a
     *  frame's raster layer. */
    fun copyPackItemTo(
        itemId: String,
        fileName: String,
        destination: File,
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
    ): StoreResult<File> = runCatching {
        val directory = itemDirectory(scope, projectId, itemId)
        val pack = when (val loaded = loadPack(itemId, scope, projectId)) {
            is StoreResult.Value -> loaded.value.second
            is StoreResult.Failure -> throw IllegalStateException(loaded.message, loaded.cause)
        }
        require(pack.items.any { it.file == fileName }) { "Drawing is not a member of this pack" }
        val source = com.donnclab.kinetiframe.data.storage.ContainedPaths.resolveWithin(
            packPayloadDirectory(itemId, scope, projectId),
            fileName,
            "Pack drawing",
        )
        require(source.isFile) { "That pack drawing is missing" }
        destination.parentFile?.mkdirs()
        source.copyTo(destination, overwrite = true)
        destination
    }.fold(::value, ::failure)

    /** The on-disk file for one pack drawing, for read-only display (a Home Packs-tab card's
     *  collage thumbnail) -- unlike [copyPackItemTo], this never copies anything out; the caller
     *  just reads it in place (e.g. via Coil's AsyncImage). */
    fun packItemFile(
        itemId: String,
        fileName: String,
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
    ): File = com.donnclab.kinetiframe.data.storage.ContainedPaths.resolveWithin(
        packPayloadDirectory(itemId, scope, projectId),
        fileName,
        "Pack drawing",
    )

    fun packPayloadDirectory(
        itemId: String,
        scope: LibraryScope = LibraryScope.PERSONAL,
        projectId: String? = null,
    ): File {
        val directory = itemDirectory(scope, projectId, itemId)
        return com.donnclab.kinetiframe.data.storage.ContainedPaths.resolveWithin(
            directory,
            readPackCommit(directory)?.payloadDirectory ?: "payload",
            "Pack payload",
        )
    }

    private fun readPackCommit(directory: File, refresh: Boolean = false): PackCommit? {
        if (!refresh) packCommits[directory.absolutePath]?.let { return it }
        return files.readOrNull(File(directory, PACK_COMMIT))?.let {
            json.decodeFromString<PackCommit>(it).also { commit -> packCommits[directory.absolutePath] = commit }
        }
    }

    private fun toEntity(manifest: LibraryItemManifest) = com.donnclab.kinetiframe.data.db.LibraryItemEntity(
        libraryItemId = manifest.libraryItemId, kind = manifest.kind.name, displayName = manifest.displayName,
        sourceActorId = manifest.sourceActorId, sourcePerformanceId = manifest.sourcePerformanceId,
        sourceProjectId = manifest.sourceProjectId,
        scope = manifest.scope.name,
        payloadFileName = manifest.payloadFileName,
        tags = manifest.tags.joinToString(","), license = manifest.license, attribution = manifest.attribution,
        createdAtEpochMs = manifest.createdAtEpochMs,
    )

    private fun toManifest(entity: com.donnclab.kinetiframe.data.db.LibraryItemEntity) = LibraryItemManifest(
        libraryItemId = entity.libraryItemId,
        kind = LibraryItemKind.valueOf(
            entity.kind,
        ),
        displayName = entity.displayName,
        sourceActorId = entity.sourceActorId, sourcePerformanceId = entity.sourcePerformanceId,
        sourceProjectId = entity.sourceProjectId,
        scope = LibraryScope.valueOf(
            entity.scope,
        ),
        payloadFileName = entity.payloadFileName,
        tags = entity.tags.split(
            ",",
        ).filter(String::isNotBlank).toSet(),
        license = entity.license, attribution = entity.attribution,
        createdAtEpochMs = entity.createdAtEpochMs,
    )

    private fun <T> value(value: T): StoreResult<T> = StoreResult.Value(value)
    private fun failure(error: Throwable): StoreResult.Failure =
        StoreResult.Failure(error.message ?: "Library operation failed", error)
    private companion object {
        const val MANIFEST = "manifest.json"
        const val PACK_META = "meta.json"
        const val PACK_COMMIT = "pack-commit.json"
    }
}

@Serializable
private data class PackCommit(val manifest: LibraryItemManifest, val pack: PackManifest, val payloadDirectory: String)
