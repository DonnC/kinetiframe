package com.donnclab.kinetiframe.studio.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import androidx.core.graphics.createBitmap
import com.donnclab.kinetiframe.actors.render.ActorFollowCamera
import com.donnclab.kinetiframe.actors.render.ActorFrameRenderer
import com.donnclab.kinetiframe.core.document.Layer
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.media.video.SceneVideoRenderer
import com.donnclab.kinetiframe.studio.drawing.LayerCompositor

/**
 * Headless per-frame flattener: background + raster layers ([LayerCompositor], shared with the
 * live editor) + actor placements ([ActorFrameRenderer], also shared) onto one stage-sized [Bitmap]. This is deliberately the *only* place a full frame
 * is assembled outside the live Compose canvas -- [com.donnclab.kinetiframe.export.data.ProjectExporter]
 * and the project-card thumbnail hook both call [composite] instead of re-deriving flattening
 * rules a second time.
 *
 * Scene video is composited over raster content, below actor placements. Legacy reference
 * clips remain editor-only and are excluded from exports and thumbnails.
 *
 * [destination] and [rasterScratch] are allocated once and reused for every frame in a run: an
 * export can be several hundred frames, and a fresh ARGB_8888 bitmap per frame would be wasteful
 * GC churn on a mobile-first app. Callers must not hold onto a [Bitmap] returned by [composite]
 * past their next call to it (copy it first, e.g. before handing it to a video encoder that needs
 * to keep working while the next frame is prepared) or past [release].
 */
class ProjectFrameCompositor(
    private val project: ProjectDocument,
    private val frameLayers: Map<Int, List<Layer>>,
    private val cacheManager: FrameCacheManager,
    private val actorRenderer: ActorFrameRenderer?,
    private val backgroundBitmap: Bitmap?,
    private val fontRegistry: com.donnclab.kinetiframe.studio.drawing.FontRegistry? = null,
    /** Previews render directly into small buffers; export leaves this at stage resolution. */
    outputMaxEdge: Int = maxOf(project.stage.width, project.stage.height),
    /** Optional test/decoder seam; returned frames are owned and recycled by this compositor. */
    private val videoFrameAt: (suspend (Int) -> Bitmap?)? = null,
    /** Gallery posters remain legible even when every project frame is faded. Never for filmstrips/export. */
    private val includeFades: Boolean = true,
) {
    private val scale = minOf(
        1f,
        outputMaxEdge.coerceAtLeast(1).toFloat() / maxOf(project.stage.width, project.stage.height),
    )
    private val outputWidth = (project.stage.width * scale).toInt().coerceAtLeast(1)
    private val outputHeight = (project.stage.height * scale).toInt().coerceAtLeast(1)
    private val destination = createBitmap(outputWidth, outputHeight)
    private val rasterScratch = createBitmap(outputWidth, outputHeight)
    private val canvas = Canvas(destination)
    private val stageRect = android.graphics.Rect(0, 0, project.stage.width, project.stage.height)
    private val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val backgroundRect = backgroundBitmap?.let { background ->
        val cover = maxOf(
            project.stage.width.toFloat() / background.width,
            project.stage.height.toFloat() / background.height,
        )
        val width = background.width * cover
        val height = background.height * cover
        android.graphics.RectF(
            (project.stage.width - width) / 2f,
            (project.stage.height - height) / 2f,
            (project.stage.width + width) / 2f,
            (project.stage.height + height) / 2f,
        )
    }
    private val identity = Matrix()
    private val cameraMatrix = Matrix()
    private val fadePaint = Paint().apply { color = Color.BLACK }
    private val videoDecoder = com.donnclab.kinetiframe.media.video.VideoFrameDecoder(outputMaxEdge)
    private val videoRenderer = SceneVideoRenderer()

    init {
        canvas.scale(outputWidth.toFloat() / project.stage.width, outputHeight.toFloat() / project.stage.height)
    }

    /** Composites [frame] into the shared [destination] bitmap and returns it. The same instance
     *  is returned every call -- see the class doc for the reuse contract. Sparse legacy frame
     *  metadata falls back to the project stack; pixel absence still means an empty drawing. */
    suspend fun composite(frame: Int): Bitmap {
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        val pose = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            ActorFollowCamera.evaluate(project, frame, actorRenderer)
        }
        val sceneSave = canvas.save()
        canvas.clipRect(stageRect)
        if (project.cameraShots.isNotEmpty()) canvas.drawColor(Color.BLACK)
        cameraMatrix.setValues(
            com.donnclab.kinetiframe.engine.camera.SceneCamera.matrix(
                pose,
                project.stage.width,
                project.stage.height,
            ).androidValues(),
        )
        canvas.concat(cameraMatrix)
        try {
            // Camera previews use white drawing paper when no background is attached; exposed
            // offstage pixels remain black. Preserve legacy transparent exports without camera.
            if (project.cameraShots.isNotEmpty()) {
                fadePaint.alpha = 255
                fadePaint.color = Color.WHITE
                canvas.drawRect(stageRect, fadePaint)
                fadePaint.color = Color.BLACK
            }
            backgroundBitmap?.takeUnless(Bitmap::isRecycled)?.let { background ->
                val backgroundSave = canvas.save()
                try {
                    canvas.clipRect(stageRect)
                    canvas.drawBitmap(background, null, requireNotNull(backgroundRect), backgroundPaint)
                } finally {
                    canvas.restoreToCount(backgroundSave)
                }
            }
            val layers = frameLayers[frame] ?: project.layers
            if (layers.isNotEmpty()) {
                LayerCompositor.compose(layers, frame, rasterScratch, project.stage.width, project.stage.height) {
                        layer,
                        index,
                    ->
                    cacheManager.getFrame(layer.id, index)
                }
                canvas.drawBitmap(rasterScratch, null, stageRect, backgroundPaint)
            }
            val video = project.videoClips.values.firstOrNull {
                it.isSceneContent &&
                    frame in it.timelineStartFrame until it.timelineEndFrame
            }
            if (video != null) {
                val bitmap = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                    if (videoFrameAt != null) videoFrameAt.invoke(frame) else videoDecoder.decode(project, frame)
                } ?: throw IllegalStateException("Video '${video.label}' unavailable at frame ${frame + 1}")
                try {
                    canvas.save()
                    videoRenderer.draw(canvas, bitmap, video)
                    canvas.restore()
                } finally {
                    bitmap.recycle()
                }
            }
            if (actorRenderer != null && project.actorPlacements.isNotEmpty()) {
                val resolved = actorRenderer.resolveAll(project.actorPlacements.values, project.id, frame, project.fps)
                resolved.firstOrNull {
                    it.diagnostic != null
                }?.diagnostic?.let { throw IllegalStateException(it.message) }
                val renders = resolved.mapNotNull { it.render }
                actorRenderer.draw(canvas, identity, renders)
            }
            project.nodes.values.filterIsInstance<com.donnclab.kinetiframe.core.document.TextNode>().filter {
                it.isVisible
            }.forEach { node ->
                val typeface =
                    fontRegistry?.availableFonts?.firstOrNull { it.fileName == node.fontFileName }?.typeface
                        ?: android.graphics.Typeface.DEFAULT
                com.donnclab.kinetiframe.studio.drawing.MultilineTextRenderer.draw(
                    canvas,
                    node,
                    typeface,
                    project.nodeDensity,
                )
            }
        } finally {
            canvas.restoreToCount(sceneSave)
        }
        if (includeFades && pose.black > 0f) {
            fadePaint.alpha = (pose.black * 255).toInt()
            canvas.drawRect(0f, 0f, project.stage.width.toFloat(), project.stage.height.toFloat(), fadePaint)
        }
        return destination
    }

    /** Releases the reused bitmaps and the actor renderer's own bitmap cache. Call once when a
     *  run (an export, or one thumbnail generation) is done. */
    fun release() {
        videoDecoder.close()
        actorRenderer?.clear()
        if (!destination.isRecycled) destination.recycle()
        if (!rasterScratch.isRecycled) rasterScratch.recycle()
    }
}
