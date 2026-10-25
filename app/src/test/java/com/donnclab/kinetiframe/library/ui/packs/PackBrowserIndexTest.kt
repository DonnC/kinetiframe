package com.donnclab.kinetiframe.library.ui.packs

import com.donnclab.kinetiframe.core.actor.document.LibraryItemKind
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.library.model.AssetCategory
import com.donnclab.kinetiframe.library.model.AssetOrganization
import com.donnclab.kinetiframe.library.model.BrowseMode
import com.donnclab.kinetiframe.library.model.BrowserPosition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PackBrowserIndexTest {
    private val mouths = LibraryItemManifest(
        libraryItemId = "ls-tavi-mouths-v2",
        kind = LibraryItemKind.PACK,
        displayName = "Tavi mouths",
        createdAtEpochMs = 1,
    )
    private val hands = mouths.copy(libraryItemId = "hands", displayName = "Hand poses", createdAtEpochMs = 2)

    @Test fun `search matches item names tags and personal names not just pack titles`() {
        val names = mapOf(hands.libraryItemId to listOf("Pointing hand", "Fist"))
        assertEquals(
            listOf(hands),
            PackBrowserIndex.filter(listOf(mouths, hands), names, emptyMap(), BrowserPosition(), "pointing hand"),
        )
        val meta = mapOf(hands.libraryItemId to AssetOrganization(displayName = "Reporter gestures"))
        assertEquals(
            listOf(hands),
            PackBrowserIndex.filter(listOf(mouths, hands), names, meta, BrowserPosition(), "reporter"),
        )
        assertTrue(PackBrowserIndex.filter(listOf(hands), names, meta, BrowserPosition(), "pointing elbow").isEmpty())
    }

    @Test fun `category collection and favourites combine without merging assets`() {
        val meta = mapOf(mouths.libraryItemId to PackBrowserIndex.defaults(mouths).copy(favourite = true))
        val position = BrowserPosition(BrowseMode.FAVOURITES, "Loose Signal", AssetCategory.EXPRESSIONS)
        assertEquals(listOf(mouths), PackBrowserIndex.filter(listOf(mouths, hands), emptyMap(), meta, position, ""))
    }

    @Test fun `recent uses last use before creation and scoped IDs do not collide`() {
        val meta = mapOf(mouths.libraryItemId to AssetOrganization(lastUsedEpochMs = 100))
        assertEquals(
            mouths,
            PackBrowserIndex.filter(listOf(hands, mouths), emptyMap(), meta, BrowserPosition(), "").first(),
        )
        val projectPack = mouths.copy(scope = LibraryScope.PROJECT, sourceProjectId = "episode")
        assertNotEquals(PackBrowserIndex.key(mouths), PackBrowserIndex.key(projectPack))
        assertEquals(
            listOf(mouths),
            PackBrowserIndex.filter(
                listOf(mouths, projectPack),
                emptyMap(),
                mapOf(PackBrowserIndex.key(mouths) to AssetOrganization(favourite = true)),
                BrowserPosition(BrowseMode.FAVOURITES),
                "",
            ),
        )
    }
}
