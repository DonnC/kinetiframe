package com.donnclab.kinetiframe.presentation.studio.color

import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.library.data.PaletteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PaletteViewModelTest {
    @TempDir lateinit var root: File
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }

    @Test fun `load installs real palettes and selection is an MVI intent`() = runTest(dispatcher) {
        val model = PaletteViewModel(PaletteRepository(DocumentFileStore(root)), ioDispatcher = dispatcher)
        val loaded = model.state.first { !it.loading }
        assertNull(loaded.error)
        assertEquals(8, loaded.palettes.size)
        val selected = loaded.palettes.last().id
        model.handleIntent(PaletteIntent.Select(selected))
        assertEquals(selected, model.state.value.selectedId)
    }

    @Test fun `a starter palette can be copied, renamed and filled`() = runTest(dispatcher) {
        val model = PaletteViewModel(PaletteRepository(DocumentFileStore(root)), ioDispatcher = dispatcher)
        val starter = model.state.first { !it.loading }.palettes.first { PaletteRepository.isStarter(it.id) }

        model.handleIntent(PaletteIntent.Duplicate(starter.id, "Mine"))
        dispatcher.scheduler.advanceUntilIdle()
        val copy = model.state.value.palettes.single { it.name == "Mine" }
        assertEquals(starter.swatches, copy.swatches)
        assertTrue(model.state.value.canEdit(copy))

        model.handleIntent(PaletteIntent.Rename(copy.id, "Night"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals("Night", model.state.value.palettes.single { it.id == copy.id }.name)

        model.handleIntent(PaletteIntent.Create("Pair", listOf(0xFFC9372CL, 0xFF000000L)))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(2, model.state.value.palettes.single { it.name == "Pair" }.swatches.size)
    }
}
