package com.donnclab.kinetiframe.data.actor

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import com.donnclab.kinetiframe.domain.actor.ActorTemplateContract

sealed interface ActorStoreResult<out T> {
    data class Value<T>(val value: T) : ActorStoreResult<T>
    data class Failure(val message: String, val cause: Throwable? = null) : ActorStoreResult<Nothing>
}

/**
 * Repository boundaries keep raw files and parse failures outside Compose and ViewModels.
 *
 * Files are authoritative. Every listing reconciles the optional Room index, because a partially
 * indexed library is still non-empty and must not conceal successfully committed resources.
 */
class ActorRepository(
    private val files: ActorFileStore,
    private val json: Json = Json { ignoreUnknownKeys = false; prettyPrint = true },
    private val dao: com.donnclab.kinetiframe.data.actor.db.ActorDao? = null
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
        dao?.let { d -> cached.orEmpty().filter { row -> fromDisk.none { it.actorId == row.actorId } }
            .forEach { runCatching { d.deleteById(it.actorId) } } }
        dao?.let { d -> withThumbnails.forEach { (manifest, thumbnail) -> runCatching { d.upsert(toEntity(manifest, thumbnail)) } } }
        return withThumbnails
    }

    fun isDisplayNameAvailable(displayName: String, excludingActorId: String? = null): ActorStoreResult<Boolean> = runCatching {
        val normalized = displayName.trim().lowercase()
        require(normalized.isNotBlank()) { "Actor name cannot be blank" }
        when (val result = list()) {
            is ActorStoreResult.Value -> result.value.none { it.actorId != excludingActorId && it.displayName.trim().lowercase() == normalized }
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
        runCatching { saveViewsValue(manifest, revisions); manifest }.fold(::value, ::failure)

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
            require(existing == null || existing == revision) { "Actor revision already exists; save changes as a new revision" }
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
        val manifest = json.decodeFromString<ActorManifest>(requireNotNull(files.readOrNull(File(directory, MANIFEST))) { "Actor not found" }).validate()
        val revision = json.decodeFromString<ActorRevision>(requireNotNull(files.readOrNull(File(directory, "revisions/${manifest.currentRevisionId}.json"))) { "Actor revision not found" }).validate()
        require(manifest.actorId == actorId && revision.actorId == actorId && revision.revisionId == manifest.currentRevisionId) { "Actor revision mismatch" }
        manifest.authoredViews().forEach { (view, id) ->
            val snapshot = if (id == revision.revisionId) revision else when (val loaded = loadRevision(actorId, id)) {
                is ActorStoreResult.Value -> loaded.value
                is ActorStoreResult.Failure -> error(loaded.message)
            }
            require(snapshot.view == view) { "Actor view revision mismatch" }
            ActorTemplateContract.validate(manifest.templateId, snapshot.sceneGraph)
        }
        manifest to revision
    }.fold(::value, ::failure)

    fun loadRevision(actorId: String, revisionId: String): ActorStoreResult<ActorRevision> = runCatching {
        ActorDocumentValidation.requireId(actorId, "actor ID"); ActorDocumentValidation.requireId(revisionId, "revision ID")
        val revision = json.decodeFromString<ActorRevision>(requireNotNull(files.readOrNull(File(files.actorDirectory(actorId), "revisions/$revisionId.json"))) { "Actor revision not found" }).validate()
        require(revision.actorId == actorId && revision.revisionId == revisionId) { "Actor revision mismatch" }
        revision
    }.fold(::value, ::failure)

    /** Returns a stable file thumbnail for dashboard-only use; absence is not an error. */
    fun characterPreview(actorId: String): ActorStoreResult<String?> = runCatching {
        com.donnclab.kinetiframe.domain.render.ActorThumbnailMaker.create(files.actorDirectory(actorId), loadValue(actorId).second)
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

    fun saveRevision(actorId: String, sceneGraph: com.donnclab.kinetiframe.engine.rigging.model.SceneGraph,
                     newRevisionId: String, nowEpochMs: Long, artworkAssetPaths: Map<String, String>? = null,
                     view: com.donnclab.kinetiframe.engine.rigging.model.ActorView? = null): ActorStoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
        val (manifest, selected) = loadValue(actorId)
        val current = if (view == null || selected.view == view) selected else {
            val revisionId = requireNotNull(manifest.authoredViews()[view]) { "View is not authored" }
            when (val loaded = loadRevision(actorId, revisionId)) {
                is ActorStoreResult.Value -> loaded.value
                is ActorStoreResult.Failure -> error(loaded.message)
            }
        }
        ActorDocumentValidation.requireId(newRevisionId, "revision ID")
        val revision = current.copy(revisionId = newRevisionId, sceneGraph = sceneGraph,
            artworkAssetPaths = artworkAssetPaths ?: current.artworkAssetPaths, createdAtEpochMs = nowEpochMs).validate()
        ActorTemplateContract.validate(manifest.templateId, revision.sceneGraph)
        val nextManifest = manifest.copy(currentRevisionId = newRevisionId, updatedAtEpochMs = nowEpochMs,
            viewRevisions = manifest.authoredViews() + (current.view to newRevisionId)).validate()
        saveValue(nextManifest, revision)
        nextManifest to revision
    }.fold(::value, ::failure)

    fun selectView(actorId: String, view: com.donnclab.kinetiframe.engine.rigging.model.ActorView,
                   nowEpochMs: Long): ActorStoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
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
    fun createView(actorId: String, view: com.donnclab.kinetiframe.engine.rigging.model.ActorView,
                   revisionId: String, nowEpochMs: Long): ActorStoreResult<Pair<ActorManifest, ActorRevision>> = runCatching {
        val (manifest, _) = loadValue(actorId)
        require(view != com.donnclab.kinetiframe.engine.rigging.model.ActorView.ORIGINAL && view !in manifest.authoredViews()) { "View already exists" }
        val template = when (manifest.templateId) {
            ActorManifest.HUMAN_TEMPLATE_ID -> com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate.HUMAN
            ActorManifest.ANIMAL_TEMPLATE_ID -> com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate.ANIMAL
            else -> com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate.CUSTOM
        }
        val revision = ActorRevision(revisionId = revisionId, actorId = actorId, view = view,
            sceneGraph = com.donnclab.kinetiframe.engine.rigging.model.ActorTemplates.create(template, com.donnclab.kinetiframe.engine.model.StageDimensions.LandscapeHd, view),
            createdAtEpochMs = nowEpochMs)
        val next = manifest.copy(currentRevisionId = revisionId, updatedAtEpochMs = nowEpochMs,
            viewRevisions = manifest.authoredViews() + (view to revisionId))
        saveValue(next, revision)
        next to revision
    }.fold(::value, ::failure)

    /** Duplicates structural data and its revision-owned artwork under fresh actor identity. */
    fun duplicate(sourceActorId: String, duplicateActorId: String, duplicateRevisionId: String,
                  displayName: String, nowEpochMs: Long): ActorStoreResult<ActorManifest> = runCatching {
        require(!files.actorDirectory(duplicateActorId).exists()) { "Actor duplicate ID already exists" }
        val (sourceManifest, source) = loadValue(sourceActorId)
        val copies = sourceManifest.authoredViews().map { (view, revisionId) ->
            val original = when (val loaded = loadRevision(sourceActorId, revisionId)) {
                is ActorStoreResult.Value -> loaded.value
                is ActorStoreResult.Failure -> error(loaded.message)
            }
            val targetId = if (revisionId == source.revisionId) duplicateRevisionId else "$duplicateRevisionId-${view.name.lowercase()}"
            original.copy(actorId = duplicateActorId, revisionId = targetId,
                artworkAssetPaths = copyRevisionArtwork(sourceActorId, revisionId, duplicateActorId, targetId, original.artworkAssetPaths),
                createdAtEpochMs = nowEpochMs)
        }
        val manifest = ActorManifest(actorId = duplicateActorId, displayName = displayName.trim(),
            templateId = sourceManifest.templateId, currentRevisionId = duplicateRevisionId,
            createdAtEpochMs = nowEpochMs, updatedAtEpochMs = nowEpochMs,
            viewRevisions = copies.associate { it.view to it.revisionId }).validate()
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

    private fun toEntity(manifest: ActorManifest, thumbnail: String?) = com.donnclab.kinetiframe.data.actor.db.ActorEntity(
        actorId = manifest.actorId, displayName = manifest.displayName, templateId = manifest.templateId,
        currentRevisionId = manifest.currentRevisionId, createdAtEpochMs = manifest.createdAtEpochMs,
        updatedAtEpochMs = manifest.updatedAtEpochMs, thumbnailPath = thumbnail
    )

    private fun toManifest(entity: com.donnclab.kinetiframe.data.actor.db.ActorEntity) = ActorManifest(
        actorId = entity.actorId, displayName = entity.displayName, templateId = entity.templateId,
        currentRevisionId = entity.currentRevisionId, createdAtEpochMs = entity.createdAtEpochMs,
        updatedAtEpochMs = entity.updatedAtEpochMs
    )

    private fun copyRevisionArtwork(sourceActorId: String, sourceRevisionId: String, targetActorId: String,
                                    targetRevisionId: String, artwork: Map<String, String>): Map<String, String> =
        artwork.mapNotNull { (partId, relativePath) ->
            val source = safeArtworkFile(files.actorDirectory(sourceActorId), relativePath)
            require(source.isFile) { "Actor artwork is missing: $relativePath" }
            val fileName = source.name
            val targetRelative = "revisions/$targetRevisionId/artwork/$fileName"
            val target = safeArtworkFile(files.actorDirectory(targetActorId), targetRelative)
            target.parentFile?.mkdirs()
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            partId to targetRelative
        }.toMap()

    private fun safeArtworkFile(root: File, relativePath: String): File {
        return ActorPathPolicy.resolveWithin(root, relativePath, "Artwork")
    }

    private fun <T> value(value: T): ActorStoreResult<T> = ActorStoreResult.Value(value)
    private fun failure(error: Throwable): ActorStoreResult.Failure = ActorStoreResult.Failure(error.message ?: "Actor storage failed", error)
    private companion object { const val MANIFEST = "manifest.json" }
}

/**
 * Full documents are read from disk and the optional index is reconciled. A picker may use only
 * lightweight fields, but returning a document must never silently strip its authored motion.
 */
class PerformanceRepository(
    private val files: ActorFileStore,
    private val json: Json = Json { ignoreUnknownKeys = false; prettyPrint = true },
    private val dao: com.donnclab.kinetiframe.data.actor.db.PerformanceDao? = null
) {
    /** Discover motions across projects; placements keep an independent local snapshot. */
    fun listForActor(actorId: String): ActorStoreResult<List<PerformanceDocument>> = runCatching {
        ActorDocumentValidation.requireId(actorId, "actor ID")
        files.projectsRoot().listFiles().orEmpty().filter(File::isDirectory).flatMap { directory ->
            when (val result = list(directory.name, actorId)) {
                is ActorStoreResult.Value -> result.value
                is ActorStoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
            }
        }.sortedBy { it.displayName.lowercase() }
    }.fold(::value, ::failure)

    /** Copy, never relocate: later edits cannot alter the original project's motion. */
    fun copyIntoProject(source: PerformanceDocument, projectId: String): ActorStoreResult<PerformanceDocument> {
        if (source.projectId == projectId) return load(projectId, source.performanceId)
        val existing = when (val result = list(projectId, source.actorId)) {
            is ActorStoreResult.Value -> result.value
            is ActorStoreResult.Failure -> return result
        }
        var name = source.displayName
        var suffix = 2
        while (existing.any { it.displayName.equals(name, true) }) name = "${source.displayName.take(48)} (${suffix++})"
        return save(source.copy(projectId = projectId, performanceId = "performance-${java.util.UUID.randomUUID()}", displayName = name))
    }
    fun save(document: PerformanceDocument): ActorStoreResult<PerformanceDocument> = runCatching {
        document.validate()
        val duplicateName = when (val existing = list(document.projectId, document.actorId)) {
            is ActorStoreResult.Value -> existing.value.any {
                it.performanceId != document.performanceId && it.displayName.equals(document.displayName, ignoreCase = true)
            }
            is ActorStoreResult.Failure -> throw IllegalStateException(existing.message, existing.cause)
        }
        require(!duplicateName) { "An actor performance named '${document.displayName}' already exists in this project" }
        files.writeAtomically(files.performanceFile(document.projectId, document.performanceId), json.encodeToString(document))
        dao?.let { d -> runCatching { d.upsert(toEntity(document)) } }
        document
    }.fold(::value, ::failure)

    fun list(projectId: String, actorId: String? = null): ActorStoreResult<List<PerformanceDocument>> = runCatching {
        ActorDocumentValidation.requireId(projectId, "project ID")
        val cached = dao?.let { d -> runCatching { d.getForProject(projectId) }.getOrNull() }
        val fromDisk = File(files.projectDirectory(projectId), "performances").listFiles().orEmpty()
            .filter { it.extension == "json" }.mapNotNull { file ->
                runCatching { json.decodeFromString<PerformanceDocument>(file.readText()).validate().also {
                    require(it.projectId == projectId && it.performanceId == file.nameWithoutExtension)
                } }.getOrNull()
            }
        dao?.let { d -> cached.orEmpty().filter { row -> fromDisk.none { it.performanceId == row.performanceId } }
            .forEach { runCatching { d.deleteById(it.performanceId) } } }
        dao?.let { d -> fromDisk.forEach { document -> runCatching { d.upsert(toEntity(document)) } } }
        fromDisk.filter { actorId == null || it.actorId == actorId }.sortedBy { it.displayName.lowercase() }
    }.fold(::value, ::failure)

    fun load(projectId: String, performanceId: String): ActorStoreResult<PerformanceDocument> = runCatching {
        ActorDocumentValidation.requireId(projectId, "project ID"); ActorDocumentValidation.requireId(performanceId, "performance ID")
        json.decodeFromString<PerformanceDocument>(requireNotNull(files.readOrNull(files.performanceFile(projectId, performanceId))) { "Performance not found" }).validate().also {
            require(it.projectId == projectId && it.performanceId == performanceId) { "Performance identity mismatch" }
        }
    }.fold(::value, ::failure)

    fun rename(projectId: String, performanceId: String, name: String, nowEpochMs: Long): ActorStoreResult<PerformanceDocument> = runCatching {
        val original = loadValue(projectId, performanceId)
        val renamed = original.copy(displayName = name.trim(), updatedAtEpochMs = nowEpochMs).validate()
        saveValue(renamed); renamed
    }.fold(::value, ::failure)

    fun duplicate(projectId: String, performanceId: String, newPerformanceId: String, nowEpochMs: Long): ActorStoreResult<PerformanceDocument> = runCatching {
        val original = loadValue(projectId, performanceId)
        val duplicate = original.copy(performanceId = newPerformanceId, displayName = "${original.displayName} copy",
            createdAtEpochMs = nowEpochMs, updatedAtEpochMs = nowEpochMs).validate()
        saveValue(duplicate); duplicate
    }.fold(::value, ::failure)

    fun archive(projectId: String, performanceId: String): ActorStoreResult<Result<Unit>?> = runCatching {
        require(files.performanceFile(projectId, performanceId).isFile) { "Performance not found" }
        files.move(files.performanceFile(projectId, performanceId), files.archivedPerformanceFile(projectId, performanceId))
        dao?.let { d -> runCatching { d.deleteById(performanceId) } }
    }.fold(::value, ::failure)

    private fun loadValue(projectId: String, performanceId: String): PerformanceDocument = when (val result = load(projectId, performanceId)) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
    }

    private fun saveValue(document: PerformanceDocument) {
        when (val result = save(document)) {
            is ActorStoreResult.Value -> Unit
            is ActorStoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
        }
    }

    private fun toEntity(document: PerformanceDocument) = com.donnclab.kinetiframe.data.actor.db.PerformanceEntity(
        performanceId = document.performanceId, projectId = document.projectId, actorId = document.actorId,
        actorRevisionId = document.actorRevisionId, displayName = document.displayName,
        durationMicros = document.durationMicros, playback = document.playback.name,
        createdAtEpochMs = document.createdAtEpochMs, updatedAtEpochMs = document.updatedAtEpochMs
    )

    /** Reconstructs just enough of a [PerformanceDocument] to satisfy every current list-then-use
     *  call site (name, duration, actor/revision identity) -- pose keys and rotation curves stay
     *  empty, since nothing that reads a *listed* performance touches them; a caller that needs
     *  the real animation data always [load]s the full document by ID separately before using it. */
    private fun toIndexOnlyDocument(entity: com.donnclab.kinetiframe.data.actor.db.PerformanceEntity) = PerformanceDocument(
        performanceId = entity.performanceId, projectId = entity.projectId, actorId = entity.actorId,
        actorRevisionId = entity.actorRevisionId, displayName = entity.displayName,
        durationMicros = entity.durationMicros, playback = runCatching { PerformancePlayback.valueOf(entity.playback) }.getOrDefault(PerformancePlayback.LOOP),
        createdAtEpochMs = entity.createdAtEpochMs, updatedAtEpochMs = entity.updatedAtEpochMs
    )

    private fun <T> value(value: T): ActorStoreResult<T> = ActorStoreResult.Value(value)
    private fun failure(error: Throwable): ActorStoreResult.Failure = ActorStoreResult.Failure(error.message ?: "Performance storage failed", error)
}

class ActorPlacementRepository(private val files: ActorFileStore, private val json: Json = Json { ignoreUnknownKeys = false; prettyPrint = true }) {
    fun save(placement: ActorPlacement): ActorStoreResult<ActorPlacement> = runCatching {
        placement.validate()
        files.writeAtomically(File(files.projectDirectory(placement.projectId), "placements/${placement.placementId}.json"), json.encodeToString(placement))
        placement
    }.fold(::value, ::failure)

    fun list(projectId: String): ActorStoreResult<List<ActorPlacement>> = runCatching {
        ActorDocumentValidation.requireId(projectId, "project ID")
        File(files.projectDirectory(projectId), "placements").listFiles().orEmpty().filter { it.extension == "json" }
            .map { json.decodeFromString<ActorPlacement>(it.readText()).validate() }.sortedWith(compareBy(ActorPlacement::startFrame, ActorPlacement::zIndex))
    }.fold(::value, ::failure)

    /** Recoverable removal; never permanently deletes a project placement document. */
    fun archive(projectId: String, placementId: String): ActorStoreResult<Unit> = runCatching {
        ActorDocumentValidation.requireId(projectId, "project ID")
        ActorDocumentValidation.requireId(placementId, "placement ID")
        val source = File(files.projectDirectory(projectId), "placements/$placementId.json")
        require(source.isFile) { "Placement not found" }
        files.move(source, File(files.projectDirectory(projectId), "archive/placements/$placementId.json"))
    }.fold(::value, ::failure)

    private fun <T> value(value: T): ActorStoreResult<T> = ActorStoreResult.Value(value)
    private fun failure(error: Throwable): ActorStoreResult.Failure = ActorStoreResult.Failure(error.message ?: "Placement storage failed", error)
}

/** Read-only reference scan used to make actor archive/delete decisions safe. */
class ActorReferenceIndex(private val files: ActorFileStore, private val json: Json = Json { ignoreUnknownKeys = false }) {
    fun isReferenced(actorId: String): Boolean = runCatching {
        File(files.projectsRoot(), "").walkTopDown().filter { it.extension == "json" && it.parentFile?.name == "performances" }
            .any { json.decodeFromString<PerformanceDocument>(it.readText()).validate().actorId == actorId } ||
            files.libraryRoot().walkTopDown().filter { it.name == "manifest.json" }.any {
                json.decodeFromString<LibraryItemManifest>(it.readText()).validate().sourceActorId == actorId
            }
    }.getOrDefault(true)
}
