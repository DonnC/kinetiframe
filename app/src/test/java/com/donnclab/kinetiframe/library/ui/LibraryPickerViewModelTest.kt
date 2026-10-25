package com.donnclab.kinetiframe.library.ui

import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.LibraryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryPickerViewModelTest {
    @TempDir lateinit var root: File

    @TempDir lateinit var sources: File

    private val dispatcher = StandardTestDispatcher()
    private lateinit var library: LibraryRepository

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
        library = LibraryRepository(DocumentFileStore(root))
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }

    private fun export(
        kind: LibraryItemKind,
        id: String,
        scope: LibraryScope = LibraryScope.PERSONAL,
        extension: String = "png",
    ): LibraryItemManifest {
        val source = File(sources, "$id.$extension").apply { writeBytes(byteArrayOf(1, 2, 3)) }
        val projectId = "project-a".takeIf { scope == LibraryScope.PROJECT }
        val result = library.exportAsset(kind, id, id, source, scope, projectId, nowEpochMs = 1L)
        return (result as StoreResult.Value).value
    }

    @Test fun `opening lists the personal library and the toggle switches to the project's`() = runTest(dispatcher) {
        export(LibraryItemKind.BACKGROUND_IMAGE, "personal-room")
        export(LibraryItemKind.BACKGROUND_IMAGE, "project-room", LibraryScope.PROJECT)
        val model = LibraryPickerViewModel(library, "project-a", dispatcher)

        model.handleIntent(LibraryPickerIntent.Open)
        advanceUntilIdle()
        assertEquals(listOf("personal-room"), model.state.value.items.map { it.libraryItemId })

        model.handleIntent(LibraryPickerIntent.Filter(LibraryItemKind.BACKGROUND_IMAGE))
        model.handleIntent(LibraryPickerIntent.SelectScope(LibraryScope.PROJECT))
        advanceUntilIdle()
        assertEquals(listOf("project-room"), model.state.value.items.map { it.libraryItemId })
        assertNull(model.state.value.kindFilter)
        assertEquals(false, model.state.value.loading)
    }

    @Test fun `an audio preview finds the item's file until it is closed`() = runTest(dispatcher) {
        val voice = export(LibraryItemKind.AUDIO, "voice", extension = "m4a")
        val model = LibraryPickerViewModel(library, ioDispatcher = dispatcher)

        model.handleIntent(LibraryPickerIntent.Preview(voice))
        advanceUntilIdle()
        val path = model.state.value.preview?.path
        assertNotNull(path)
        assertEquals("asset.m4a", File(path!!).name)

        model.handleIntent(LibraryPickerIntent.ClosePreview)
        assertNull(model.state.value.preview)
    }

    @Test fun `cards show an image for backgrounds and none for audio`() {
        val room = export(LibraryItemKind.BACKGROUND_IMAGE, "room")
        val voice = export(LibraryItemKind.AUDIO, "voice", extension = "m4a")
        val model = LibraryPickerViewModel(library, ioDispatcher = dispatcher)

        assertEquals("asset.png", model.previewImage(room)?.name)
        assertNull(model.previewImage(voice))
    }
}
