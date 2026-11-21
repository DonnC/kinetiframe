package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import java.io.InputStream

/**
 * The content that ships with the app: the demo projects, the demo library items and the sample
 * actors, plus the one-time cleanup of the trial library. Each installer keeps a receipt for what
 * it installed, so [install] never installs anything twice and a demo the animator deleted stays
 * deleted. Call from a background thread.
 */
class BundledContent(
    private val files: DocumentFileStore,
    private val library: LibraryRepository,
    private val openAsset: (String) -> InputStream,
) {
    /** [listsChanged] is true when projects or actors were added, so their lists need a reload. */
    data class Installed(val listsChanged: Boolean, val errors: List<String>)

    fun install(): Installed {
        val demos = BundledDemoInstaller(files, openAsset, library).install()
        val resources = BundledDemoLibraryInstaller(files, openAsset, library).install()
        val actorSamples = BundledActorSampleInstaller(files, openAsset).install()
        return Installed(
            listsChanged = demos.installed > 0 || actorSamples.installed > 0,
            errors = demos.errors + resources.errors + actorSamples.errors,
        )
    }

    /** Installs only the demo library items, which the Library seeds on its own. Returns the errors. */
    fun installLibraryItems(): List<String> = BundledDemoLibraryInstaller(files, openAsset, library).install().errors

    /** Whether the trial library still waits to be archived in favour of the curated one. */
    fun cleanupPending(): Boolean = CuratedLibraryMigration(files).pending()

    fun cleanUp(): StoreResult<Int> = CuratedLibraryMigration(files).migrate()
}
