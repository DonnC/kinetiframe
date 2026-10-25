package com.donnclab.kinetiframe.library.ui

import com.donnclab.kinetiframe.library.model.AssetOrganization
import com.donnclab.kinetiframe.library.model.BrowseMode
import com.donnclab.kinetiframe.library.model.BrowserPosition
import com.donnclab.kinetiframe.library.model.LibraryOrganization
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HierarchyBrowserIndexTest {
    private data class Entry(val id: String, val name: String, val edited: Long)
    private val entries = listOf(Entry("actor:a", "Tavi", 10), Entry("actor:b", "Guest", 20))
    private val organization = LibraryOrganization(
        items = mapOf(
            "actor:a" to AssetOrganization(collection = "Loose Signal", favourite = true, lastUsedEpochMs = 30),
            "project:a" to AssetOrganization(collection = "Education", favourite = true),
        ),
    )
    private fun browse(position: BrowserPosition, query: String = "") = HierarchyBrowserIndex.filter(entries, {
        it.id
    }, { it.name }, { it.edited }, organization, position, query)

    @Test fun `recent uses last opened without rewriting edited timestamp`() {
        assertEquals(listOf("Tavi", "Guest"), browse(BrowserPosition()).map { it.name })
        assertEquals(10L, entries.first().edited)
    }

    @Test fun `favourites and collections respect resource namespaces`() {
        assertEquals(listOf("Tavi"), browse(BrowserPosition(BrowseMode.FAVOURITES)).map { it.name })
        assertTrue(browse(BrowserPosition(BrowseMode.COLLECTIONS, "Education")).isEmpty())
        assertEquals(listOf("Tavi"), browse(BrowserPosition(BrowseMode.COLLECTIONS, "Loose Signal")).map { it.name })
        assertEquals(
            listOf("General Essentials", "Loose Signal"),
            HierarchyBrowserIndex.collections(entries, {
                it.id
            }, organization),
        )
    }

    @Test fun `search is trimmed case insensitive and stable alphabetically`() {
        assertEquals(listOf("Tavi"), browse(BrowserPosition(BrowseMode.ALL), " tAV ").map { it.name })
        assertEquals(listOf("Guest", "Tavi"), browse(BrowserPosition(BrowseMode.ALL)).map { it.name })
    }
}
