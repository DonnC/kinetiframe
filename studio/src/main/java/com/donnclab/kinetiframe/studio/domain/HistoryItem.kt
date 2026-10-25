package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import com.donnclab.kinetiframe.core.document.BaseNode
import com.donnclab.kinetiframe.core.document.DrawCommand

sealed class HistoryItem {
    data class Stroke(val command: DrawCommand) : HistoryItem()

    /** Commands produced by one user gesture (for example, a mirrored stroke). */
    data class StrokeBatch(val commands: List<DrawCommand>) : HistoryItem()

    data class SelectionEdit(
        val bitmap: Bitmap?,
        val nodes: Map<String, BaseNode>,
        val matrix: Matrix,
        val originalBounds: Rect,
        val type: SelectionEditType,
    ) : HistoryItem()

    data class ClearArea(val path: Path?, val rect: Rect?, val backupBitmap: Bitmap? = null) : HistoryItem()

    /** Metadata only: never duplicates actor artwork or performance data. */
    data class ObjectEdit(
        val beforeActors: Map<String, com.donnclab.kinetiframe.core.document.ActorPlacementReference>,
        val afterActors: Map<String, com.donnclab.kinetiframe.core.document.ActorPlacementReference>,
        val beforeNodes: Map<String, BaseNode>,
        val afterNodes: Map<String, BaseNode>,
        val beforeVideos: Map<String, com.donnclab.kinetiframe.core.document.VideoClipPlacement> = emptyMap(),
        val afterVideos: Map<String, com.donnclab.kinetiframe.core.document.VideoClipPlacement> = emptyMap(),
        val beforeAudio: Map<String, com.donnclab.kinetiframe.core.document.AudioClipPlacement> = emptyMap(),
        val afterAudio: Map<String, com.donnclab.kinetiframe.core.document.AudioClipPlacement> = emptyMap(),
    ) : HistoryItem()

    /** A pixel move is one undo step: source removal and transformed destination together. */
    data class RasterSelection(val clear: ClearArea?, val stamp: SelectionEdit?) : HistoryItem()
}

enum class SelectionEditType {
    COMMIT,
    DELETE,
    PASTE,
}
