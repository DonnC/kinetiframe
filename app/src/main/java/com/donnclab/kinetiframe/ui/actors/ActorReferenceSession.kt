package com.donnclab.kinetiframe.ui.actors

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import com.donnclab.kinetiframe.ui.components.canvas.SelectionTransformState

/** UI-session resource, deliberately absent from StudioState, caches, assets and saved state.
 * Never recycle an exposed bitmap: RenderThread may still be reading the previous display list.
 * Dropping the one bounded reference lets Android reclaim it safely. */
@Stable
class ActorReferenceSession {
    var bitmap by mutableStateOf<Bitmap?>(null)
        private set

    /** Invalidates imports still decoding when a guide is removed or the session is cleared. */
    var importGeneration: Long = 0
        private set
    var visible by mutableStateOf(true)
    var editing by mutableStateOf(false)
    var menuOpen by mutableStateOf(false)
    var lockAspectRatio by mutableStateOf(true)
    var skewMode by mutableStateOf(false)
    var opacity by mutableFloatStateOf(0.5f)
    var transform by mutableStateOf(SelectionTransformState())
        private set
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val matrix = android.graphics.Matrix()
    private val androidValues = FloatArray(9).apply { this[8] = 1f }
    val bounds: Rect get() = bitmap?.let { Rect(0f, 0f, it.width.toFloat(), it.height.toFloat()) } ?: Rect.Zero

    fun replace(image: Bitmap, stageWidth: Int, stageHeight: Int) {
        bitmap = image
        visible = true
        opacity = 0.5f
        lockAspectRatio = true
        skewMode = false
        fit(stageWidth, stageHeight)
        editing = true
    }

    fun fit(stageWidth: Int, stageHeight: Int) {
        val image = bitmap ?: return
        val scale = minOf(stageWidth.toFloat() / image.width, stageHeight.toFloat() / image.height)
        transform = SelectionTransformState(
            androidx.compose.ui.graphics.Matrix().apply {
                translate((stageWidth - image.width * scale) / 2f, (stageHeight - image.height * scale) / 2f)
                scale(scale, scale)
            },
        )
    }

    fun remove() {
        importGeneration++
        editing = false
        menuOpen = false
        bitmap = null
    }

    /** Called inside an existing document-space draw, beneath ink and outside eraser groups. */
    fun draw(canvas: Canvas) {
        val image = bitmap ?: return
        if (!visible || image.isRecycled) return
        val values = transform.matrix.values
        androidValues[0] = values[0]
        androidValues[1] = values[4]
        androidValues[2] = values[12]
        androidValues[3] = values[1]
        androidValues[4] = values[5]
        androidValues[5] = values[13]
        matrix.setValues(androidValues)
        paint.alpha = (opacity.coerceIn(0f, 1f) * 255).toInt()
        canvas.drawBitmap(image, matrix, paint)
    }
}
