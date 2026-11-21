package com.donnclab.kinetiframe.actors.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint

/** One draw path for studio, project previews and export. All mesh work is in the render plan;
 * this performs no bitmap/path/vertex allocation and creates no intermediate render layers. */
object PuppetArtworkRenderer {
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
