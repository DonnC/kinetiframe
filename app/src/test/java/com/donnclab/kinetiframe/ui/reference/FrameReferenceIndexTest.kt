package com.donnclab.kinetiframe.ui.reference

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class FrameReferenceIndexTest {
    private class Guide(var bytes: Long = 0)

    @Test fun `each frame owns an independent slot`() {
        val cache = FrameReferenceIndex<Guide>(100) { it.bytes }
        val first = cache.getOrPut(0) { Guide(20) }
        assertSame(first, cache.getOrPut(0) { Guide() })
        assertNotSame(first, cache.getOrPut(1) { Guide() })
    }

    @Test fun `insert moves references without copying them to new frames`() {
        val cache = FrameReferenceIndex<Guide>(100) { it.bytes }
        val original = cache.getOrPut(1) { Guide(20) }
        cache.insert(1)
        assertSame(original, cache.getOrPut(2) { Guide() })
        assertNotSame(original, cache.getOrPut(1) { Guide() })
    }

    @Test fun `delete releases only deleted slot and shifts later slots`() {
        val cache = FrameReferenceIndex<Guide>(100) { it.bytes }
        val deleted = cache.getOrPut(0) { Guide(20) }
        val retained = cache.getOrPut(2) { Guide(20) }
        assertSame(deleted, cache.delete(0))
        assertFalse(cache.contains(deleted))
        assertSame(retained, cache.getOrPut(1) { Guide() })
    }

    @Test fun `replacement accounts for other frames without evicting`() {
        val cache = FrameReferenceIndex<Guide>(100) { it.bytes }
        val first = cache.getOrPut(0) { Guide(40) }
        val second = cache.getOrPut(1) { Guide(30) }
        assertTrue(cache.canReplace(first, 70))
        assertFalse(cache.canReplace(first, 71))
        assertTrue(cache.contains(second))
        second.bytes = 0
        assertTrue(cache.canReplace(first, 100))
    }

    @Test fun `clear invalidates old import targets`() {
        val cache = FrameReferenceIndex<Guide>(100) { it.bytes }
        val old = cache.getOrPut(0) { Guide(40) }
        assertEquals(listOf(old), cache.clear())
        assertFalse(cache.canReplace(old, 1))
        assertNotSame(old, cache.getOrPut(0) { Guide() })
    }
}
