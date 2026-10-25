package com.donnclab.kinetiframe.library.model

/** Keep metadata attached to its drawing during the same atomic commit as pixel reindexing. */
fun reindexPackTags(tags: Map<Int, PackFrameTag>, inserted: Int?, deleted: List<Int>): Map<Int, PackFrameTag> {
    var result = tags
    for (index in deleted.sortedDescending()) {
        result = result.filterKeys { it != index }.mapKeys { (key, _) -> if (key > index) key - 1 else key }
    }
    return if (inserted == null) result else result.mapKeys { (key, _) -> if (key >= inserted) key + 1 else key }
}
