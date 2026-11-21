package com.donnclab.kinetiframe.data.storage

import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DocumentFileStoreTest {
    @TempDir lateinit var root: File

    @Test fun `storage IDs cannot escape their resource root`() {
        val store = DocumentFileStore(root)
        assertThrows(IllegalArgumentException::class.java) { store.projectDirectory("../outside") }
        assertThrows(IllegalArgumentException::class.java) { store.libraryDirectory("../../outside") }
    }
}
