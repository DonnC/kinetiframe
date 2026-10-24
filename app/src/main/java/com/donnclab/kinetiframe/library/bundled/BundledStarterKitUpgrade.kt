package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.validate
import com.donnclab.kinetiframe.data.storage.ContainedPaths
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.DurableFile
import com.donnclab.kinetiframe.data.storage.StoreResult
import kotlinx.serialization.json.Json
import java.io.File

/** Append repaired snapshots only to an untouched bundled cast. Never rewrite a pinned revision.
 * Asset publication precedes the catalog pointer, making retries safe after an interrupted copy. */
internal class BundledStarterKitUpgrade(
    private val source: DocumentFileStore,
    private val destination: DocumentFileStore,
) {
    private val json = Json { ignoreUnknownKeys = false }
    fun upgradeActor(id: String) {
        val target = destination.actorDirectory(id)
        if (!target.exists()) {
            require(source.actorDirectory(id).copyRecursively(target))
            return
        }
        val repository = ActorRepository(destination)
        val old = value(repository.load(id)).first
        if (!isOriginal(old)) return
        val original = ActorRepository(source)
        val repaired = value(original.load(id)).first
        val revisions = repaired.authoredViews().values.map { value(original.loadRevision(id, it)) }
        revisions.forEach { revision ->
            revision.artworkAssetPaths.values.forEach { relative ->
                val from = ContainedPaths.resolveWithin(source.actorDirectory(id), relative, "Starter artwork")
                val to = ContainedPaths.resolveWithin(target, relative, "Starter artwork")
                // New versioned locations only. An existing different file is not safe to overwrite.
                if (to.exists()) {
                    require(from.readBytes().contentEquals(to.readBytes())) { "Conflicting starter artwork" }
                } else {
                    DurableFile.write(to) { output -> from.inputStream().use { it.copyTo(output) } }
                }
            }
        }
        val activeView = old.authoredViews().entries.first { it.value == old.currentRevisionId }.key
        value(
            repository.saveViews(
                repaired.copy(
                    displayName = old.displayName,
                    createdAtEpochMs = old.createdAtEpochMs,
                    updatedAtEpochMs = maxOf(old.updatedAtEpochMs, repaired.updatedAtEpochMs),
                    currentRevisionId = repaired.authoredViews().getValue(activeView),
                ),
                revisions,
            ),
        )
    }

    /** A reusable shipped bundle isn't an edited actor. Archive its prior bytes before replacing
     * it; preserve user labels/tags. Missing originals stay deleted unless recovering our move. */
    fun prepareLibraryReplacement(id: String): Boolean {
        val target = destination.libraryDirectory(id)
        val archive = File(destination.libraryRoot().parentFile, "archive/starter-kits-v4/library/$id").canonicalFile
        if (!target.exists()) {
            if (archive.isDirectory) preserveLibraryMetadata(archive, source.libraryDirectory(id))
            return archive.exists()
        }
        val manifest =
            runCatching {
                json.decodeFromString<ActorManifest>(File(target, "payload/actor.json").readText())
            }.getOrNull()
                ?: return false
        if (!isOriginal(manifest)) return false
        preserveLibraryMetadata(target, source.libraryDirectory(id))
        require(target.canonicalFile.parentFile == destination.libraryRoot().canonicalFile)
        require(
            archive.toPath().startsWith(
                File(destination.libraryRoot().parentFile, "archive/starter-kits-v4").canonicalFile.toPath(),
            ),
        )
        require(!archive.exists()) { "Prior starter archive already exists" }
        destination.move(target, archive)
        return true
    }
    private fun preserveLibraryMetadata(old: File, fresh: File) {
        val previous = json.decodeFromString<LibraryItemManifest>(File(old, "manifest.json").readText()).validate()
        val next = json.decodeFromString<LibraryItemManifest>(File(fresh, "manifest.json").readText()).validate()
        source.writeAtomically(
            File(fresh, "manifest.json"),
            Json.encodeToString(
                LibraryItemManifest.serializer(),
                next.copy(displayName = previous.displayName, tags = previous.tags),
            ),
        )
    }
    private fun isOriginal(actor: ActorManifest) = actor.updatedAtEpochMs == actor.createdAtEpochMs &&
        actor.authoredViews().all { (view, revision) -> revision == "${actor.actorId}-${view.name.lowercase()}" }
    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> error(result.message)
    }
}
