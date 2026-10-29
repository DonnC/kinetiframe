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
}
