package com.donnclab.kinetiframe.actors.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/** One draw path for studio, project previews and export. All mesh work is in the render plan;
 * this performs no bitmap/path/vertex allocation and creates no intermediate render layers. */
object PuppetArtworkRenderer {
    private val figurePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /** Draws a command whether it is a drawn figure or an image; [bitmaps] finds the image. */
    fun draw(canvas: Canvas, command: PuppetRenderCommand, paint: Paint, bitmaps: (String) -> Bitmap?) {
        val figure = command.figure
        if (figure != null) {
            drawFigure(canvas, command.matrix, figure, paint.alpha)
        } else {
            bitmaps(command.assetId)?.takeUnless(Bitmap::isRecycled)?.let { draw(canvas, command, it, paint) }
        }
    }

    private fun drawFigure(canvas: Canvas, matrix: android.graphics.Matrix, figure: FigureDrawing, alpha: Int) {
        val save = canvas.save()
        try {
            canvas.concat(matrix)
            figure.strokes.forEach { stroke ->
                stroke.fill?.let { colour ->
                    figurePaint.style = Paint.Style.FILL
                    figurePaint.color = colour
                    figurePaint.alpha = alpha
                    canvas.drawPath(stroke.path, figurePaint)
                }
                if (stroke.lineWidth > 0f) {
                    val inner = canvas.save()
                    stroke.hiddenOutline?.let { canvas.clipOutPath(it) }
                    figurePaint.style = Paint.Style.STROKE
                    figurePaint.strokeWidth = stroke.lineWidth
                    figurePaint.color = stroke.ink
                    figurePaint.alpha = alpha
                    canvas.drawPath(stroke.path, figurePaint)
                    canvas.restoreToCount(inner)
                }
            }
        } finally {
            canvas.restoreToCount(save)
        }
    }

    /** The four corners of a command on the canvas, for selection and hit testing. */
    fun outline(command: PuppetRenderCommand, bitmaps: (String) -> Bitmap?): FloatArray? {
        command.figure?.let { figure ->
            val b = figure.bounds
            return floatArrayOf(b.left, b.top, b.right, b.top, b.right, b.bottom, b.left, b.bottom)
                .also(command.matrix::mapPoints)
        }
        val bitmap = bitmaps(command.assetId) ?: return null
        return outline(command, bitmap.width, bitmap.height)
    }

    fun draw(canvas: Canvas, command: PuppetRenderCommand, bitmap: Bitmap, paint: Paint) {
        if (bitmap.isRecycled) return
        val mesh = command.mesh
        if (mesh != null && (mesh.width != bitmap.width || mesh.height != bitmap.height)) return
        val save = canvas.save()
        try {
            canvas.concat(command.matrix)
            if (mesh == null) {
                canvas.drawBitmap(bitmap, 0f, 0f, paint)
            } else {
                canvas.drawBitmapMesh(bitmap, mesh.columns, mesh.rows, mesh.vertices, 0, null, 0, paint)
            }
        } finally {
            canvas.restoreToCount(save)
        }
    }

    fun outline(command: PuppetRenderCommand, width: Int, height: Int): FloatArray {
        val mesh = command.mesh
        val points = if (mesh == null) {
            floatArrayOf(
                0f,
                0f,
                width.toFloat(),
                0f,
                width.toFloat(),
                height.toFloat(),
                0f,
                height.toFloat(),
            )
        } else {
            mesh.vertices.copyOf()
        }
        command.matrix.mapPoints(points)
        return points
    }
}
