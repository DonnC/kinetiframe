package com.donnclab.kinetiframe.presentation.library.packs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import com.donnclab.kinetiframe.data.project.ProjectRepository
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.domain.PackDrafts
import com.donnclab.kinetiframe.library.model.PackFrameTag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PackCreatorViewModelTest {
    @TempDir lateinit var root: File

    @TempDir lateinit var cacheDir: File

    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }

    private fun model(files: DocumentFileStore): PackCreatorViewModel {
        val drafts = PackDrafts(files, ProjectRepository(files), LibraryRepository(files), cacheDir)
        val factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                PackCreatorViewModel(drafts, ioDispatcher = dispatcher) as T
        }
        return ViewModelProvider(store, factory)[PackCreatorViewModel::class.java]
    }

    @Test fun `a new pack's draft is created at the chosen size and discarded when closed unsaved`() = runTest(
        dispatcher,
    ) {
        val files = DocumentFileStore(root)
        val model = model(files)

        model.handleIntent(PackCreatorIntent.EditForm(AddPackFormState(name = "Moods", dimension = 256)))
        model.handleIntent(PackCreatorIntent.Create)
        advanceUntilIdle()
        val draftId = model.state.value.draftId
        assertNotNull(draftId)
        assertEquals("Moods", model.state.value.packName)
        assertEquals(256, model.state.value.canvasDimension)
        assertTrue(files.projectDirectory(draftId!!).exists())

        store.clear()

        assertFalse(files.projectDirectory(draftId).exists())
    }

    @Test fun `a draft whose save failed is kept when closed`() = runTest(dispatcher) {
        val files = DocumentFileStore(root)
        val model = model(files)
        model.handleIntent(PackCreatorIntent.Create)
        advanceUntilIdle()
        val draftId = model.state.value.draftId!!

        model.handleIntent(PackCreatorIntent.SetTag(0, PackFrameTag("")))
        model.handleIntent(PackCreatorIntent.Save(totalFrames = 1))
        advanceUntilIdle()
        assertTrue(model.state.value.saveError.orEmpty().contains("untagged"))

        store.clear()

        assertTrue(files.projectDirectory(draftId).exists())
    }
}
