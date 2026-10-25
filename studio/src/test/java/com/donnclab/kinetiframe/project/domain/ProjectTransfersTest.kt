package com.donnclab.kinetiframe.project.domain

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.project.ProjectSaveDocument
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectTransfersTest {
    @TempDir lateinit var root: File

    @TempDir lateinit var filesDir: File

    @TempDir lateinit var cacheDir: File

    private val manifest = ProjectManifest(
        projectId = "project-a",
        displayName = "My Cartoon",
        stage = StageDimensions.LandscapeHd,
        fps = 12,
        createdAtEpochMs = 1L,
        updatedAtEpochMs = 1L,
        totalFrames = 1,
    )

    private val document = ProjectSaveDocument(
        project = ProjectDocument(
            id = "project-a",
            stage = StageDimensions.LandscapeHd,
            fps = 12,
            layers = listOf(Layer(name = "Main Layer", id = "main")),
        ),
        frameLayers = mapOf(0 to listOf(Layer(name = "Main Layer", id = "main"))),
        totalFrames = 1,
    )

    private fun <T> value(result: StoreResult<T>): T = when (result) {
        is StoreResult.Value -> result.value
        is StoreResult.Failure -> throw AssertionError("Expected success but got: ${result.message}", result.cause)
    }

    private fun transfers(files: DocumentFileStore) =
        ProjectTransfers(files, LibraryRepository(files), filesDir, cacheDir, now = { 42L })

    @Test fun `a shared project is a kframe archive under the file provider's folder`() {
        val files = DocumentFileStore(root)
        value(ProjectRepository(files).save(manifest, document))

        val archive = value(transfers(files).projectForSharing("project-a"))

        assertEquals(File(File(filesDir, "kframe-exports"), "project-a.kframe"), archive)
        assertTrue(archive.length() > 0)
    }

    @Test fun `an unreadable pick fails with a message and leaves no working copy`() {
        val result = transfers(DocumentFileStore(root)).importArchive { null }

        assertEquals("Could not read the selected file", (result as StoreResult.Failure).message)
        assertTrue(cacheDir.listFiles().orEmpty().isEmpty())
    }

    @Test fun `a project saved to the library imports back after the original is deleted`() {
        val files = DocumentFileStore(root)
        val projects = ProjectRepository(files)
        value(projects.save(manifest, document))
        val transfers = transfers(files)

        val item = value(transfers.saveToLibrary(manifest))
        // Importing refuses a project id that already exists locally.
        value(projects.delete("project-a"))
        val importedId = value(transfers.importFromLibrary(item))

        assertEquals("My Cartoon", value(projects.load(importedId)).first.displayName)
        assertTrue(cacheDir.listFiles().orEmpty().isEmpty())
    }
}
