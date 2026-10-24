package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.data.storage.ContainedPaths
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import java.io.File
import java.util.zip.ZipFile

/** Private staging only: installing reusable actors must not import hundreds of scene cels.
 * Validate every relative destination, expansion budget and actor catalog before publication.
 * The staging directory is disposable; durable public writes belong to the repository/installer. */
internal object BundledActorSourceReader {
    const val ASSET_PATH = "actor/starter-actors-v4.zip"

    fun read(archive: File, files: DocumentFileStore, expectedIds: Set<String>) {
        var total = 0L
        val names = mutableSetOf<String>()
        ZipFile(archive).use { zip ->
            val entries = zip.entries()
            while (entries.hasMoreElements()) {
                val entry = entries.nextElement()
                if (entry.isDirectory || !entry.name.startsWith("dependencies/actors/")) continue
                val relative = entry.name.removePrefix("dependencies/actors/")
                val id = relative.substringBefore('/')
                if (id !in expectedIds) continue
                require(names.add(relative)) { "Duplicate bundled actor file" }
                val destination = ContainedPaths.resolveWithin(files.actorsRoot(), relative, "Bundled actor")
                destination.parentFile?.mkdirs()
                zip.getInputStream(entry).use { input ->
                    destination.outputStream().buffered().use { output ->
                        val buffer = ByteArray(8192)
                        var count = input.read(buffer)
                        while (count != -1) {
                            total += count
                            require(total <= 128L * 1024 * 1024) { "Bundled actor expansion budget exceeded" }
                            output.write(buffer, 0, count)
                            count = input.read(buffer)
                        }
                    }
                }
            }
        }
        val repository = ActorRepository(files)
        expectedIds.forEach { id ->
            val loaded = repository.load(id)
            require(loaded is StoreResult.Value) { "Incomplete bundled actor $id" }
            loaded.value.first.authoredViews().values.forEach { revisionId ->
                val revision = repository.loadRevision(id, revisionId)
                require(revision is StoreResult.Value)
                revision.value.artworkAssetPaths.values.forEach { relative ->
                    val artwork = ContainedPaths.resolveWithin(files.actorDirectory(id), relative, "Bundled artwork")
                    require(artwork.isFile && artwork.length() > 0) { "Missing bundled artwork" }
                }
            }
        }
    }
}
