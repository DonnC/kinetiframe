// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.canvas

import android.graphics.Bitmap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import com.donnclab.kinetiframe.core.document.BaseNode
import com.donnclab.kinetiframe.core.document.BrushSnapshot
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.core.document.TouchPoint
import com.donnclab.kinetiframe.studio.EraserType
import com.donnclab.kinetiframe.studio.RulerGuide
import com.donnclab.kinetiframe.studio.SelectionToolType
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioMode
import com.donnclab.kinetiframe.studio.drawing.ConstructionShape
import com.donnclab.kinetiframe.studio.drawing.FontRegistry

/** The page being drawn on: its size in document pixels, where it sits on screen, and the screen's density. */
data class CanvasSheet(
    val width: Int,
    val height: Int,
    val matrix: android.graphics.Matrix,
    val displayDensity: Float,
    /** Draws the paper's shadow and border; off when the page sits inside another surface. */
    val paperDecoration: Boolean = true,
)

/** Neighbouring frames drawn faintly behind the current one, each side with its own tint. */
data class OnionSkinLayers(
    val enabled: Boolean,
    val past: List<OnionFrame>,
    val future: List<OnionFrame>,
    val opacity: Float,
    val pastTint: ColorFilter?,
    val futureTint: ColorFilter?,
)

/** What the current frame shows under the stroke being drawn, from the back forward. */
data class CanvasContent(
    /** The project's persistent background, scaled to cover the sheet. Null when none is set. */
    val background: Bitmap?,
    val currentFrame: Bitmap?,
    val nodes: Map<String, BaseNode>,
    val fontRegistry: FontRegistry,
    /** The video reference track's frame at the playhead, drawn over the background at
     *  [videoOpacity] so it can be traced. */
    val videoFrame: Bitmap? = null,
    val videoOpacity: Float = 0.5f,
    val referenceUnderlay: ((android.graphics.Canvas) -> Unit)? = null,
)

/** The stroke under the user's finger and the tool settings it is drawn with. */
data class StrokePreview(
    val stroke: ActiveStroke,
    val tool: DrawingTool,
    val eraserType: EraserType,
    val width: Float,
    val pressurePaint: android.graphics.Paint,
    /** Erasing draws into an offscreen layer so it cuts through the frame's pixels only. */
    val useSaveLayer: Boolean,
    val alpha: Float = 1f,
    val strength: Float = 0.5f,
    val brush: BrushSnapshot = BrushSnapshot(),
)

/** The outline of the area being selected. */
data class SelectionOutline(val path: Path, val paint: android.graphics.Paint, val rect: Rect?)

/** Where the stage sits on screen, to map touches into document pixels. */
data class InputViewport(val stage: StageDimensions, val canvasMatrixValues: FloatArray, val displayDensity: Float)

/** The tool a touch drives, and its settings. */
data class InputTool(
    val studioMode: StudioMode,
    val activeTool: DrawingTool,
    val selectionTool: SelectionToolType,
    val rulerGuide: RulerGuide?,
    val pressureSensitive: Boolean = false,
    val constructionShape: ConstructionShape? = null,
    val constructionPreviewPending: Boolean = false,
)

/** What touches read and draw into: the frame under the finger, the scene's nodes, and the live
 *  stroke and selection. */
data class InputTargets(
    val sourceFrame: Bitmap?,
    val nodes: Map<String, BaseNode>,
    val fontRegistry: FontRegistry?,
    val selectionActive: Boolean,
    val activeStroke: ActiveStroke,
    val selectionPath: Path,
    /** Raw pixels of the selected layer. Manipulation must never sample the composited frame. */
    val manipulationSource: Bitmap? = null,
    val manipulationWidth: Float = 0f,
    val manipulationStrength: Float = 0f,
)

/** Where [CanvasInputLayer] reports strokes, selections, the eraser cursor and studio intents. */
data class InputCallbacks(
    val onIntent: (StudioIntent) -> Unit,
    val onStrokeSettled: (List<TouchPoint>) -> Unit,
    val onStrokeComplete: (List<TouchPoint>) -> Unit,
    val onSelectionRectChanged: (Rect?) -> Unit,
    val onTextNodeSelect: (String?) -> Unit,
    val onSelectionAreaComplete: (Path?, Rect?, Float, FontRegistry?) -> Unit,
    val onEraserCursorChange: (Offset?) -> Unit,
    val onConstructionPreview: (List<List<TouchPoint>>) -> Unit = {},
    val onRigJointHit: ((Offset) -> String?)? = null,
)
