package com.donnclab.kinetiframe.studio.reference

import android.graphics.Bitmap
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.donnclab.kinetiframe.actors.ui.ActorReferenceSession

/** Main-thread UI resources, never part of StudioState, disk caches or saved-state handles. */
@Stable
class FrameReferenceSession(heapBytes: Long = Runtime.getRuntime().maxMemory()) {
    private val index = FrameReferenceIndex<ActorReferenceSession>(minOf(16L * 1024 * 1024, heapBytes / 16)) {
        it.bitmap?.allocationByteCount?.toLong() ?: 0L
    }
    private var revision by mutableIntStateOf(0)
    fun frame(frame: Int): ActorReferenceSession {
        revision // Observe structural remapping in composition.
        return index.getOrPut(frame) { ActorReferenceSession() }
    }
    fun replace(session: ActorReferenceSession, image: Bitmap, width: Int, height: Int): Boolean {
        if (!index.canReplace(session, image.allocationByteCount.toLong())) return false
        session.replace(image, width, height)
        return true
    }
    fun insert(frame: Int) {
        index.insert(frame)
        revision++
    }
    fun delete(frame: Int) {
        index.delete(frame)?.remove()
        revision++
    }
    fun clear() {
        index.clear().forEach { it.remove() }
        revision++
    }
}
