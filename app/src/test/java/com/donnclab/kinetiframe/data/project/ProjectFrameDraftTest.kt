package com.donnclab.kinetiframe.data.project

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.nio.file.Files

class ProjectFrameDraftTest {
    @Test fun `editing and discarding draft leaves saved cels untouched`() {
        val root = Files.createTempDirectory("frame-draft-test").toFile()
        val saved = root.resolve("saved").apply { mkdirs() }
        saved.resolve("frame.png").writeText("original")
        val draft = root.resolve("draft")
        ProjectFrameDraft.open(saved, draft)
        draft.resolve("frame.png").writeText("edited")
        assertEquals("original", saved.resolve("frame.png").readText())
        ProjectFrameDraft.publish(draft, saved)
        assertEquals("edited", saved.resolve("frame.png").readText())
    }

    @Test fun `draft cannot overwrite its source at open`() {
        val root = Files.createTempDirectory("frame-draft-test").toFile()
        assertThrows(IllegalArgumentException::class.java) { ProjectFrameDraft.open(root, root) }
    }
}
