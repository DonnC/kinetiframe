package com.donnclab.kinetiframe.presentation.library

import com.donnclab.kinetiframe.library.data.LibraryOrganizationStore
import com.donnclab.kinetiframe.library.model.AssetOrganization
import com.donnclab.kinetiframe.library.model.BrowseMode
import com.donnclab.kinetiframe.library.model.BrowserPosition
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

@OptIn(ExperimentalCoroutinesApi::class)
class HierarchyBrowserViewModelTest {
    @TempDir lateinit var root: File
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }
    private fun store() = LibraryOrganizationStore(File(root, "organisation.json"))

    @Test fun `actor and project organisation persist without affecting pack preferences`() = runTest(dispatcher) {
        store().rememberBrowser("packs", BrowserPosition(BrowseMode.FAVOURITES))
        val model = HierarchyBrowserViewModel(store(), ioDispatcher = dispatcher)
        model.handleIntent(HierarchyBrowserIntent.Organise("actor:tavi", AssetOrganization("Loose Signal")))
        model.handleIntent(HierarchyBrowserIntent.Favourite("actor:tavi"))
        model.handleIntent(HierarchyBrowserIntent.Organise("project:episode", AssetOrganization("Loose Signal")))
        model.handleIntent(
            HierarchyBrowserIntent.Browse("projects", BrowserPosition(BrowseMode.COLLECTIONS, "Loose Signal")),
        )
        model.state.first { it.organization.browsers["projects"]?.collection == "Loose Signal" }
        val reopened = HierarchyBrowserViewModel(store(), ioDispatcher = dispatcher)
        val state = reopened.state.first { it.organization.items.size == 2 }
        assertTrue(state.organization.items["actor:tavi"]!!.favourite)
        assertEquals("Loose Signal", state.organization.items["project:episode"]!!.collection)
        assertEquals(BrowseMode.FAVOURITES, state.organization.browsers["packs"]!!.mode)
    }

    @Test fun `invalid private metadata surfaces error and remains intact`() = runTest(dispatcher) {
        val file = File(root, "organisation.json").apply { writeText("invalid") }
        val model = HierarchyBrowserViewModel(store(), ioDispatcher = dispatcher)
        model.state.first { it.error != null }
        model.handleIntent(HierarchyBrowserIntent.Favourite("actor:tavi"))
        advanceUntilIdle()
        assertEquals("invalid", file.readText())
    }
}
