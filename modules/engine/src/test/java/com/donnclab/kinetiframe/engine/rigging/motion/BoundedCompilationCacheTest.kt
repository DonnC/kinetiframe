// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class BoundedCompilationCacheTest {
    private data class Entry(val id: Int)

    @Test fun `hits do not compile or refresh FIFO order and clear drops entries`() {
        val cache = BoundedCompilationCache<Entry>(2)
        var compilations = 0
        fun entry(id: Int) = cache.getOrPut({ it.id == id }) {
            compilations++
            Entry(id)
        }
        val first = entry(1)
        entry(2)
        assertSame(first, entry(1))
        assertEquals(2, compilations)
        entry(3)
        entry(1)
        assertEquals(4, compilations)
        cache.clear()
        entry(1)
        assertEquals(5, compilations)
    }

    @Test fun `failed compilation never publishes a cache entry`() {
        val cache = BoundedCompilationCache<Entry>(1)
        val failure = IllegalStateException()
        assertSame(
            failure,
            assertThrows(IllegalStateException::class.java) {
                cache.getOrPut({ true }) { throw failure }
            },
        )
        assertEquals(Entry(1), cache.getOrPut({ true }) { Entry(1) })
    }

    @Test fun `invalid limits expose typed failure`() {
        assertEquals(
            CompilationCacheIssue.INVALID_CAPACITY,
            assertThrows(CompilationCacheException::class.java) {
                BoundedCompilationCache<Entry>(0)
            }.issue,
        )
    }
}
