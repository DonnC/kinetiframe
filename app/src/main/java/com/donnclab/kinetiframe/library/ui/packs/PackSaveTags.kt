package com.donnclab.kinetiframe.library.ui.packs

import com.donnclab.kinetiframe.library.model.PackFrameTag

/** Keep existing metadata, provide usable names for new cells, omit deleted cells. */
fun packSaveTags(totalFrames: Int, tags: Map<Int, PackFrameTag>): Map<Int, PackFrameTag> {
    require(totalFrames > 0)
    return (0 until totalFrames).associateWith { index ->
        val tag = tags[index] ?: PackFrameTag()
        tag.copy(name = tag.name.ifBlank { "Item ${index + 1}" })
    }
}
