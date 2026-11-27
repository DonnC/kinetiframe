package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.contracts.StoreFailure
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.DurableFile
import com.donnclab.kinetiframe.library.data.LibraryRepository
import java.io.File

/** Explicit one-time trial-library reset. Archive, never erase, user-authored payloads.
 * Must run on IO after the curated replacements are installed and before browse reload.
 * A completed receipt ensures future user projects/packs aren't removed on another launch.
 */
class CuratedLibraryMigration(private val files: DocumentFileStore) {
    /**
     * Whether there is trial content to offer to archive. A fresh install has only bundled content,
     * so it is never asked; its receipt is written then, and its own future work never counts as trial.
     */
    fun pending(): Boolean = synchronized(lock) {
        val receipt = receipt()
        if (receipt.isFile) return false
        // Trial work means projects and actors. The library also holds items the app installs
        // itself, such as palettes, so it alone never triggers the offer.
        val archivable = runCatching {
            archivable().any { (sourceRoot, _) -> sourceRoot != files.libraryRoot() }
        }.getOrDefault(true)
        if (!archivable) runCatching { DurableFile.write(receipt) { it.write("nothing-to-archive\n".toByteArray()) } }
        archivable
    }

    private fun receipt() = File(requireNotNull(files.projectsRoot().parentFile).canonicalFile, RECEIPT)

    /** Folders that cleanup would move: anything that isn't the curated set or what it depends on. */
    private fun archivable(): List<Pair<File, File>> {
        val library = when (val listed = LibraryRepository(files).list()) {
            is StoreResult.Value -> listed.value
            is StoreResult.Failure -> error(listed.message)
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
        return listOf(
            files.projectsRoot() to retainedProjects,
            files.libraryRoot() to retainedLibrary,
            files.actorsRoot() to retainedActors,
        ).flatMap { (sourceRoot, retained) ->
            sourceRoot.listFiles().orEmpty().filter { it.isDirectory && it.name !in retained }.map { sourceRoot to it }
        }
    }

    fun migrate(): StoreResult<Int> = synchronized(lock) {
        runCatching {
            val root = requireNotNull(files.projectsRoot().parentFile).canonicalFile
            val receipt = receipt()
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
            ) { "The bundled Tavi actors must be installed before cleanup" }
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
            var moved = 0
            archivable().forEach { (sourceRoot, source) ->
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
            DurableFile.write(receipt) { it.write("archived-curated-v3\n".toByteArray()) }
            moved
        }.fold({ StoreResult.Value(it) }, { StoreResult.Failure(StoreFailure.LIBRARY_CLEANUP, it) })
    }

    companion object {
        private const val RECEIPT = "demo-receipts/curated-v3.installed"
        private val lock = Any()
        val castIds = setOf(BundledActorSampleInstaller.DRAWN_TAVI_ID)
    }
}
