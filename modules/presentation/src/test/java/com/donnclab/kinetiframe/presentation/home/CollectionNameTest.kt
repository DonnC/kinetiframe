// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.home

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** R3-10: a new collection's suggested name is one not already in use. */
class CollectionNameTest {
    @Test fun `the base name when it is free`() {
        assertEquals("My series", unusedCollectionName("My series", setOf("Shorts")))
    }

    @Test fun `the first free number when the base is taken`() {
        assertEquals("My series 3", unusedCollectionName("My series", setOf("my series", "My series 2")))
    }
}
