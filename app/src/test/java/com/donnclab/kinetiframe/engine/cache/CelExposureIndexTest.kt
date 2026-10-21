package com.donnclab.kinetiframe.engine.cache

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class CelExposureIndexTest {
    @Test fun `inserting an empty exposure shifts links without changing cel identity`() {
        assertEquals(
            mapOf(0 to "a", 2 to "a", 3 to "b"),
            CelExposureIndex.insert(mapOf(0 to "a", 1 to "a", 2 to "b"), 1),
        )
    }

    @Test fun `deleting first exposure preserves its surviving cel owner`() {
        assertEquals(
            mapOf(0 to "a", 1 to "a"),
            CelExposureIndex.delete(mapOf(0 to "a", 1 to "a", 2 to "a"), 0),
        )
    }

    @Test fun `holding a range replaces only that range`() {
        assertEquals(
            mapOf(0 to "a", 1 to "b", 2 to "b", 3 to "a"),
            CelExposureIndex.link((0..3).associateWith { "a" }, 1, 2, "b"),
        )
    }

    @Test fun `unsafe cel names and out of bounds exposures are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            CelExposureIndex.validate(mapOf("main" to mapOf(0 to "../pixels")), 2)
        }
        assertThrows(IllegalArgumentException::class.java) {
            CelExposureIndex.validate(mapOf("main" to mapOf(2 to "cel-a")), 2)
        }
        CelExposureIndex.validate(emptyMap(), 2) // Legacy documents remain valid.
    }
}
