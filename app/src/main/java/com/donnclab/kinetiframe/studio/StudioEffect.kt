package com.donnclab.kinetiframe.studio

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.donnclab.kinetiframe.actors.rig.model.PivotPlacement
import com.donnclab.kinetiframe.project.model.BaseNode
import com.donnclab.kinetiframe.project.model.DrawCommand
import com.donnclab.kinetiframe.project.model.DrawingTool
import com.donnclab.kinetiframe.project.model.Layer
import com.donnclab.kinetiframe.project.model.LayerType
import com.donnclab.kinetiframe.project.model.ProjectDocument
import com.donnclab.kinetiframe.project.model.TextNode
import com.donnclab.kinetiframe.project.model.TouchPoint
import com.donnclab.kinetiframe.studio.drawing.BrushId
import com.donnclab.kinetiframe.studio.drawing.BrushSnapshot
import com.donnclab.kinetiframe.studio.drawing.ConstructionShape
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState

/**
 * One-off events the studio shows once, such as an error or a text dialog.
 */
sealed class StudioEffect : ViewEffect {
    data class ShowError(val message: String) : StudioEffect()
    data class ShowTextEditDialog(val id: String, val initialText: String) : StudioEffect()
    data class ShowTextAddDialog(val x: Float, val y: Float) : StudioEffect()
}
