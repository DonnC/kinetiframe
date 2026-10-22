package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.donnclab.kinetiframe.project.model.DrawCommand
import com.donnclab.kinetiframe.project.model.DrawingTool

/**
 * Draws one saved [DrawCommand] onto a layer. Blur and smudge read and write [bitmap] directly,
 * and fill floods it, so the canvas must draw into that same bitmap.
 */
internal class DrawCommandRenderer {
    private val blurTool = BlurTool()
    private val smudgeTool = SmudgeTool()

    fun draw(canvas: Canvas, command: DrawCommand, bitmap: Bitmap) {
        val points = command.points
        if (points.isEmpty()) return

        if (command.tool == DrawingTool.BLUR || command.tool == DrawingTool.SMUDGE) {
            ManipulationStrokeRenderer.render(
                canvas = canvas,
                bitmap = bitmap,
                points = points,
                tool = command.tool,
                baseWidth = command.width,
                strength = command.strength,
                blurTool = blurTool,
                smudgeTool = smudgeTool,
            )
            return
        }

        if (command.tool == DrawingTool.COLOR_FILL) {
            val point = points.first()
            FloodFill.fill(bitmap, point.x.toInt(), point.y.toInt(), command.color, command.width)
            return
        }

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            if (command.tool == DrawingTool.ERASER) {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
                color = Color.TRANSPARENT
                alpha = 0
            } else {
                color = command.color
                alpha = (command.alpha * 255).toInt().coerceIn(0, 255)
                xfermode = null
            }
        }

        BrushRenderers.draw(canvas, points, command.width, paint, command.brush)
    }
}
