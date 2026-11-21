package com.donnclab.kinetiframe.studio.reference

/** Told when frames are inserted, deleted or the studio closes, so per-frame UI resources follow. */
interface FrameStructureListener {
    fun insert(frame: Int)
    fun delete(frame: Int)
    fun clear()
}
