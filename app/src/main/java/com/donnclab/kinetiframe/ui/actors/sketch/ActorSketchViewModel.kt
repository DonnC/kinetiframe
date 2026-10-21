package com.donnclab.kinetiframe.ui.actors.sketch

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.core.graphics.createBitmap
import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.drawing.BrushId
import com.donnclab.kinetiframe.engine.drawing.BrushRenderers
import com.donnclab.kinetiframe.engine.drawing.BrushSnapshot
import com.donnclab.kinetiframe.engine.drawing.SketchColourFill
import com.donnclab.kinetiframe.engine.rigging.math.ActorDrawingBinder
import com.donnclab.kinetiframe.engine.rigging.model.ActorDrawing
import com.donnclab.kinetiframe.engine.rigging.model.ActorDrawingRegion
import com.donnclab.kinetiframe.engine.rigging.model.DrawingBinding
import com.donnclab.kinetiframe.engine.rigging.model.DrawingComposition
import com.donnclab.kinetiframe.engine.rigging.model.DrawingExtent
import com.donnclab.kinetiframe.engine.rigging.model.DrawingPlacement
import com.donnclab.kinetiframe.engine.rigging.model.DrawingSource
import com.donnclab.kinetiframe.engine.rigging.model.SceneGraph
import com.donnclab.kinetiframe.engine.rigging.model.SkinSourceSize
import com.donnclab.kinetiframe.ui.BaseViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/** Rest-source editor, separate from performance and FxF state. Published pixels are immutable;
 * history is byte-bounded and never recycles a bitmap borrowed by Android's display list. */
class ActorSketchViewModel(
    private val graph: SceneGraph,
    private val sourceWidth: Int,
    private val sourceHeight: Int,
    private val bounds: FloatArray,
    initialInk: Bitmap?,
    initialColour: Bitmap?,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) : BaseViewModel<ActorSketchState, ActorSketchIntent, ActorSketchEffect>(ActorSketchState()) {
    private data class Snapshot(
        val ink: Bitmap,
        val colour: Bitmap,
        val regions: List<ActorDrawingRegion>,
        val graph: SceneGraph,
    ) {
        val bytes get() = ink.allocationByteCount.toLong() +
            colour.allocationByteCount
    }
    private fun snapshot() = Snapshot(
        requireNotNull(currentState.ink),
        requireNotNull(currentState.colour),
        currentState.regions,
        requireNotNull(currentState.graph),
    )
    private val undo = ArrayDeque<Snapshot>()
    private val redo = ArrayDeque<Snapshot>()
    private val budget = minOf(16L * 1024 * 1024, Runtime.getRuntime().maxMemory() / 16)
    init {
        require(
            sourceWidth in 1..2048 && sourceHeight in 1..2048 && sourceWidth.toLong() * sourceHeight <= 1_048_576 &&
                bounds.size == 4,
        )
        viewModelScope.launch {
            val pixels = withContext(dispatcher) {
                Snapshot(
                    initialInk?.copy(Bitmap.Config.ARGB_8888, true) ?: createBitmap(sourceWidth, sourceHeight),
                    initialColour?.copy(Bitmap.Config.ARGB_8888, true) ?: createBitmap(sourceWidth, sourceHeight),
                    graph.drawing?.regions.orEmpty(),
                    graph,
                )
            }
            updateState {
                it.copy(
                    ink = pixels.ink,
                    colour = pixels.colour,
                    busy = false,
                    regions = pixels.regions,
                    graph = graph,
                    bindingPartId = graph.rootBoneIds.firstOrNull(),
                )
            }
        }
    }
    override fun handleIntent(intent: ActorSketchIntent) {
        if (currentState.busy) return
        when (intent) {
            is ActorSketchIntent.MoveJoint -> {
                val previous = snapshot()
                if (!intent.target.x.isFinite() || !intent.target.y.isFinite() ||
                    intent.partId !in previous.graph.bones
                ) {
                    return
                }
                val changed = if (intent.start) {
                    com.donnclab.kinetiframe.engine.rigging.math.ActorPoseEditor.moveOrigin(
                        previous.graph,
                        intent.partId,
                        intent.target,
                    )
                } else {
                    com.donnclab.kinetiframe.engine.rigging.math.ActorPoseEditor.fit(
                        previous.graph,
                        intent.partId,
                        intent.target,
                    )
                }
                if (changed != previous.graph) {
                    undo.addLast(previous)
                    redo.clear()
                    trim(undo)
                    updateState { it.copy(graph = changed, dirty = true, canUndo = undo.isNotEmpty(), canRedo = false) }
                }
            }

            is ActorSketchIntent.Layer -> updateState { it.copy(layer = intent.value, tool = SketchTool.PEN) }

            is ActorSketchIntent.Tool -> updateState {
                it.copy(
                    tool = intent.value,
                    layer = if (intent.value ==
                        SketchTool.FILL
                    ) {
                        SketchLayer.COLOUR
                    } else {
                        it.layer
                    },
                )
            }

            is ActorSketchIntent.Colour -> updateState { it.copy(paintColour = intent.value) }

            is ActorSketchIntent.Size -> updateState { it.copy(size = intent.value.coerceIn(1f, 48f)) }

            is ActorSketchIntent.BindingPart -> if (intent.id in
                graph.bones
            ) {
                updateState { it.copy(bindingPartId = intent.id) }
            }

            is ActorSketchIntent.BindRegion -> {
                val part = currentState.bindingPartId ?: return
                if (currentState.regions.size >=
                    32
                ) {
                    sendEffect(ActorSketchEffect.Error("This drawing already has 32 binding corrections"))
                    return
                }
                val points = intent.outline.filter {
                    it.x.isFinite() && it.y.isFinite()
                }.map { it.copy(x = it.x.coerceIn(0f, 1f), y = it.y.coerceIn(0f, 1f)) }.distinct().take(256)
                if (points.size >= 3) changeRegions(currentState.regions + ActorDrawingRegion(part, points))
            }

            is ActorSketchIntent.ClearRegions -> changeRegions(
                currentState.regions.filterNot {
                    it.boneId ==
                        intent.partId
                },
            )

            ActorSketchIntent.Undo -> restore(undo, redo)

            ActorSketchIntent.Redo -> restore(redo, undo)

            is ActorSketchIntent.Stroke -> if (currentState.tool in
                setOf(SketchTool.PEN, SketchTool.ERASER)
            ) {
                edit { ink, colour ->
                    val target = if (currentState.layer == SketchLayer.LINEWORK) ink else colour
                    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = currentState.paintColour
                        strokeCap = Paint.Cap.ROUND
                        strokeJoin = Paint.Join.ROUND
                        if (currentState.tool == SketchTool.ERASER) xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                    }
                    BrushRenderers.draw(
                        Canvas(target),
                        intent.points.take(8192),
                        currentState.size,
                        paint,
                        BrushSnapshot(BrushId.PEN),
                    )
                }
            }

            is ActorSketchIntent.Fill -> edit { ink, colour ->
                val lines = IntArray(sourceWidth * sourceHeight)
                val pixels = IntArray(lines.size)
                ink.getPixels(lines, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight)
                colour.getPixels(pixels, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight)
                val filled = SketchColourFill.apply(
                    lines,
                    pixels,
                    sourceWidth,
                    sourceHeight,
                    intent.x,
                    intent.y,
                    currentState.paintColour,
                )
                colour.setPixels(filled, 0, sourceWidth, 0, 0, sourceWidth, sourceHeight)
            }

            ActorSketchIntent.Bind -> bind()
        }
    }
    private fun edit(operation: (Bitmap, Bitmap) -> Unit) {
        val previous = snapshot()
        updateState { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val next =
                    withContext(dispatcher) {
                        Snapshot(
                            previous.ink.copy(Bitmap.Config.ARGB_8888, true),
                            previous.colour.copy(Bitmap.Config.ARGB_8888, true),
                            previous.regions,
                            previous.graph,
                        ).also {
                            operation(it.ink, it.colour)
                        }
                    }
                undo.addLast(previous)
                redo.clear()
                trim(undo)
                updateState {
                    it.copy(
                        ink = next.ink,
                        colour = next.colour,
                        busy = false,
                        dirty = true,
                        revision = it.revision + 1,
                        canUndo = undo.isNotEmpty(),
                        canRedo = false,
                    )
                }
            } catch (
                cancel: CancellationException,
            ) {
                throw cancel
            } catch (
                failure: Exception,
            ) {
                updateState { it.copy(busy = false) }
                sendEffect(ActorSketchEffect.Error(failure.message ?: "Could not edit drawing"))
            }
        }
    }
    private fun trim(history: ArrayDeque<Snapshot>) {
        while (history.sumOf { it.bytes } >
            budget
        ) {
            history.removeFirst()
        }
    }
    private fun changeRegions(regions: List<ActorDrawingRegion>) {
        if (regions == currentState.regions) return
        undo.addLast(snapshot())
        redo.clear()
        trim(undo)
        updateState { it.copy(regions = regions, dirty = true, canUndo = undo.isNotEmpty(), canRedo = false) }
    }
    private fun restore(from: ArrayDeque<Snapshot>, to: ArrayDeque<Snapshot>) {
        val next = from.removeLastOrNull() ?: return
        to.addLast(snapshot())
        trim(to)
        updateState {
            it.copy(
                ink = next.ink,
                colour = next.colour,
                regions = next.regions,
                graph = next.graph,
                dirty = true,
                revision =
                it.revision + 1,
                canUndo = undo.isNotEmpty(),
                canRedo = redo.isNotEmpty(),
            )
        }
    }
    private fun bind() {
        val snapshot = snapshot()
        updateState { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val result = withContext(dispatcher) {
                    fun hasArtwork(bitmap: Bitmap): Boolean {
                        val row = IntArray(bitmap.width)
                        for (y in 0 until bitmap.height) {
                            bitmap.getPixels(row, 0, bitmap.width, 0, y, bitmap.width, 1)
                            if (row.any { it ushr 24 != 0 }) return true
                        }
                        return false
                    }
                    require(hasArtwork(snapshot.ink) || hasArtwork(snapshot.colour)) {
                        "Draw your character before making it movable"
                    }
                    val rest = com.donnclab.kinetiframe.engine.rigging.math.ArtworkSkinRestBinding.rebase(
                        snapshot.graph,
                    )
                    val size = SkinSourceSize(sourceWidth, sourceHeight)
                    val placement = DrawingPlacement(Point2(bounds[0], bounds[1]), DrawingExtent(bounds[2], bounds[3]))
                    val skin = ActorDrawingBinder.bind(rest, size, placement, snapshot.regions)
                    val drawing = ActorDrawing(
                        source = DrawingSource(UUID.randomUUID().toString(), UUID.randomUUID().toString(), size),
                        placement = placement,
                        binding = DrawingBinding(skin, snapshot.regions),
                        composition = DrawingComposition(
                            overlayParts = graph.drawing?.overlayParts.orEmpty(),
                            underlayParts = graph.drawing?.underlayParts.orEmpty(),
                        ),
                    )
                    ActorSketchEffect.Bound(
                        snapshot.ink.copy(Bitmap.Config.ARGB_8888, false),
                        snapshot.colour.copy(Bitmap.Config.ARGB_8888, false),
                        drawing,
                        rest,
                    )
                }
                sendEffect(result)
                updateState { it.copy(busy = false) }
            } catch (
                cancel: CancellationException,
            ) {
                throw cancel
            } catch (
                failure: Exception,
            ) {
                updateState { it.copy(busy = false) }
                sendEffect(ActorSketchEffect.Error(failure.message ?: "Could not bind sketch"))
            }
        }
    }
}
