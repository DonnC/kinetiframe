package com.donnclab.kinetiframe.studio.domain

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.asAndroidPath
import com.donnclab.kinetiframe.studio.drawing.DrawCommandRenderer

/** Draws undo history items onto a layer bitmap, so a layer can be rebuilt from its history. */
internal class HistoryItemPainter(val commands: DrawCommandRenderer = DrawCommandRenderer()) {
    /** Replays [items] in order onto [canvas], which draws into [bitmap]. */
    fun paintAll(canvas: Canvas, items: List<HistoryItem>, bitmap: Bitmap) {
        for (item in items) {
            when (item) {
                is HistoryItem.Stroke -> commands.draw(canvas, item.command, bitmap)

                is HistoryItem.StrokeBatch -> item.commands.forEach { commands.draw(canvas, it, bitmap) }

                is HistoryItem.SelectionEdit -> paintSelectionEdit(canvas, item)

                is HistoryItem.ClearArea -> paintClear(canvas, item)

                // Already applied to durable scene metadata.
                is HistoryItem.ObjectEdit -> Unit

                is HistoryItem.RasterSelection -> paintRasterSelection(canvas, item)
            }
        }
    }

    /** Draws a committed or pasted selection at its transformed position. Other edits draw nothing. */
    fun paintSelectionEdit(canvas: Canvas, edit: HistoryItem.SelectionEdit) {
        if (edit.type != SelectionEditType.COMMIT && edit.type != SelectionEditType.PASTE) return
        val proxy = edit.bitmap ?: return
        canvas.save()
        canvas.concat(edit.matrix.toAndroidMatrix())
        canvas.drawBitmap(proxy, edit.originalBounds.left, edit.originalBounds.top, Paint(Paint.ANTI_ALIAS_FLAG))
        canvas.restore()
    }

    /** Removes the selection's source pixels, then draws its moved copy. */
    fun paintRasterSelection(canvas: Canvas, edit: HistoryItem.RasterSelection) {
        edit.clear?.let { paintClear(canvas, it) }
        edit.stamp?.let { stamp ->
            stamp.bitmap?.let { proxy ->
                canvas.save()
                canvas.concat(stamp.matrix.toAndroidMatrix())
                canvas.drawBitmap(
                    proxy,
                    stamp.originalBounds.left,
                    stamp.originalBounds.top,
                    Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG),
                )
                canvas.restore()
            }
        }
    }

    private fun paintClear(canvas: Canvas, clear: HistoryItem.ClearArea) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
        clear.path?.let { canvas.drawPath(it.asAndroidPath(), paint) }
            ?: clear.rect?.let { canvas.drawRect(it.left, it.top, it.right, it.bottom, paint) }
    }

    private fun Matrix.toAndroidMatrix() = android.graphics.Matrix().also { android ->
        val v = this.values
        android.setValues(floatArrayOf(v[0], v[4], v[12], v[1], v[5], v[13], v[3], v[7], v[15]))
    }
}
