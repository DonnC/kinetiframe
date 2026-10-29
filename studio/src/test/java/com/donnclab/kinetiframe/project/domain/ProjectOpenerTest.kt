package com.donnclab.kinetiframe.project.domain

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.ProjectSaveDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ProjectOpenerTest {
    @TempDir lateinit var root: File

    @TempDir lateinit var draft: File

    private fun saved(files: DocumentFileStore): ProjectSaveDocument {
        val layer = Layer(name = "Main Layer", id = "main")
        val document = ProjectSaveDocument(
            project = ProjectDocument(id = "project-a", layers = listOf(layer)),
            frameLayers = mapOf(0 to listOf(layer)),
            totalFrames = 1,
        )
        val manifest = ProjectManifest(
            projectId = "project-a",
            displayName = "My Cartoon",
            stage = StageDimensions.LandscapeHd,
            fps = 12,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L,
            totalFrames = 1,
        )
        assertTrue(ProjectRepository(files).save(manifest, document) is StoreResult.Value)
        return document
    }

    @Test fun `a project opens with its document, and a draft gets a copy of its frames`() {
        val files = DocumentFileStore(root)
        val document = saved(files)
        val opener = ProjectOpener(files, ProjectRepository(files))
        File(opener.projectDirectory("project-a"), "frames").apply { mkdirs() }.resolve("frame.png").writeText("pixels")

        val draftFrames = File(draft, "frames")
        val opened = opener.open("project-a", draftFrames)

        assertEquals(document, (opened as StoreResult.Value).value)
        assertEquals("pixels", draftFrames.walkTopDown().single { it.isFile }.readText())
    }

    @Test fun `a missing project fails without touching the draft`() {
        val files = DocumentFileStore(root)
        val opener = ProjectOpener(files, ProjectRepository(files))

        val draftFrames = File(draft, "frames")
        val opened = opener.open("missing", draftFrames)

        assertTrue(opened is StoreResult.Failure)
        assertTrue(!draftFrames.exists())
    }
}
