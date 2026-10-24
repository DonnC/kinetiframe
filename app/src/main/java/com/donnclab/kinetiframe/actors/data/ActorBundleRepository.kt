package com.donnclab.kinetiframe.actors.data

import com.donnclab.kinetiframe.core.actor.document.ActorDocumentValidation
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.ActorRevision
import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.actor.document.validate
import com.donnclab.kinetiframe.data.storage.ContainedPaths
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Local library payloads are validated documents, not executable archives or filename-based imports. */
class ActorBundleRepository(
    private val files: DocumentFileStore,
    private val actors: ActorRepository,
    private val performances: PerformanceRepository,
    private val json: Json = Json {
        ignoreUnknownKeys = false
        prettyPrint = true
    },
    /** Best-effort index write for the ACTOR/PERFORMANCE library items this class creates, same
     *  contract as [com.donnclab.kinetiframe.library.data.LibraryRepository]'s own [dao] -- a listing that
     *  hasn't seen this write yet just self-heals from disk next time it comes back empty. */
    private val libraryDao: com.donnclab.kinetiframe.actors.data.db.LibraryItemDao? = null,
) {
    fun exportActor(
        actorId: String,
        itemId: String,
        name: String,
        nowEpochMs: Long,
    ): StoreResult<LibraryItemManifest> = runCatching {
        val (actor, revision) = requireActor(actorId)
        val manifest = LibraryItemManifest(
            libraryItemId = itemId,
            kind = LibraryItemKind.ACTOR,
            displayName = name,
            sourceActorId = actor.actorId,
            createdAtEpochMs = nowEpochMs,
        ).validate()
        val directory = files.libraryDirectory(itemId)
        require(!File(directory, "manifest.json").exists()) { "Library item ID already exists" }
        files.writeAtomically(File(directory, "payload/actor.json"), json.encodeToString(actor))
        val portableArtwork = exportArtwork(actor.actorId, revision, directory)
        files.writeAtomically(
            File(directory, "payload/revision.json"),
            json.encodeToString(revision.copy(artworkAssetPaths = portableArtwork)),
        )
        val views = actor.authoredViews().values.map { revisionId ->
            val snapshot = when (val loaded = actors.loadRevision(actorId, revisionId)) {
                is StoreResult.Value -> loaded.value
                is StoreResult.Failure -> error(loaded.message)
            }
            snapshot.copy(artworkAssetPaths = exportArtwork(actorId, snapshot, directory))
        }
        files.writeAtomically(File(directory, "payload/views.json"), json.encodeToString(views))
        files.writeAtomically(File(directory, "manifest.json"), json.encodeToString(manifest))
        indexInDao(manifest)
        manifest
    }.fold(::value, ::failure)

    fun exportPerformance(
        projectId: String,
        performanceId: String,
        itemId: String,
        name: String,
        nowEpochMs: Long,
    ): StoreResult<LibraryItemManifest> = runCatching {
        val performance = requirePerformance(projectId, performanceId)
        val manifest = LibraryItemManifest(
            libraryItemId = itemId,
            kind = LibraryItemKind.PERFORMANCE,
            displayName = name,
            sourceActorId = performance.actorId,
            sourcePerformanceId = performance.performanceId,
            createdAtEpochMs = nowEpochMs,
        ).validate()
        val directory = files.libraryDirectory(itemId)
        require(!File(directory, "manifest.json").exists()) { "Library item ID already exists" }
        files.writeAtomically(File(directory, "payload/performance.json"), json.encodeToString(performance))
        files.writeAtomically(File(directory, "manifest.json"), json.encodeToString(manifest))
        indexInDao(manifest)
        manifest
    }.fold(::value, ::failure)

    /** Imports actor assets with fresh local identity so an untrusted bundle cannot overwrite a user's actor. */
    fun importActor(
        itemId: String,
        newActorId: String,
        newRevisionId: String,
        nowEpochMs: Long,
    ): StoreResult<ActorManifest> = runCatching {
        val directory = files.libraryDirectory(itemId)
        val library = json.decodeFromString<LibraryItemManifest>(
            requireNotNull(files.readOrNull(File(directory, "manifest.json"))) {
                "Library item not found"
            },
        ).validate()
        require(library.kind == LibraryItemKind.ACTOR || library.kind == LibraryItemKind.BUNDLE) {
            "Library item is not an actor"
        }
        val sourceActor = json.decodeFromString<ActorManifest>(
            requireNotNull(files.readOrNull(File(directory, "payload/actor.json"))) {
                "Actor payload missing"
            },
        ).validate()
        val sourceRevision = json.decodeFromString<ActorRevision>(
            requireNotNull(files.readOrNull(File(directory, "payload/revision.json"))) {
                "Actor revision payload missing"
            },
        ).validate()
        require(!files.actorDirectory(newActorId).exists()) { "An actor with this import ID already exists" }
        val sourceViews = files.readOrNull(File(directory, "payload/views.json"))?.let {
            json.decodeFromString<List<ActorRevision>>(it)
        } ?: listOf(sourceRevision)
        require(
            sourceViews.size == sourceActor.authoredViews().size &&
                sourceViews.map {
                    it.revisionId
                }.toSet() == sourceActor.authoredViews().values.toSet(),
        ) { "Incomplete actor view payload" }
        require(
            sourceViews.firstOrNull {
                it.revisionId == sourceRevision.revisionId
            } == sourceRevision,
        ) { "Conflicting actor view payload" }
        sourceViews.forEach { source ->
            source.validate()
            com.donnclab.kinetiframe.actors.domain.ActorTemplateContract.validate(
                sourceActor.templateId,
                source.sceneGraph,
            )
            require(
                source.actorId == sourceActor.actorId && sourceActor.authoredViews()[source.view] == source.revisionId,
            ) {
                "Actor view identity mismatch"
            }
            source.artworkAssetPaths.values.forEach { path ->
                require(safeFile(directory, path).isFile) { "Bundle artwork is missing: $path" }
            }
            val targetId = if (source.revisionId ==
                sourceActor.currentRevisionId
            ) {
                newRevisionId
            } else {
                "$newRevisionId-${source.view.name.lowercase()}"
            }
            ActorDocumentValidation.requireId(targetId, "imported view revision ID")
        }
        val revisions = sourceViews.map { source ->
            require(
                source.actorId == sourceActor.actorId && sourceActor.authoredViews()[source.view] == source.revisionId,
            ) {
                "Actor view identity mismatch"
            }
            val targetId = if (source.revisionId ==
                sourceActor.currentRevisionId
            ) {
                newRevisionId
            } else {
                "$newRevisionId-${source.view.name.lowercase()}"
            }
            source.copy(
                actorId = newActorId,
                revisionId = targetId,
                artworkAssetPaths = importArtwork(newActorId, targetId, source.artworkAssetPaths, directory),
                createdAtEpochMs = nowEpochMs,
            ).validate()
        }
        val actor = sourceActor.copy(
            actorId = newActorId,
            currentRevisionId = newRevisionId,
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs,
            viewRevisions = revisions.associate { it.view to it.revisionId },
        ).validate()
        requireSuccess(actors.saveViews(actor, revisions))
        actor
    }.fold(::value, ::failure)

    /**
     * Imports a mathematical performance into a chosen project without changing its pinned actor
     * revision. A performance bundle is therefore safe to accept only when that exact revision
     * already exists locally; callers can offer actor import first when it does not.
     */
    fun importPerformance(
        itemId: String,
        targetProjectId: String,
        newPerformanceId: String,
        nowEpochMs: Long,
    ): StoreResult<PerformanceDocument> = runCatching {
        val directory = files.libraryDirectory(itemId)
        val library = json.decodeFromString<LibraryItemManifest>(
            requireNotNull(files.readOrNull(File(directory, "manifest.json"))) { "Library item not found" },
        ).validate()
        require(library.kind == LibraryItemKind.PERFORMANCE || library.kind == LibraryItemKind.BUNDLE) {
            "Library item is not a performance"
        }
        val source = json.decodeFromString<PerformanceDocument>(
            requireNotNull(files.readOrNull(File(directory, "payload/performance.json"))) {
                "Performance payload missing"
            },
        ).validate()
        val revision = when (val loaded = actors.loadRevision(source.actorId, source.actorRevisionId)) {
            is StoreResult.Value -> loaded.value

            is StoreResult.Failure -> throw IllegalStateException(
                "Import the matching actor revision before importing this performance",
                loaded.cause,
            )
        }
        val imported = source.copy(
            performanceId = newPerformanceId,
            projectId = targetProjectId,
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs,
        ).validate()
        com.donnclab.kinetiframe.actors.domain.PerformanceCompatibility.validate(imported, revision)
        requireSuccess(performances.save(imported))
        imported
    }.fold(::value, ::failure)

    private fun requireActor(actorId: String): Pair<ActorManifest, ActorRevision> =
        when (val value = actors.load(actorId)) {
            is StoreResult.Value -> value.value
            is StoreResult.Failure -> throw IllegalStateException(value.message, value.cause)
        }
    private fun requirePerformance(projectId: String, performanceId: String): PerformanceDocument =
        when (val value = performances.load(projectId, performanceId)) {
            is StoreResult.Value -> value.value
            is StoreResult.Failure -> throw IllegalStateException(value.message, value.cause)
        }
    private fun requireSuccess(value: StoreResult<*>) {
        if (value is StoreResult.Failure) throw IllegalStateException(value.message, value.cause)
    }

    /** Makes library payloads self-contained: revisions never retain source-actor file paths. */
    private fun exportArtwork(actorId: String, revision: ActorRevision, libraryDirectory: File): Map<String, String> =
        revision.artworkAssetPaths.mapNotNull { (partId, path) ->
            val source = safeFile(files.actorDirectory(actorId), path)
            require(source.isFile) { "Actor artwork is missing: $path" }
            val targetRelative = "payload/artwork/${revision.revisionId}/${source.name}"
            val target = safeFile(libraryDirectory, targetRelative)
            target.parentFile?.mkdirs()
            com.donnclab.kinetiframe.data.storage.DurableFile.copy(source, target)
            partId to targetRelative
        }.toMap()

    private fun importArtwork(
        actorId: String,
        revisionId: String,
        artwork: Map<String, String>,
        libraryDirectory: File,
    ): Map<String, String> = artwork.mapNotNull { (partId, path) ->
        val source = safeFile(libraryDirectory, path)
        require(source.isFile) { "Bundle artwork is missing: $path" }
        val targetRelative = "revisions/$revisionId/artwork/${source.name}"
        val target = safeFile(files.actorDirectory(actorId), targetRelative)
        target.parentFile?.mkdirs()
        com.donnclab.kinetiframe.data.storage.DurableFile.copy(source, target)
        partId to targetRelative
    }.toMap()

    private fun safeFile(root: File, relativePath: String): File =
        ContainedPaths.resolveWithin(root, relativePath, "Bundle artwork")

    private fun indexInDao(manifest: LibraryItemManifest) {
        val d = libraryDao ?: return
        runCatching {
            d.upsert(
                com.donnclab.kinetiframe.actors.data.db.LibraryItemEntity(
                    libraryItemId = manifest.libraryItemId,
                    kind = manifest.kind.name,
                    displayName = manifest.displayName,
                    sourceActorId = manifest.sourceActorId, sourcePerformanceId = manifest.sourcePerformanceId,
                    sourceProjectId = manifest.sourceProjectId,
                    scope = manifest.scope.name,
                    payloadFileName = manifest.payloadFileName,
                    tags = manifest.tags.joinToString(
                        ",",
                    ),
                    license = manifest.license, attribution = manifest.attribution,
                    createdAtEpochMs = manifest.createdAtEpochMs,
                ),
            )
        }
    }

    private fun <T> value(value: T): StoreResult<T> = StoreResult.Value(value)
    private fun failure(error: Throwable): StoreResult.Failure =
        StoreResult.Failure(error.message ?: "Library operation failed", error)
}
