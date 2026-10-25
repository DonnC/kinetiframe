package com.donnclab.kinetiframe.library.data

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class LibraryOrganizationStoreTest {
    @TempDir lateinit var root: File
    private fun store() = LibraryOrganizationStore(File(root, "organisation.json"))

    @Test fun `collections categories favourites and browser position survive restart`() {
        store().updateItem("mouths") {
            it.copy(
                collection = "Loose Signal",
                category = AssetCategory.EXPRESSIONS,
                favourite = true,
                displayName = "Tavi · Mouths",
            )
        }
        val position = BrowserPosition(BrowseMode.COLLECTIONS, "Loose Signal", AssetCategory.EXPRESSIONS)
        store().rememberBrowser("packs", position)
        val saved = store().load()
        assertEquals(position, saved.browsers["packs"])
        assertEquals("Tavi · Mouths", saved.items["mouths"]?.displayName)
        assertTrue(saved.items["mouths"]!!.favourite)
    }

    @Test fun `independent writers retain each others edits`() {
        val first = store()
        val second = store()
        first.updateItem("a") { it.copy(favourite = true) }
        second.updateItem("b") { it.copy(collection = "School") }
        first.rememberBrowser("packs", BrowserPosition(BrowseMode.FAVOURITES))
        assertEquals(setOf("a", "b"), second.load().items.keys)
        assertEquals(BrowseMode.FAVOURITES, second.load().browsers["packs"]?.mode)
    }

    @Test fun `removing collection changes only membership never asset files`() {
        val artwork = File(root, "artwork.png").apply { writeText("keep") }
        store().updateItem("a") { it.copy(collection = "School", favourite = true) }
        store().rememberBrowser("packs", BrowserPosition(BrowseMode.COLLECTIONS, "School"))
        store().removeCollection("School")
        assertEquals("General Essentials", store().load().items["a"]?.collection)
        assertTrue(store().load().items["a"]!!.favourite)
        assertNull(store().load().browsers["packs"]?.collection)
        assertEquals("keep", artwork.readText())
    }

    @Test fun `corrupt organisation is reported and never silently overwritten`() {
        val file = File(root, "organisation.json").apply { writeText("broken") }
        assertThrows(Exception::class.java) { store().updateItem("a") { it.copy(favourite = true) } }
        assertEquals("broken", file.readText())
    }
}
