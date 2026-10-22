package com.donnclab.kinetiframe.library.ui.packs

import com.donnclab.kinetiframe.library.data.PackItemEntry
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PackFrameTagTest {
    @Test fun `dialog accepts the maximum lengths supported by exported packs`() {
        val tag =
            PackFrameTag("n".repeat(PackItemEntry.MAX_NAME_LENGTH), "d".repeat(PackItemEntry.MAX_DESCRIPTION_LENGTH))
        assertTrue(tag.isValid)
        assertDoesNotThrow { PackItemEntry("frame_0.png", 0, tag.name, tag.description).validate() }
    }

    @Test fun `blank or oversized metadata cannot enable confirmation`() {
        assertFalse(PackFrameTag(" ").isValid)
        assertFalse(PackFrameTag("n".repeat(41)).isValid)
        assertFalse(PackFrameTag("Name", "d".repeat(201)).isValid)
    }
}
