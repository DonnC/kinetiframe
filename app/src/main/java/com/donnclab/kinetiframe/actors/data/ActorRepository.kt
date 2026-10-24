package com.donnclab.kinetiframe.actors.data

import com.donnclab.kinetiframe.actors.data.db.ActorDao
import com.donnclab.kinetiframe.actors.data.db.ActorEntity
import com.donnclab.kinetiframe.actors.domain.ActorTemplateContract
import com.donnclab.kinetiframe.actors.render.ActorThumbnailMaker
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.core.actor.document.ActorDocumentValidation
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.ActorRevision
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.core.actor.document.validate
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.storage.ContainedPaths
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * Repository boundaries keep raw files and parse failures outside Compose and ViewModels.
 *
 * Files are authoritative. Every listing reconciles the optional Room index, because a partially
 * indexed library is still non-empty and must not conceal successfully committed resources.
 */
class ActorRepository(
    private val files: DocumentFileStore,
    private val json: Json = Json {
        ignoreUnknownKeys = false
        prettyPrint = true
    },
    private val dao: ActorDao? = null,
) {
    /** Read-only character hierarchy: motion remains owned by its original project/revision. */
    fun relatedPerformances(): List<PerformanceDocument> {
        val performances = PerformanceRepository(files)
        return files.projectsRoot().listFiles().orEmpty().filter(File::isDirectory).flatMap { directory ->
            when (val result = performances.list(directory.name)) {
                is StoreResult.Value -> result.value
                is StoreResult.Failure -> emptyList()
            }
        }
    }
    fun list(): StoreResult<List<ActorManifest>> = runCatching {
        listWithThumbnailsValue().map { it.first }
    }.fold(::value, ::failure)

    /** Validated disk listing paired with available revision-owned dashboard artwork. */
    fun listWithThumbnails(): StoreResult<List<Pair<ActorManifest, String?>>> = runCatching {
        listWithThumbnailsValue()
    }.fold(::value, ::failure)

    private fun listWithThumbnailsValue(): List<Pair<ActorManifest, String?>> {
        val cached = dao?.let { d -> runCatching { d.getAll() }.getOrNull() }
        val root = files.actorsRoot()
        val fromDisk = root.listFiles().orEmpty().filter(File::isDirectory).mapNotNull { directory ->
            runCatching { loadValue(directory.name).first }.getOrNull()
        }.sortedBy { it.displayName.lowercase() }
        val withThumbnails = fromDisk.map { it to thumbnailPathFor(it.actorId) }
        dao?.let { d ->
            cached.orEmpty().filter { row -> fromDisk.none { it.actorId == row.actorId } }
                .forEach { runCatching { d.deleteById(it.actorId) } }
        }
        dao?.let { d ->
            withThumbnails.forEach { (manifest, thumbnail) -> runCatching { d.upsert(toEntity(manifest, thumbnail)) } }
        }
        return withThumbnails
    }

    fun isDisplayNameAvailable(displayName: String, excludingActorId: String? = null): StoreResult<Boolean> =
        runCatching {
            val normalized = displayName.trim().lowercase()
            require(normalized.isNotBlank()) { "Actor name cannot be blank" }
            when (val result = list()) {
                is StoreResult.Value -> result.value.none {
                    it.actorId != excludingActorId &&
                        it.displayName.trim().lowercase() == normalized
                }

                is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
            }
        }.fold(::value, ::failure)

    fun save(manifest: ActorManifest, revision: ActorRevision): StoreResult<ActorManifest> = runCatching {
        require(manifest.currentRevisionId == revision.revisionId) { "Manifest must point to saved revision" }
        saveViewsValue(manifest, listOf(revision))
        manifest
    }.fold(::value, ::failure)

    /** Commits every immutable view snapshot before publishing the catalog once. */
    fun saveViews(manifest: ActorManifest, revisions: List<ActorRevision>): StoreResult<ActorManifest> = runCatching {
        saveViewsValue(manifest, revisions)
        manifest
    }.fold(::value, ::failure)

    private fun saveViewsValue(manifest: ActorManifest, revisions: List<ActorRevision>) {
        manifest.validate()
        require(revisions.map { it.revisionId }.distinct().size == revisions.size) { "Duplicate revision" }
        val directory = files.actorDirectory(manifest.actorId)
        val catalog = manifest.authoredViews().map { (view, revisionId) ->
            val revision = revisions.firstOrNull { it.revisionId == revisionId }
                ?: when (val loaded = loadRevision(manifest.actorId, revisionId)) {
                    is StoreResult.Value -> loaded.value
                    is StoreResult.Failure -> error(loaded.message)
                }
            revision.validate()
            ActorTemplateContract.validate(manifest.templateId, revision.sceneGraph)
            require(revision.actorId == manifest.actorId && revision.view == view) { "Actor view revision mismatch" }
            revision
        }
        require(revisions.all { it in catalog }) { "Revision is not in the actor view catalog" }
        revisions.forEach { revision ->
            val revisionFile = File(directory, "revisions/${revision.revisionId}.json")
            require(!revisionFile.exists() || revisionFile.isFile) { "Actor revision destination is not a file" }
            val existing = files.readOrNull(revisionFile)?.let { json.decodeFromString<ActorRevision>(it).validate() }
            require(existing == null || existing == revision) {
                "Actor revision already exists; save changes as a new revision"
            }
        }
        // Publish the pointer last: an interrupted save may leave an unreferenced revision,
        // but must never leave the actor pointing at a revision that was not committed.
        revisions.forEach { revision ->
            val revisionFile = File(directory, "revisions/${revision.revisionId}.json")
            if (!revisionFile.isFile) files.writeAtomically(revisionFile, json.encodeToString(revision))
        }
        files.writeAtomically(File(directory, MANIFEST), json.encodeToString(manifest))
        indexInDao(manifest, catalog.first { it.revisionId == manifest.currentRevisionId })
    }

    fun load(actorId: String): StoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
        ActorDocumentValidation.requireId(actorId, "actor ID")
        val directory = files.actorDirectory(actorId)
        val manifest = json.decodeFromString<ActorManifest>(
            requireNotNull(files.readOrNull(File(directory, MANIFEST))) {
                "Actor not found"
            },
        ).validate()
        val revision = json.decodeFromString<ActorRevision>(
            requireNotNull(files.readOrNull(File(directory, "revisions/${manifest.currentRevisionId}.json"))) {
                "Actor revision not found"
            },
        ).validate()
        require(
            manifest.actorId == actorId && revision.actorId == actorId &&
                revision.revisionId == manifest.currentRevisionId,
        ) { "Actor revision mismatch" }
        manifest.authoredViews().forEach { (view, id) ->
            val snapshot = if (id == revision.revisionId) {
                revision
            } else {
                when (val loaded = loadRevision(actorId, id)) {
                    is StoreResult.Value -> loaded.value
                    is StoreResult.Failure -> error(loaded.message)
                }
            }
            require(snapshot.view == view) { "Actor view revision mismatch" }
            ActorTemplateContract.validate(manifest.templateId, snapshot.sceneGraph)
        }
        manifest to revision
    }.fold(::value, ::failure)

    fun loadRevision(actorId: String, revisionId: String): StoreResult<ActorRevision> = runCatching {
        ActorDocumentValidation.requireId(actorId, "actor ID")
        ActorDocumentValidation.requireId(revisionId, "revision ID")
        val revision = json.decodeFromString<ActorRevision>(
            requireNotNull(files.readOrNull(File(files.actorDirectory(actorId), "revisions/$revisionId.json"))) {
                "Actor revision not found"
            },
        ).validate()
        require(revision.actorId == actorId && revision.revisionId == revisionId) { "Actor revision mismatch" }
        revision
    }.fold(::value, ::failure)

    /** Returns a stable file thumbnail for dashboard-only use; absence is not an error. */
    fun characterPreview(actorId: String): StoreResult<String?> = runCatching {
        ActorThumbnailMaker.create(
            files.actorDirectory(actorId),
            loadValue(actorId).second,
        )
    }.fold(::value, ::failure)

    fun thumbnailPath(actorId: String): StoreResult<String?> = runCatching {
        val (_, revision) = loadValue(actorId)
        revision.artworkAssetPaths.toSortedMap().values
            .asSequence()
            .map { path -> safeArtworkFile(files.actorDirectory(actorId), path) }
            .firstOrNull(File::isFile)
            ?.absolutePath
    }.fold(::value, ::failure)

    fun rename(actorId: String, displayName: String, nowEpochMs: Long): StoreResult<ActorManifest> = runCatching {
        val (manifest, revision) = loadValue(actorId)
        val renamed = manifest.copy(displayName = displayName.trim(), updatedAtEpochMs = nowEpochMs).validate()
        saveValue(renamed, revision)
        renamed
    }.fold(::value, ::failure)

    fun saveRevision(
        actorId: String,
        sceneGraph: SceneGraph,
        newRevisionId: String,
        nowEpochMs: Long,
        artworkAssetPaths: Map<String, String>? = null,
        view: ActorView? = null,
    ): StoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
        val (manifest, selected) = loadValue(actorId)
        val current = if (view == null || selected.view == view) {
            selected
        } else {
            val revisionId = requireNotNull(manifest.authoredViews()[view]) { "View is not authored" }
            when (val loaded = loadRevision(actorId, revisionId)) {
                is StoreResult.Value -> loaded.value
                is StoreResult.Failure -> error(loaded.message)
            }
        }
        ActorDocumentValidation.requireId(newRevisionId, "revision ID")
        val revision = current.copy(
            revisionId = newRevisionId,
            sceneGraph = sceneGraph,
            artworkAssetPaths = artworkAssetPaths ?: current.artworkAssetPaths,
            createdAtEpochMs = nowEpochMs,
        ).validate()
        ActorTemplateContract.validate(manifest.templateId, revision.sceneGraph)
        val nextManifest = manifest.copy(
            currentRevisionId = newRevisionId,
            updatedAtEpochMs = nowEpochMs,
            viewRevisions = manifest.authoredViews() + (current.view to newRevisionId),
        ).validate()
        saveValue(nextManifest, revision)
        nextManifest to revision
    }.fold(::value, ::failure)

    fun selectView(
        actorId: String,
        view: ActorView,
        nowEpochMs: Long,
    ): StoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
        val (manifest, _) = loadValue(actorId)
        val revisionId = requireNotNull(manifest.authoredViews()[view]) { "View is not authored" }
        val revision = when (val loaded = loadRevision(actorId, revisionId)) {
            is StoreResult.Value -> loaded.value
            is StoreResult.Failure -> error(loaded.message)
        }
        val next = manifest.copy(currentRevisionId = revisionId, updatedAtEpochMs = nowEpochMs)
        saveValue(next, revision)
        next to revision
    }.fold(::value, ::failure)

    /** New views start without artwork. Drawing unseen surfaces is an authoring decision. */
    fun createView(
        actorId: String,
        view: ActorView,
        revisionId: String,
        nowEpochMs: Long,
    ): StoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
        val (manifest, _) = loadValue(actorId)
        require(
            view != ActorView.ORIGINAL && view !in manifest.authoredViews(),
        ) {
            "View already exists"
        }
        val template = when (manifest.templateId) {
            ActorManifest.HUMAN_TEMPLATE_ID -> ActorTemplate.HUMAN
            ActorManifest.ANIMAL_TEMPLATE_ID -> ActorTemplate.ANIMAL
            else -> ActorTemplate.CUSTOM
        }
        val revision = ActorRevision(
            revisionId = revisionId,
            actorId = actorId,
            view = view,
            sceneGraph = ActorTemplates.create(
                template,
                StageDimensions.LandscapeHd,
                view,
            ),
            createdAtEpochMs = nowEpochMs,
        )
        val next = manifest.copy(
            currentRevisionId = revisionId,
            updatedAtEpochMs = nowEpochMs,
            viewRevisions = manifest.authoredViews() + (view to revisionId),
        )
        saveValue(next, revision)
        next to revision
    }.fold(::value, ::failure)

    /** Duplicates structural data and its revision-owned artwork under fresh actor identity. */
    fun duplicate(
        sourceActorId: String,
        duplicateActorId: String,
        duplicateRevisionId: String,
        displayName: String,
        nowEpochMs: Long,
    ): StoreResult<ActorManifest> = runCatching {
        require(!files.actorDirectory(duplicateActorId).exists()) { "Actor duplicate ID already exists" }
        val (sourceManifest, source) = loadValue(sourceActorId)
        val copies = sourceManifest.authoredViews().map { (view, revisionId) ->
            val original = when (val loaded = loadRevision(sourceActorId, revisionId)) {
                is StoreResult.Value -> loaded.value
                is StoreResult.Failure -> error(loaded.message)
            }
            val targetId = if (revisionId ==
                source.revisionId
            ) {
                duplicateRevisionId
            } else {
                "$duplicateRevisionId-${view.name.lowercase()}"
            }
            original.copy(
                actorId = duplicateActorId,
                revisionId = targetId,
                artworkAssetPaths = copyRevisionArtwork(
                    sourceActorId,
                    revisionId,
                    duplicateActorId,
                    targetId,
                    original.artworkAssetPaths,
                ),
                createdAtEpochMs = nowEpochMs,
            )
        }
        val manifest = ActorManifest(
            actorId = duplicateActorId,
            displayName = displayName.trim(),
            templateId = sourceManifest.templateId,
            currentRevisionId = duplicateRevisionId,
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs,
            viewRevisions = copies.associate { it.view to it.revisionId },
        ).validate()
        saveViewsValue(manifest, copies)
        manifest
    }.fold(::value, ::failure)

    /** Archive is recoverable and refuses to move a referenced actor. */
    fun archive(actorId: String, isReferenced: (String) -> Boolean): StoreResult<Result<Unit>?> = runCatching {
        ActorDocumentValidation.requireId(actorId, "actor ID")
        require(!isReferenced(actorId)) { "Actor is used by a project or library item" }
        val source = files.actorDirectory(actorId)
        require(source.isDirectory) { "Actor not found" }
        files.move(source, files.archiveDirectory(actorId))
        dao?.let { d -> runCatching { d.deleteById(actorId) } }
    }.fold(::value, ::failure)

    private fun loadValue(actorId: String): Pair<ActorManifest, ActorRevision> = when (val result = load(actorId)) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
    }

    private fun saveValue(manifest: ActorManifest, revision: ActorRevision) {
        when (val result = save(manifest, revision)) {
            is StoreResult.Value -> Unit
            is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
        }
    }

    /** Best-effort index write: a DAO hiccup never blocks or fails the real, disk-backed save --
     *  worst case the index just self-heals empty next [list]. */
    private fun indexInDao(manifest: ActorManifest, revision: ActorRevision) {
        val d = dao ?: return
        runCatching {
            val thumbnail = revision.artworkAssetPaths.toSortedMap().values.asSequence()
                .map { path -> safeArtworkFile(files.actorDirectory(manifest.actorId), path) }
                .firstOrNull(File::isFile)?.absolutePath
            d.upsert(toEntity(manifest, thumbnail))
        }
    }

    private fun thumbnailPathFor(actorId: String): String? = when (val result = thumbnailPath(actorId)) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> null
    }

    private fun toEntity(manifest: ActorManifest, thumbnail: String?) = ActorEntity(
        actorId = manifest.actorId,
        displayName = manifest.displayName,
        templateId = manifest.templateId,
        currentRevisionId = manifest.currentRevisionId,
        createdAtEpochMs = manifest.createdAtEpochMs,
        updatedAtEpochMs = manifest.updatedAtEpochMs,
        thumbnailPath = thumbnail,
    )

    private fun toManifest(entity: ActorEntity) = ActorManifest(
        actorId = entity.actorId,
        displayName = entity.displayName,
        templateId = entity.templateId,
        currentRevisionId = entity.currentRevisionId,
        createdAtEpochMs = entity.createdAtEpochMs,
        updatedAtEpochMs = entity.updatedAtEpochMs,
    )

    private fun copyRevisionArtwork(
        sourceActorId: String,
        sourceRevisionId: String,
        targetActorId: String,
        targetRevisionId: String,
        artwork: Map<String, String>,
    ): Map<String, String> = artwork.mapNotNull { (partId, relativePath) ->
        val source = safeArtworkFile(files.actorDirectory(sourceActorId), relativePath)
        require(source.isFile) { "Actor artwork is missing: $relativePath" }
        val fileName = source.name
        val targetRelative = "revisions/$targetRevisionId/artwork/$fileName"
        val target = safeArtworkFile(files.actorDirectory(targetActorId), targetRelative)
        target.parentFile?.mkdirs()
        Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
        partId to targetRelative
    }.toMap()

    private fun safeArtworkFile(root: File, relativePath: String): File =
        ContainedPaths.resolveWithin(root, relativePath, "Artwork")

    private fun <T> value(value: T): StoreResult<T> = StoreResult.Value(value)
    private fun failure(error: Throwable): StoreResult.Failure =
        StoreResult.Failure(error.message ?: "Actor storage failed", error)
    private companion object {
        const val MANIFEST = "manifest.json"
    }
}
