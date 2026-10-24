package com.donnclab.kinetiframe.actors.data

import com.donnclab.kinetiframe.core.actor.motion.PerformancePlayback
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Full documents are read from disk and the optional index is reconciled. A picker may use only
 * lightweight fields, but returning a document must never silently strip its authored motion.
 */
class PerformanceRepository(
    private val files: DocumentFileStore,
    private val json: Json = Json {
        ignoreUnknownKeys = false
        prettyPrint = true
    },
    private val dao: com.donnclab.kinetiframe.actors.data.db.PerformanceDao? = null,
) {
    /** Discover motions across projects; placements keep an independent local snapshot. */
    fun listForActor(actorId: String): StoreResult<List<PerformanceDocument>> = runCatching {
        ActorDocumentValidation.requireId(actorId, "actor ID")
        files.projectsRoot().listFiles().orEmpty().filter(File::isDirectory).flatMap { directory ->
            when (val result = list(directory.name, actorId)) {
                is StoreResult.Value -> result.value
                is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
            }
        }.sortedBy { it.displayName.lowercase() }
    }.fold(::value, ::failure)

    /** Copy, never relocate: later edits cannot alter the original project's motion. */
    fun copyIntoProject(source: PerformanceDocument, projectId: String): StoreResult<PerformanceDocument> {
        if (source.projectId == projectId) return load(projectId, source.performanceId)
        val existing = when (val result = list(projectId, source.actorId)) {
            is StoreResult.Value -> result.value
            is StoreResult.Failure -> return result
        }
        var name = source.displayName
        var suffix = 2
        while (existing.any { it.displayName.equals(name, true) }) name = "${source.displayName.take(48)} (${suffix++})"
        return save(
            source.copy(
                projectId = projectId,
                performanceId = "performance-${java.util.UUID.randomUUID()}",
                displayName = name,
            ),
        )
    }
    fun save(document: PerformanceDocument): StoreResult<PerformanceDocument> = runCatching {
        document.validate()
        val duplicateName = when (val existing = list(document.projectId, document.actorId)) {
            is StoreResult.Value -> existing.value.any {
                it.performanceId != document.performanceId &&
                    it.displayName.equals(document.displayName, ignoreCase = true)
            }

            is StoreResult.Failure -> throw IllegalStateException(existing.message, existing.cause)
        }
        require(!duplicateName) {
            "An actor performance named '${document.displayName}' already exists in this project"
        }
        files.writeAtomically(
            files.performanceFile(document.projectId, document.performanceId),
            json.encodeToString(document),
        )
        dao?.let { d -> runCatching { d.upsert(toEntity(document)) } }
        document
    }.fold(::value, ::failure)

    fun list(projectId: String, actorId: String? = null): StoreResult<List<PerformanceDocument>> = runCatching {
        ActorDocumentValidation.requireId(projectId, "project ID")
        val cached = dao?.let { d -> runCatching { d.getForProject(projectId) }.getOrNull() }
        val fromDisk = File(files.projectDirectory(projectId), "performances").listFiles().orEmpty()
            .filter { it.extension == "json" }.mapNotNull { file ->
                runCatching {
                    json.decodeFromString<PerformanceDocument>(file.readText()).validate().also {
                        require(it.projectId == projectId && it.performanceId == file.nameWithoutExtension)
                    }
                }.getOrNull()
            }
        dao?.let { d ->
            cached.orEmpty().filter { row -> fromDisk.none { it.performanceId == row.performanceId } }
                .forEach { runCatching { d.deleteById(it.performanceId) } }
        }
        dao?.let { d -> fromDisk.forEach { document -> runCatching { d.upsert(toEntity(document)) } } }
        fromDisk.filter { actorId == null || it.actorId == actorId }.sortedBy { it.displayName.lowercase() }
    }.fold(::value, ::failure)

    fun load(projectId: String, performanceId: String): StoreResult<PerformanceDocument> = runCatching {
        ActorDocumentValidation.requireId(projectId, "project ID")
        ActorDocumentValidation.requireId(performanceId, "performance ID")
        json.decodeFromString<PerformanceDocument>(
            requireNotNull(files.readOrNull(files.performanceFile(projectId, performanceId))) {
                "Performance not found"
            },
        ).validate().also {
            require(it.projectId == projectId && it.performanceId == performanceId) { "Performance identity mismatch" }
        }
    }.fold(::value, ::failure)

    fun rename(
        projectId: String,
        performanceId: String,
        name: String,
        nowEpochMs: Long,
    ): StoreResult<PerformanceDocument> = runCatching {
        val original = loadValue(projectId, performanceId)
        val renamed = original.copy(displayName = name.trim(), updatedAtEpochMs = nowEpochMs).validate()
        saveValue(renamed)
        renamed
    }.fold(::value, ::failure)

    fun duplicate(
        projectId: String,
        performanceId: String,
        newPerformanceId: String,
        nowEpochMs: Long,
    ): StoreResult<PerformanceDocument> = runCatching {
        val original = loadValue(projectId, performanceId)
        val duplicate = original.copy(
            performanceId = newPerformanceId,
            displayName = "${original.displayName} copy",
            createdAtEpochMs = nowEpochMs,
            updatedAtEpochMs = nowEpochMs,
        ).validate()
        saveValue(duplicate)
        duplicate
    }.fold(::value, ::failure)

    fun archive(projectId: String, performanceId: String): StoreResult<Result<Unit>?> = runCatching {
        require(files.performanceFile(projectId, performanceId).isFile) { "Performance not found" }
        files.move(
            files.performanceFile(projectId, performanceId),
            files.archivedPerformanceFile(projectId, performanceId),
        )
        dao?.let { d -> runCatching { d.deleteById(performanceId) } }
    }.fold(::value, ::failure)

    private fun loadValue(projectId: String, performanceId: String): PerformanceDocument =
        when (val result = load(projectId, performanceId)) {
            is StoreResult.Value -> result.value
            is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
        }

    private fun saveValue(document: PerformanceDocument) {
        when (val result = save(document)) {
            is StoreResult.Value -> Unit
            is StoreResult.Failure -> throw IllegalStateException(result.message, result.cause)
        }
    }

    private fun toEntity(document: PerformanceDocument) = com.donnclab.kinetiframe.actors.data.db.PerformanceEntity(
        performanceId = document.performanceId, projectId = document.projectId, actorId = document.actorId,
        actorRevisionId = document.actorRevisionId, displayName = document.displayName,
        durationMicros = document.durationMicros, playback = document.playback.name,
        createdAtEpochMs = document.createdAtEpochMs, updatedAtEpochMs = document.updatedAtEpochMs,
    )

    /** Reconstructs just enough of a [PerformanceDocument] to satisfy every current list-then-use
     *  call site (name, duration, actor/revision identity) -- pose keys and rotation curves stay
     *  empty, since nothing that reads a *listed* performance touches them; a caller that needs
     *  the real animation data always [load]s the full document by ID separately before using it. */
    private fun toIndexOnlyDocument(entity: com.donnclab.kinetiframe.actors.data.db.PerformanceEntity) =
        PerformanceDocument(
            performanceId = entity.performanceId, projectId = entity.projectId, actorId = entity.actorId,
            actorRevisionId = entity.actorRevisionId, displayName = entity.displayName,
            durationMicros = entity.durationMicros,
            playback = runCatching {
                PerformancePlayback.valueOf(entity.playback)
            }.getOrDefault(PerformancePlayback.LOOP),
            createdAtEpochMs = entity.createdAtEpochMs, updatedAtEpochMs = entity.updatedAtEpochMs,
        )

    private fun <T> value(value: T): StoreResult<T> = StoreResult.Value(value)
    private fun failure(error: Throwable): StoreResult.Failure =
        StoreResult.Failure(error.message ?: "Performance storage failed", error)
}
