package com.donnclab.kinetiframe.actors.ui.sketch

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.core.actor.artwork.DrawingPoint
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.project.model.TouchPoint
import kotlin.math.hypot

/** One-finger drawing/stylus input; two fingers pan/zoom without leaving ink. No heavy objects
 * are created in the draw callback. Coordinates remain in the editable source, never posed space. */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
@Composable
internal fun ActorSketchCanvas(
    state: ActorSketchState,
    graph: SceneGraph,
    bounds: FloatArray,
    showBones: Boolean,
    onIntent: (ActorSketchIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val ink = remember(state.ink) { state.ink?.asImageBitmap() }
    val colour = remember(state.colour) { state.colour?.asImageBitmap() }
    val width = ink?.width ?: 1
    val height = ink?.height ?: 1
    val regionPaths = remember(state.regions, width, height) {
        state.regions.map { region ->
            androidx.compose.ui.graphics.Path().apply {
                region.outline.forEachIndexed { index, p ->
                    if (index ==
                        0
                    ) {
                        moveTo(p.x * width, p.y * height)
                    } else {
                        lineTo(p.x * width, p.y * height)
                    }
                }
                close()
            }
        }
    }
    val joints = remember(graph, bounds.toList(), width, height) {
        RigGeometry.resolve(graph).map { (id, joint) ->
            fun source(p: Offset) =
                Offset((p.x - bounds[0]) * width / bounds[2], (p.y - bounds[1]) * height / bounds[3])
            Triple(id, source(joint.start), source(joint.end))
        }
    }
    val density = LocalDensity.current
    val hitRadius = with(density) { 24.dp.toPx() }
    val handleRadius = with(density) { 6.dp.toPx() }
    val tieTolerance = with(density) { 3.dp.toPx() }
    var jointDrag by remember { mutableStateOf<Pair<String, Boolean>?>(null) }
    var jointTarget by remember { mutableStateOf<Offset?>(null) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var points by remember { mutableStateOf<List<TouchPoint>>(emptyList()) }
    var twoFingers by remember { mutableStateOf(false) }
    var lastCentre by remember { mutableStateOf(Offset.Zero) }
    var lastDistance by remember { mutableFloatStateOf(1f) }
    val scale = minOf(size.width.toFloat() / width, size.height.toFloat() / height) * .94f * zoom
    val origin = Offset((size.width - width * scale) / 2f, (size.height - height * scale) / 2f) + pan
    val workspaceColour = androidx.compose.material3.MaterialTheme.colorScheme.background
    Canvas(
        modifier.fillMaxSize().background(workspaceColour).onSizeChanged {
            size = it
        }.pointerInteropFilter { event ->
            if (state.busy || scale <= 0) return@pointerInteropFilter true
            if (event.pointerCount >= 2) {
                val centre = Offset((event.getX(0) + event.getX(1)) / 2, (event.getY(0) + event.getY(1)) / 2)
                val distance = hypot(event.getX(0) - event.getX(1), event.getY(0) - event.getY(1)).coerceAtLeast(1f)
                if (twoFingers) {
                    pan += centre - lastCentre
                    zoom = (zoom * distance / lastDistance).coerceIn(.5f, 8f)
                }
                twoFingers = true
                points = emptyList()
                jointDrag = null
                jointTarget = null
                lastCentre = centre
                lastDistance =
                    distance
                return@pointerInteropFilter true
            }
            fun point() = TouchPoint(
                (event.x - origin.x) / scale,
                (event.y - origin.y) / scale,
                pressure = event.pressure.coerceIn(.05f, 1.5f),
                tilt = event.getAxisValue(MotionEvent.AXIS_TILT),
                orientation = event.getAxisValue(MotionEvent.AXIS_ORIENTATION),
                timestamp = event.eventTime,
            )
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    twoFingers = false
                    if (state.tool == SketchTool.JOINT) {
                        val position = Offset(event.x, event.y)
                        val candidates = joints.flatMap { (id, start, end) ->
                            val bone = graph.bones.getValue(id)
                            // Tiny attachment markers (eyes/mouth/pivots) move position, not length.
                            SketchJointPicker.handles(
                                id,
                                start * scale + origin,
                                end * scale + origin,
                                bone.parentId != null && bone.length <= 2f,
                            )
                        }
                        // Selecting a named part resolves coincident profile/facial handles predictably.
                        jointDrag =
                            SketchJointPicker.pick(candidates, position, hitRadius, tieTolerance, state.bindingPartId)
                                ?.let { it.partId to it.start }
                        jointDrag?.let { onIntent(ActorSketchIntent.BindingPart(it.first)) }
                        jointTarget = jointDrag?.let { Offset(point().x, point().y) }
                    } else {
                        points = listOf(point())
                    }
                }

                MotionEvent.ACTION_MOVE -> if (!twoFingers) {
                    if (state.tool == SketchTool.JOINT) {
                        jointTarget = jointDrag?.let { Offset(point().x, point().y) }
                    } else if (points.size < 8192) {
                        points = points + point()
                    }
                }

                MotionEvent.ACTION_UP -> {
                    jointDrag?.let { (id, start) ->
                        jointTarget?.let { target ->
                            onIntent(
                                ActorSketchIntent.MoveJoint(
                                    id,
                                    start,
                                    Offset(
                                        bounds[0] + target.x * bounds[2] / width,
                                        bounds[1] + target.y * bounds[3] / height,
                                    ),
                                ),
                            )
                        }
                    }
                    jointDrag = null
                    jointTarget = null
                    if (!twoFingers && points.isNotEmpty()) {
                        if (state.tool ==
                            SketchTool.FILL
                        ) {
                            onIntent(ActorSketchIntent.Fill(points[0].x.toInt(), points[0].y.toInt()))
                        } else if (state.tool == SketchTool.BIND_REGION) {
                            val stride = maxOf(1, (points.size + 254) / 255)
                            onIntent(
                                ActorSketchIntent.BindRegion(
                                    points.filterIndexed { index, _ -> index % stride == 0 }
                                        .map { DrawingPoint(it.x / width, it.y / height) },
                                ),
                            )
                        } else {
                            onIntent(ActorSketchIntent.Stroke(points + point()))
                        }
                    }
                    points = emptyList()
                    twoFingers = false
                }

                MotionEvent.ACTION_CANCEL -> {
                    points = emptyList()
                    twoFingers = false
                    jointDrag = null
                    jointTarget =
                        null
                }
            }
            true
        },
    ) {
        withTransform({
            translate(origin.x, origin.y)
            scale(scale, scale, Offset.Zero)
        }) {
            drawRect(Color.White, size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()))
            drawRect(
                Color.Gray.copy(alpha = .3f),
                size = androidx.compose.ui.geometry.Size(width.toFloat(), height.toFloat()),
                style = Stroke(1f / scale),
            )
            colour?.let { drawImage(it) }
            ink?.let { drawImage(it) }
            if (showBones) {
                joints.forEach { (id, start, end) ->
                    val a = if (jointDrag == (id to true)) jointTarget ?: start else start
                    val b = if (jointDrag == (id to false)) jointTarget ?: end else end
                    val tint = Color(0xff32b6ac)
                    drawLine(tint, a, b, 2f / scale)
                    val bone = graph.bones.getValue(id)
                    (if (bone.parentId != null && bone.length <= 2f) listOf(a) else listOf(a, b)).forEach { p ->
                        if (state.tool == SketchTool.JOINT && id == state.bindingPartId) {
                            drawCircle(tint, handleRadius * 1.6f / scale, p, style = Stroke(2f / scale))
                        }
                        drawCircle(Color.White, handleRadius / scale, p)
                        drawCircle(tint, handleRadius / scale, p, style = Stroke(2f / scale))
                    }
                }
            }
            if (state.tool == SketchTool.BIND_REGION) {
                regionPaths.forEach {
                    drawPath(it, Color(0xff32b6ac).copy(alpha = .12f))
                    drawPath(it, Color(0xff32b6ac), style = Stroke(1.5f / scale))
                }
            }
            if (state.tool != SketchTool.FILL) {
                val previewColour = when (state.tool) {
                    SketchTool.ERASER -> Color.Gray.copy(alpha = .5f)
                    SketchTool.BIND_REGION -> Color(0xff32b6ac)
                    else -> Color(state.paintColour)
                }
                points.zipWithNext().forEach { (a, b) ->
                    drawLine(
                        previewColour,
                        Offset(a.x, a.y),
                        Offset(b.x, b.y),
                        state.size,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                }
            }
        }
    }
}
