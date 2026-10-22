package com.donnclab.kinetiframe.ui.packs

import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.library.AssetCategory
import com.donnclab.kinetiframe.data.library.BrowseMode
import com.donnclab.kinetiframe.data.library.BrowserPosition
import com.donnclab.kinetiframe.data.library.LibraryRepository
import com.donnclab.kinetiframe.data.library.PackItemEntry
import com.donnclab.kinetiframe.data.library.PackManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class PacksBrowserViewModelTest {
    @TempDir lateinit var root: File
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }

    private fun repository(): LibraryRepository {
        val repository = LibraryRepository(ActorFileStore(root))
        val png = File(root, "hand.png").apply { writeText("artwork") }
        repository.exportPack(
            "ls-hands",
            "Hands",
            PackManifest(
                "Hands",
                totalItems = 1,
                canvasDimension = 128,
                items = listOf(PackItemEntry("hand.png", 0, "Pointing hand")),
            ),
            mapOf("hand.png" to png),
            nowEpochMs = 1,
        )
        return repository
    }

    @Test fun `pack item index favourites and filters survive model restart`() = runTest(dispatcher) {
        val repository = repository()
        val model = PacksTabViewModel(repository, ioDispatcher = dispatcher)
        val loaded = model.state.first { !it.loading }
        assertEquals(listOf("Pointing hand "), loaded.itemNames["ls-hands"])
        model.handleIntent(PacksTabIntent.ToggleFavourite(loaded.packs.single()))
        model.state.first { it.organization.items["ls-hands"]?.favourite == true }
        val position = BrowserPosition(BrowseMode.COLLECTIONS, "Loose Signal", AssetCategory.GESTURES)
        model.handleIntent(PacksTabIntent.Browse(position))
        model.state.first { it.organization.browsers["packs"] == position }
        val reopened = PacksTabViewModel(repository, ioDispatcher = dispatcher)
        val state = reopened.state.first { !it.loading }
        assertEquals(position, state.browser)
        assertTrue(state.organization.items["ls-hands"]!!.favourite)
        assertEquals("Loose Signal", state.organization.items["ls-hands"]!!.collection)
    }

    @Test fun `bad private metadata is reported instead of crashing or destroying saved packs`() = runTest(dispatcher) {
        val repository = repository()
        File(root, "library-organisation.json").writeText("corrupt")
        val model = PacksTabViewModel(repository, ioDispatcher = dispatcher)
        val state = model.state.first { !it.loading }
        assertNotNull(state.error)
        assertTrue(repository.loadPack("ls-hands") is ActorStoreResult.Value)
        assertEquals("corrupt", File(root, "library-organisation.json").readText())
    }
}
