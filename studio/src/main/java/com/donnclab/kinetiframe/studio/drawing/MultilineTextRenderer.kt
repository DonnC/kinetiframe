package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.donnclab.kinetiframe.core.document.TextNode

object MultilineTextRenderer {

    fun getBounds(
        textNode: TextNode,
        typeface: android.graphics.Typeface?,
        density: Float,
    ): androidx.compose.ui.geometry.Rect {
        val staticLayout = createLayout(textNode, typeface ?: Typeface.DEFAULT, density)
        return androidx.compose.ui.geometry.Rect(0f, 0f, staticLayout.width.toFloat(), staticLayout.height.toFloat())
    }

    fun draw(canvas: Canvas, textNode: TextNode, typeface: Typeface, density: Float = 1f) {
        canvas.save()
        canvas.concat(android.graphics.Matrix().apply { setValues(textNode.selectionTransform.androidValues()) })

        // Apply Transformation (Position, Zoom, Rotation)
        canvas.translate(textNode.x, textNode.y)
        canvas.rotate(textNode.rotation)
        canvas.scale(textNode.scaleX, textNode.scaleY)

        val staticLayout = createLayout(textNode, typeface, density)

        // Center the layout
        canvas.translate(-staticLayout.width / 2f, -staticLayout.height / 2f)

        // Draw Stroke/Outline
        if (textNode.style.strokeWidthPx > 0) {
            val strokePaint = TextPaint(createTextPaint(textNode, typeface, density)).apply {
                this.color = textNode.style.strokeColorHex
                this.style = Paint.Style.STROKE
                this.strokeWidth = textNode.style.strokeWidthPx
            }
            val strokeLayout = StaticLayout.Builder.obtain(
                textNode.text,
                0,
                textNode.text.length,
                strokePaint,
                staticLayout.width,
            ).setAlignment(layoutAlignment(textNode)).setLineSpacing(0f, 1.0f).setIncludePad(true).build()

            strokeLayout.draw(canvas)
        }

        // Draw Main Text Fill
        staticLayout.draw(canvas)

        // Draw Interactive Selection Bounding Box if selected
        if (textNode.isSelected) {
            drawSelectionFrame(canvas, staticLayout.width.toFloat(), staticLayout.height.toFloat())
        }

        canvas.restore()
    }

    /**
     * Produces the one layout definition used for drawing, hit-testing, and selection.
     * Keeping these paths identical prevents a node's handles from drifting away from
     * its glyphs when a font, size, or multiline value changes.
     */
    private fun createLayout(textNode: TextNode, typeface: Typeface, density: Float): StaticLayout {
        val textPaint = createTextPaint(textNode, typeface, density)
        val desiredWidth = StaticLayout.getDesiredWidth(textNode.text, textPaint)
            .toInt()
            .coerceAtLeast(MIN_LAYOUT_WIDTH_PX)
        return StaticLayout.Builder.obtain(textNode.text, 0, textNode.text.length, textPaint, desiredWidth)
            .setAlignment(layoutAlignment(textNode))
            .setLineSpacing(0f, 1.0f)
            .setIncludePad(true)
            .build()
    }

    private fun createTextPaint(textNode: TextNode, typeface: Typeface, density: Float) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = typeface
            this.textSize = (textNode.fontSizeSp * density).coerceAtLeast(1f)
            this.color = textNode.style.mainColorHex
            this.style = Paint.Style.FILL
        }

    private fun layoutAlignment(textNode: TextNode): Layout.Alignment = when (textNode.alignment) {
        TextNode.TextAlignment.LEFT -> Layout.Alignment.ALIGN_NORMAL
        TextNode.TextAlignment.CENTER -> Layout.Alignment.ALIGN_CENTER
        TextNode.TextAlignment.RIGHT -> Layout.Alignment.ALIGN_OPPOSITE
    }

    private fun drawSelectionFrame(canvas: Canvas, width: Float, height: Float) {
        val paint = Paint().apply {
            color = Color.parseColor("#00E5FF")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        canvas.drawRect(-width / 2f, -height / 2f, width / 2f, height / 2f, paint)
    }

    private const val MIN_LAYOUT_WIDTH_PX = 100
}
