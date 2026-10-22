package com.donnclab.kinetiframe.studio.render

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SharedResourceCacheTest {
    private data class Resource(val bytes: Long)

    @Test fun `three full body rigs and recent expression swaps fit slots while remaining byte bounded`() {
        val cache = SharedResourceCache<String, Resource>(96, 24L * 1024 * 1024) { it.bytes }
        repeat(48) { part -> cache.getOrLoad("part-$part") { Resource(128 * 1024L) } }
        repeat(24) { expression -> cache.getOrLoad("expression-$expression") { Resource(32 * 1024L) } }
        repeat(48) { part -> assertNotNull(cache.getOrLoad("part-$part") { error("Static part lost") }) }
        repeat(24) { expression ->
            assertNotNull(cache.getOrLoad("expression-$expression") { error("Recent expression lost") })
        }
        assertEquals(72, cache.retainedEntries)
        assertTrue(cache.retainedBytes <= 24L * 1024 * 1024)
    }

    @Test fun `eviction reuses resources still owned by a displayed frame`() {
        val cache = SharedResourceCache<String, Resource>(1, 100) { it.bytes }
        val displayed = cache.getOrLoad("first") { Resource(40) }
        cache.getOrLoad("next") { Resource(40) }
        assertEquals(1, cache.retainedEntries)
        assertSame(displayed, cache.getOrLoad("first") { error("A live frame still owns this resource") })
    }

    @Test fun `byte budget evicts before entry limit and oversized resources are not retained`() {
        val cache = SharedResourceCache<String, Resource>(48, 100) { it.bytes }
        cache.getOrLoad("first") { Resource(60) }
        cache.getOrLoad("second") { Resource(60) }
        assertEquals(60L, cache.retainedBytes)
        assertEquals(1, cache.retainedEntries)
        val oversized = cache.getOrLoad("large") { Resource(200) }
        assertEquals(60L, cache.retainedBytes)
        assertSame(oversized, cache.getOrLoad("large") { error("Do not decode a live oversized asset twice") })
    }

    @Test fun `clear drops ownership and missing resources can be retried`() {
        val cache = SharedResourceCache<String, Resource>(1, 100) { it.bytes }
        assertNull(cache.getOrLoad("missing") { null })
        val displayed = cache.getOrLoad("missing") { Resource(30) }
        cache.clear()
        assertEquals(0L, cache.retainedBytes)
        assertEquals(0, cache.retainedEntries)
        assertEquals(30L, displayed!!.bytes)
        assertNotSame(displayed, cache.getOrLoad("missing") { Resource(30) })
    }
}
