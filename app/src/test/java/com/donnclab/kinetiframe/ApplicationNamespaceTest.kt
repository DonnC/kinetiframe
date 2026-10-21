package com.donnclab.kinetiframe

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ApplicationNamespaceTest {
    @Test
    fun `generated resources belong to the owned application namespace`() {
        assertEquals("com.donnclab.kinetiframe", R::class.java.packageName)
    }
}
