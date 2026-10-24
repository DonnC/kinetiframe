package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.data.LibraryItemKind
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.DurableFile
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.project.data.ProjectRepository
import java.io.File

/** Explicit one-time trial-library reset. Archive, never erase, user-authored payloads.
 * Must run on IO after the curated replacements are installed and before browse reload.
 * A completed receipt ensures future user projects/packs aren't removed on another launch.
 */
class CuratedLibraryMigration(private val files: DocumentFileStore) {
    fun pending(): Boolean = !File(files.projectsRoot().parentFile, "demo-receipts/curated-v3.installed").isFile
    fun migrate(): StoreResult<Int> = synchronized(lock) {
        runCatching {
            val root = requireNotNull(files.projectsRoot().parentFile).canonicalFile
            val receipt = File(root, "demo-receipts/curated-v3.installed")
            if (receipt.isFile) return@runCatching 0
            require(BundledDemoInstaller.curatedPackIds.all { files.libraryDirectory(it).isDirectory }) {
                "Curated packs must be installed before cleanup"
            }
            require(files.projectDirectory(BundledDemoInstaller.TAVI_PRESENTATION_ID).isDirectory) {
                "Tavi presents must be available before cleanup"
            }
            require(
                castIds.all {
                    files.actorDirectory(it).isDirectory
                },
            ) { "Four-view cast must be installed before cleanup" }
            val library = when (val listed = LibraryRepository(files).list()) {
                is StoreResult.Value -> listed.value
                is StoreResult.Failure -> error(listed.message)
            }
            require(
                BundledDemoInstaller.curatedPackIds.all { id ->
                    library.any {
                        it.libraryItemId == id &&
                            it.kind == LibraryItemKind.PACK
                    }
                },
            ) {
                "Curated packs must have valid manifests before cleanup"
            }
            val retainedLibrary = library.filter {
                it.kind == LibraryItemKind.BACKGROUND_IMAGE ||
                    it.libraryItemId in BundledDemoInstaller.curatedPackIds ||
                    it.libraryItemId in castIds.map { id -> "starter-$id" }
            }.map { it.libraryItemId }.toSet()
            val projects = when (val listed = ProjectRepository(files).list()) {
                is StoreResult.Value -> listed.value
                is StoreResult.Failure -> error(listed.message)
            }
            val retainedProjects = projects.filter {
                it.projectId in BundledDemoInstaller.projectIds ||
                    (
                        it.displayName.contains("Tavi", ignoreCase = true) &&
                            it.displayName.contains("presents", ignoreCase = true)
                        )
            }.map { it.projectId }.toSet() + BundledDemoInstaller.TAVI_PRESENTATION_ID
            // A retained scene must retain its dependencies, even if an actor is not in the cast.
            val retainedActors = castIds + retainedProjects.flatMap { projectId ->
                when (val loaded = ProjectRepository(files).load(projectId)) {
                    is StoreResult.Value -> loaded.value.second.project.actorPlacements.values.map { it.actorId }
                    is StoreResult.Failure -> emptyList()
                }
            }
            var moved = 0
            listOf(
                files.projectsRoot() to retainedProjects,
                files.libraryRoot() to retainedLibrary,
                files.actorsRoot() to retainedActors,
            ).forEach { (sourceRoot, retained) ->
                sourceRoot.listFiles().orEmpty().filter { it.isDirectory && it.name !in retained }.forEach { source ->
                    // Resolve both ends before moving; reject symlinks/paths outside this exact store.
                    require(source.canonicalFile.parentFile == sourceRoot.canonicalFile) { "Unsafe cleanup source" }
                    val destination = File(root, "archive/curated-v3/${sourceRoot.name}/${source.name}").canonicalFile
                    require(destination.toPath().startsWith(File(root, "archive/curated-v3").toPath())) {
                        "Unsafe archive destination"
                    }
                    require(!destination.exists()) { "Archive already contains ${source.name}; cleanup stopped" }
                    files.move(source, destination)
                    moved++
                }
            }
            DurableFile.write(receipt) { it.write("archived-curated-v3\n".toByteArray()) }
            moved
        }.fold({ StoreResult.Value(it) }, { StoreResult.Failure(it.message ?: "Library cleanup failed", it) })
    }

    companion object {
        private val lock = Any()
        val castIds = setOf("ls-tavi-all-views-v3", "ls-mira-all-views-v3", "ls-kuda-all-views-v3")
    }
}
