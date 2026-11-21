package com.donnclab.kinetiframe.presentation.library.packs

import com.donnclab.kinetiframe.library.model.PackFrameTag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PackSaveTagsTest {
    @Test fun `new cells have distinct nonblank names`() {
        assertEquals(listOf("Item 1", "Item 2", "Item 3"), packSaveTags(3, emptyMap()).values.map { it.name })
    }

    @Test fun `existing tags stay intact and deleted frame tags are omitted`() {
        val original = PackFrameTag("Pip torso", "Reusable torso")
        val result = packSaveTags(1, mapOf(0 to original, 1 to PackFrameTag("Deleted")))
        assertEquals(mapOf(0 to original), result)
    }

    @Test fun `empty packs are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { packSaveTags(0, emptyMap()) }
    }
}
