package com.donnclab.kinetiframe.ui.library

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class LibraryOpenTargetTest {
    @Test fun `working documents open their actual editors`() {
        assertEquals(LibraryOpenTarget.Project("film"), libraryOpenTarget("project:film"))
        assertEquals(LibraryOpenTarget.Actor("tavi"), libraryOpenTarget("actor:tavi"))
        assertEquals(LibraryOpenTarget.Project("film"), libraryOpenTarget("performance:film:wave"))
    }

    @Test fun `reusable assets and incomplete ids do not masquerade as editable documents`() {
        listOf(
            "PERSONAL::item",
            "project:",
            "actor:",
            "performance:film:",
            "performance:film",
            "unknown:item",
        ).forEach {
            assertNull(libraryOpenTarget(it))
        }
    }
}
