package com.donnclab.kinetiframe.presentation.home

import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.core.document.ProjectSaveDocument
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.bundled.BundledContent
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.project.domain.ProjectTransfers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.FileNotFoundException

@OptIn(ExperimentalCoroutinesApi::class)
class HomeActionsViewModelTest {
    @TempDir lateinit var root: File

    @TempDir lateinit var filesDir: File

    @TempDir lateinit var cacheDir: File

    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }

    private fun model(files: DocumentFileStore): HomeActionsViewModel {
        val library = LibraryRepository(files)
        // No bundled assets in a unit test, so every install reports an error.
        val bundled = BundledContent(files, library) { path -> throw FileNotFoundException(path) }
        return HomeActionsViewModel(bundled, ProjectTransfers(files, library, filesDir, cacheDir), dispatcher)
    }

    @Test fun `failed installs show one message and the trial cleanup is offered`() = runTest(dispatcher) {
        val model = model(DocumentFileStore(root))
        advanceUntilIdle()

        val effect = model.effect.first()

        assertEquals(
            HomeActionsEffect.ShowMessage("Some demo resources could not be installed. Your work was not replaced."),
            effect,
        )
        assertTrue(model.state.value.cleanupOffered)
    }

    @Test fun `sharing a project offers its archive to the share sheet`() = runTest(dispatcher) {
        val files = DocumentFileStore(root)
        val manifest = ProjectManifest(
            projectId = "project-a",
            displayName = "My Cartoon",
            stage = StageDimensions.LandscapeHd,
            fps = 12,
            createdAtEpochMs = 1L,
            updatedAtEpochMs = 1L,
            totalFrames = 1,
        )
        val document = ProjectSaveDocument(
            project = ProjectDocument(
                id = "project-a",
                stage = StageDimensions.LandscapeHd,
                fps = 12,
                layers = listOf(Layer(name = "Main Layer", id = "main")),
            ),
            frameLayers = mapOf(0 to listOf(Layer(name = "Main Layer", id = "main"))),
            totalFrames = 1,
        )
        assertTrue(ProjectRepository(files).save(manifest, document) is StoreResult.Value)
        val model = model(files)
        advanceUntilIdle()
        model.effect.first() // The install message.

        model.handleIntent(HomeActionsIntent.ShareProject(manifest))
        advanceUntilIdle()

        val share = model.effect.first() as HomeActionsEffect.Share
        assertEquals("Share My Cartoon", share.title)
        assertEquals(File(File(filesDir, "kframe-exports"), "project-a.kframe"), share.file)
    }
}
