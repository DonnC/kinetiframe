package com.donnclab.kinetiframe.actors.data

import com.donnclab.kinetiframe.actors.domain.ActorTemplateContract
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
    private val files: ActorFileStore,
    private val json: Json = Json {
        ignoreUnknownKeys = false
        prettyPrint = true
    },
    private val dao: com.donnclab.kinetiframe.actors.data.db.ActorDao? = null,
) {
    /** Read-only character hierarchy: motion remains owned by its original project/revision. */
    fun relatedPerformances(): List<PerformanceDocument> {
        val performances = PerformanceRepository(files)
        return files.projectsRoot().listFiles().orEmpty().filter(File::isDirectory).flatMap { directory ->
            when (val result = performances.list(directory.name)) {
                is ActorStoreResult.Value -> result.value
                is ActorStoreResult.Failure -> emptyList()
            }
        }
    }
    fun list(): ActorStoreResult<List<ActorManifest>> = runCatching {
        listWithThumbnailsValue().map { it.first }
    }.fold(::value, ::failure)

    /** Validated disk listing paired with available revision-owned dashboard artwork. */
    fun listWithThumbnails(): ActorStoreResult<List<Pair<ActorManifest, String?>>> = runCatching {
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

    fun isDisplayNameAvailable(displayName: String, excludingActorId: String? = null): ActorStoreResult<Boolean> =
        runCatching {
            val normalized = displayName.trim().lowercase()
            require(normalized.isNotBlank()) { "Actor name cannot be blank" }
            when (val result = list()) {
                is ActorStoreResult.Value -> result.value.none {
                    it.actorId != excludingActorId &&
                        it.displayName.trim().lowercase() == normalized
                }

                is ActorStoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
            }
        }.fold(::value, ::failure)

    fun save(manifest: ActorManifest, revision: ActorRevision): ActorStoreResult<ActorManifest> = runCatching {
        require(manifest.currentRevisionId == revision.revisionId) { "Manifest must point to saved revision" }
        saveViewsValue(manifest, listOf(revision))
        manifest
    }.fold(::value, ::failure)

    /** Commits every immutable view snapshot before publishing the catalog once. */
    fun saveViews(manifest: ActorManifest, revisions: List<ActorRevision>): ActorStoreResult<ActorManifest> =
        runCatching {
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
                    is ActorStoreResult.Value -> loaded.value
                    is ActorStoreResult.Failure -> error(loaded.message)
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

    fun load(actorId: String): ActorStoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
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
                    is ActorStoreResult.Value -> loaded.value
                    is ActorStoreResult.Failure -> error(loaded.message)
                }
            }
            require(snapshot.view == view) { "Actor view revision mismatch" }
            ActorTemplateContract.validate(manifest.templateId, snapshot.sceneGraph)
        }
        manifest to revision
    }.fold(::value, ::failure)

    fun loadRevision(actorId: String, revisionId: String): ActorStoreResult<ActorRevision> = runCatching {
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
    fun characterPreview(actorId: String): ActorStoreResult<String?> = runCatching {
        com.donnclab.kinetiframe.actors.render.ActorThumbnailMaker.create(
            files.actorDirectory(actorId),
            loadValue(actorId).second,
        )
    }.fold(::value, ::failure)

    fun thumbnailPath(actorId: String): ActorStoreResult<String?> = runCatching {
        val (_, revision) = loadValue(actorId)
        revision.artworkAssetPaths.toSortedMap().values
            .asSequence()
            .map { path -> safeArtworkFile(files.actorDirectory(actorId), path) }
            .firstOrNull(File::isFile)
            ?.absolutePath
    }.fold(::value, ::failure)

    fun rename(actorId: String, displayName: String, nowEpochMs: Long): ActorStoreResult<ActorManifest> = runCatching {
        val (manifest, revision) = loadValue(actorId)
        val renamed = manifest.copy(displayName = displayName.trim(), updatedAtEpochMs = nowEpochMs).validate()
        saveValue(renamed, revision)
        renamed
    }.fold(::value, ::failure)

    fun saveRevision(
        actorId: String,
        sceneGraph: com.donnclab.kinetiframe.actors.rig.model.SceneGraph,
        newRevisionId: String,
        nowEpochMs: Long,
        artworkAssetPaths: Map<String, String>? = null,
        view: com.donnclab.kinetiframe.actors.rig.model.ActorView? = null,
    ): ActorStoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
        val (manifest, selected) = loadValue(actorId)
        val current = if (view == null || selected.view == view) {
            selected
        } else {
            val revisionId = requireNotNull(manifest.authoredViews()[view]) { "View is not authored" }
            when (val loaded = loadRevision(actorId, revisionId)) {
                is ActorStoreResult.Value -> loaded.value
                is ActorStoreResult.Failure -> error(loaded.message)
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
        view: com.donnclab.kinetiframe.actors.rig.model.ActorView,
        nowEpochMs: Long,
    ): ActorStoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
        val (manifest, _) = loadValue(actorId)
        val revisionId = requireNotNull(manifest.authoredViews()[view]) { "View is not authored" }
        val revision = when (val loaded = loadRevision(actorId, revisionId)) {
            is ActorStoreResult.Value -> loaded.value
            is ActorStoreResult.Failure -> error(loaded.message)
        }
        val next = manifest.copy(currentRevisionId = revisionId, updatedAtEpochMs = nowEpochMs)
        saveValue(next, revision)
        next to revision
    }.fold(::value, ::failure)

    /** New views start without artwork. Drawing unseen surfaces is an authoring decision. */
    fun createView(
        actorId: String,
        view: com.donnclab.kinetiframe.actors.rig.model.ActorView,
        revisionId: String,
        nowEpochMs: Long,
    ): ActorStoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
        val (manifest, _) = loadValue(actorId)
        require(
            view != com.donnclab.kinetiframe.actors.rig.model.ActorView.ORIGINAL && view !in manifest.authoredViews(),
        ) {
            "View already exists"
        }
        val template = when (manifest.templateId) {
            ActorManifest.HUMAN_TEMPLATE_ID -> com.donnclab.kinetiframe.actors.rig.model.ActorTemplate.HUMAN
            ActorManifest.ANIMAL_TEMPLATE_ID -> com.donnclab.kinetiframe.actors.rig.model.ActorTemplate.ANIMAL
            else -> com.donnclab.kinetiframe.actors.rig.model.ActorTemplate.CUSTOM
        }
        val revision = ActorRevision(
            revisionId = revisionId,
            actorId = actorId,
            view = view,
            sceneGraph = com.donnclab.kinetiframe.actors.rig.model.ActorTemplates.create(
                template,
                com.donnclab.kinetiframe.project.model.StageDimensions.LandscapeHd,
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
    ): ActorStoreResult<ActorManifest> = runCatching {
        require(!files.actorDirectory(duplicateActorId).exists()) { "Actor duplicate ID already exists" }
        val (sourceManifest, source) = loadValue(sourceActorId)
        val copies = sourceManifest.authoredViews().map { (view, revisionId) ->
            val original = when (val loaded = loadRevision(sourceActorId, revisionId)) {
                is ActorStoreResult.Value -> loaded.value
                is ActorStoreResult.Failure -> error(loaded.message)
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
    fun archive(actorId: String, isReferenced: (String) -> Boolean): ActorStoreResult<Result<Unit>?> = runCatching {
        ActorDocumentValidation.requireId(actorId, "actor ID")
        require(!isReferenced(actorId)) { "Actor is used by a project or library item" }
        val source = files.actorDirectory(actorId)
        require(source.isDirectory) { "Actor not found" }
        files.move(source, files.archiveDirectory(actorId))
        dao?.let { d -> runCatching { d.deleteById(actorId) } }
    }.fold(::value, ::failure)

    private fun loadValue(actorId: String): Pair<ActorManifest, ActorRevision> = when (val result = load(actorId)) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
    }

    private fun saveValue(manifest: ActorManifest, revision: ActorRevision) {
        when (val result = save(manifest, revision)) {
            is ActorStoreResult.Value -> Unit
            is ActorStoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
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
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> null
    }

    private fun toEntity(manifest: ActorManifest, thumbnail: String?) =
        com.donnclab.kinetiframe.actors.data.db.ActorEntity(
            actorId = manifest.actorId,
            displayName = manifest.displayName,
            templateId = manifest.templateId,
            currentRevisionId = manifest.currentRevisionId,
            createdAtEpochMs = manifest.createdAtEpochMs,
            updatedAtEpochMs = manifest.updatedAtEpochMs,
            thumbnailPath = thumbnail,
        )

    private fun toManifest(entity: com.donnclab.kinetiframe.actors.data.db.ActorEntity) = ActorManifest(
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
        ActorPathPolicy.resolveWithin(root, relativePath, "Artwork")

    private fun <T> value(value: T): ActorStoreResult<T> = ActorStoreResult.Value(value)
    private fun failure(error: Throwable): ActorStoreResult.Failure =
        ActorStoreResult.Failure(error.message ?: "Actor storage failed", error)
    private companion object {
        const val MANIFEST = "manifest.json"
    }
}
