package com.donnclab.kinetiframe.project.domain

import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.data.PackArchiver
import com.donnclab.kinetiframe.project.data.KframeArchiver
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * Moves projects and packs in and out of the app: `.kframe` archives to share or import, projects
 * saved to and imported from the library, and `.kpack` archives to share. Shared archives are
 * written under [filesDir], where the app's file provider serves them; working copies go to
 * [cacheDir] and are deleted afterwards. Call from a background thread.
 */
class ProjectTransfers(
    private val files: DocumentFileStore,
    private val library: LibraryRepository,
    private val filesDir: File,
    private val cacheDir: File,
    private val now: () -> Long = System::currentTimeMillis,
) {
    /** A `.kframe` archive of [projectId], ready to share. */
    fun projectForSharing(projectId: String): StoreResult<File> =
        KframeArchiver.export(files, projectId, File(File(filesDir, "kframe-exports"), "$projectId.kframe"))

    /** Imports the `.kframe` archive that [open] reads, such as a file the animator picked. */
    fun importArchive(open: () -> InputStream?): StoreResult<String> {
        val tempFile = File(cacheDir, "import-${now()}.kframe")
        return runCatching {
            open()?.use { input -> tempFile.outputStream().use { output -> input.copyTo(output) } }
                ?: throw IllegalStateException("Could not read the selected file")
            KframeArchiver.import(files, tempFile)
        }.getOrElse { StoreResult.Failure(it.message ?: "Could not import this project") }
            .also { tempFile.delete() }
    }

    /** Saves a copy of [project] to the personal library. */
    fun saveToLibrary(project: ProjectManifest): StoreResult<LibraryItemManifest> = runCatching {
        val tempFile = File(cacheDir, "library-project-${now()}.kframe")
        when (val exported = KframeArchiver.export(files, project.projectId, tempFile)) {
            is StoreResult.Value -> Unit
            is StoreResult.Failure -> throw IllegalStateException(exported.message, exported.cause)
        }
        library.exportAsset(
            kind = LibraryItemKind.PROJECT,
            itemId = "library-${UUID.randomUUID()}",
            displayName = project.displayName,
            sourceFile = tempFile,
            projectId = project.projectId,
            nowEpochMs = now(),
        ).also { tempFile.delete() }
    }.getOrElse { StoreResult.Failure(it.message ?: "Could not save this project to your library") }

    /** Imports the project saved in the library as [item]. */
    fun importFromLibrary(item: LibraryItemManifest): StoreResult<String> {
        val tempFile = File(cacheDir, "library-import-${now()}.kframe")
        return runCatching {
            when (val copied = library.copyAssetTo(item.libraryItemId, tempFile)) {
                is StoreResult.Value -> Unit
                is StoreResult.Failure -> throw IllegalStateException(copied.message, copied.cause)
            }
            KframeArchiver.import(files, tempFile)
        }.getOrElse { StoreResult.Failure(it.message ?: "Could not import this project") }
            .also { tempFile.delete() }
    }

    /** A `.kpack` archive of the personal library pack [pack], ready to share. */
    fun packForSharing(pack: LibraryItemManifest): StoreResult<File> = PackArchiver.export(
        files,
        library,
        pack.libraryItemId,
        File(File(filesDir, "pack-exports"), "${pack.displayName}.kpack"),
        LibraryScope.PERSONAL,
    )
}
