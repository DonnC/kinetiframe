package com.donnclab.kinetiframe.ui.actors.sketch

import android.graphics.Bitmap
import com.donnclab.kinetiframe.engine.model.TouchPoint
import com.donnclab.kinetiframe.engine.rigging.model.ActorDrawing
import com.donnclab.kinetiframe.engine.rigging.model.ActorDrawingRegion
import com.donnclab.kinetiframe.engine.rigging.model.DrawingPoint
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState

enum class SketchLayer { LINEWORK, COLOUR }
enum class SketchTool { PEN, ERASER, FILL, BIND_REGION, JOINT }
data class ActorSketchState(
    val ink: Bitmap? = null,
    val colour: Bitmap? = null,
    val layer: SketchLayer = SketchLayer.LINEWORK,
    val tool: SketchTool = SketchTool.PEN,
    val paintColour: Int = android.graphics.Color.BLACK,
    val size: Float = 4f,
    val revision: Int = 0,
    val busy: Boolean = true,
    val dirty: Boolean = false,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val regions: List<ActorDrawingRegion> = emptyList(),
    val bindingPartId: String? = null,
    val graph: com.donnclab.kinetiframe.engine.rigging.model.SceneGraph? = null,
) : ViewState
sealed interface ActorSketchIntent : ViewIntent {
    data class Layer(val value: SketchLayer) : ActorSketchIntent
    data class Tool(val value: SketchTool) : ActorSketchIntent
    data class Colour(val value: Int) : ActorSketchIntent
    data class Size(val value: Float) : ActorSketchIntent
    data class Stroke(val points: List<TouchPoint>) : ActorSketchIntent
    data class Fill(val x: Int, val y: Int) : ActorSketchIntent
    data class BindingPart(val id: String) : ActorSketchIntent
    data class BindRegion(val outline: List<DrawingPoint>) : ActorSketchIntent
    data class ClearRegions(val partId: String) : ActorSketchIntent
    data class MoveJoint(val partId: String, val start: Boolean, val target: androidx.compose.ui.geometry.Offset) :
        ActorSketchIntent
    data object Undo : ActorSketchIntent
    data object Redo : ActorSketchIntent
    data object Bind : ActorSketchIntent
}
sealed interface ActorSketchEffect : ViewEffect {
    data class Error(val message: String) : ActorSketchEffect
    data class Bound(
        val ink: Bitmap,
        val colour: Bitmap,
        val drawing: ActorDrawing,
        val restGraph: com.donnclab.kinetiframe.engine.rigging.model.SceneGraph,
    ) : ActorSketchEffect
}
