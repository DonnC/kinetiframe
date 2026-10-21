package com.donnclab.kinetiframe.ui

import com.donnclab.kinetiframe.ui.packs.PackFrameTag
import com.donnclab.kinetiframe.ui.packs.reindexPackTags
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class PackTagIndexTest {
    @Test fun `inserting leaves a metadata gap without renaming existing drawings`() {
        val tags = mapOf(0 to PackFrameTag("Head"), 1 to PackFrameTag("Hand"))
        assertEquals(mapOf(0 to tags.getValue(0), 2 to tags.getValue(1)), reindexPackTags(tags, 1, emptyList()))
    }

    @Test fun `multiple deletions remove tags and shift remaining drawings in descending order`() {
        val tags = (0..4).associateWith { PackFrameTag("Item $it") }
        assertEquals(
            mapOf(0 to tags.getValue(0), 1 to tags.getValue(2), 2 to tags.getValue(4)),
            reindexPackTags(tags, null, listOf(1, 3)),
        )
    }
}
