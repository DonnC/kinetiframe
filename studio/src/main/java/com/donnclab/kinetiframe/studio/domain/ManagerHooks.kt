// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import com.donnclab.kinetiframe.studio.StudioEffect
import com.donnclab.kinetiframe.studio.StudioState
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope

/** Where a studio manager runs its work, and how it reads and changes the studio state. */
data class StudioRuntime(
    val scope: CoroutineScope,
    val dispatcher: CoroutineDispatcher,
    val getState: () -> StudioState,
    val updateState: ((StudioState) -> StudioState) -> Unit,
    val pending: PendingOperations = PendingOperations(),
)

/** The undo and redo stacks that drawing and selection edits share. */
data class UndoStacks(val history: MutableList<HistoryItem>, val redo: MutableList<HistoryItem> = mutableListOf())

/** What [CelHoldManager] calls back into. */
data class CelHoldHooks(
    val sendEffect: (StudioEffect) -> Unit,
    val finalizeDrawing: suspend () -> Unit,
    val clearHistory: () -> Unit,
    val loadFrame: suspend (Int) -> Unit,
)

/** How [DrawingManager] reaches the active layer's bitmaps. */
data class DrawingLayers(
    val getBaseBitmap: () -> Bitmap?,
    val setBaseBitmap: (Bitmap?) -> Unit,
    val activeLayerId: () -> String,
    val composeFrame: suspend (frameIndex: Int, layerId: String, layerBitmap: Bitmap?) -> Bitmap?,
    val acquireBitmap: (Int, Int) -> Bitmap,
    val releaseBitmap: (Bitmap?) -> Unit,
    /** Fired after a layer's frame bitmap settles (stroke bake, undo/redo, ...). Drives "tap a
     *  bone, then sketch" auto-binding; see `RiggingManager.autoBindLayerContent`. */
    val onFrameBaked: (layerId: String, bitmap: Bitmap) -> Unit = { _, _ -> },
)

/** What [FrameManager] calls back into. */
data class FrameHooks(
    val sendEffect: (StudioEffect) -> Unit,
    val loadFrameInternal: suspend (Int) -> Unit,
    val finalizeFrame: suspend (Int) -> Unit,
    val clearHistory: () -> Unit,
    /** UI-only guides must be remapped in the same publication turn as the frame indices. Null
     *  applies the change directly. */
    val commitFrameStructure: (suspend (Int?, List<Int>, (StudioState) -> StudioState) -> Unit)? = null,
)

/** What [PlaybackManager] calls back into. */
data class PlaybackHooks(
    val loadFrame: (Int) -> Unit,
    val composeFrame: suspend (Int, Bitmap?) -> Bitmap?,
    val activeLayerId: () -> String,
    /** Keeps audio playback in step with frame playback without this class knowing about it. */
    val onFrameChanged: (Int) -> Unit = {},
    val onPlaybackStateChanged: (Boolean) -> Unit = {},
    val onPlaybackError: (Exception) -> Unit = {},
)

/** What [SelectionManager] calls back into. */
data class SelectionHooks(
    val rebuildCurrentBitmap: suspend () -> Unit,
    val snapshotActiveLayer: suspend () -> Bitmap?,
    val setBaseBitmap: (Bitmap?) -> Unit,
    val trimHistory: suspend () -> Unit = {},
)
