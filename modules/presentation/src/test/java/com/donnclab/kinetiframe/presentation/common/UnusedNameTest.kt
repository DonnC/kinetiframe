// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** R3-10 and R3-35: suggested names for collections and performances are ones not already in use. */
class UnusedNameTest {
    @Test fun `the base name when it is free`() {
        assertEquals("My series", unusedName("My series", setOf("Shorts")))
    }

    @Test fun `the first free number when the base is taken`() {
        assertEquals("My series 3", unusedName("My series", setOf("my series", "My series 2")))
    }

    @Test fun `surrounding spaces are ignored`() {
        assertEquals("New performance 2", unusedName(" New performance ", listOf("New performance")))
    }
}
