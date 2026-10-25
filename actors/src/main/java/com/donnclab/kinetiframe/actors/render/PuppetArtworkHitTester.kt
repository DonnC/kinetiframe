package com.donnclab.kinetiframe.actors.render

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager

/** Topmost visible artwork wins. Rigid pieces reject transparent margins at source-pixel precision. */
object PuppetArtworkHitTester {
    /** Bounded sampling selects whole rig parts; it never destructively cuts a texture. */
    fun select(path: android.graphics.Path, commands: List<PuppetRenderCommand>, assets: RigAssetManager): String? {
        val bounds = android.graphics.RectF()
        path.computeBounds(bounds, true)
        if (bounds.isEmpty || !bounds.left.isFinite() || !bounds.top.isFinite() || !bounds.right.isFinite() ||
            !bounds.bottom.isFinite()
        ) {
            return null
        }
        val clip = android.graphics.Region(
            kotlin.math.floor(bounds.left).toInt(),
            kotlin.math.floor(bounds.top).toInt(),
            kotlin.math.ceil(bounds.right).toInt(),
            kotlin.math.ceil(bounds.bottom).toInt(),
        )
        val region = android.graphics.Region().apply { setPath(path, clip) }
        // Try the centre first, then distributed points for off-centre or concave lassos.
        val samples = buildList {
            add(Offset(bounds.centerX(), bounds.centerY()))
            for (row in 0 until 16) {
                for (column in 0 until 16) {
                    add(
                        Offset(
                            bounds.left + bounds.width() * (column + .5f) / 16,
                            bounds.top + bounds.height() * (row + .5f) / 16,
                        ),
                    )
                }
            }
        }.filter { region.contains(it.x.toInt(), it.y.toInt()) }
        return synchronized(assets) {
            commands.asReversed().firstOrNull { command ->
                samples.any { hit(it, listOf(command), assets) != null }
            }?.boneId
        }
    }

    fun hit(point: Offset, commands: List<PuppetRenderCommand>, assets: RigAssetManager): String? =
        synchronized(assets) {
            commands.asReversed().firstOrNull { command ->
                val bitmap = assets.getAsset(command.assetId) ?: return@firstOrNull false
                if (bitmap.isRecycled) return@firstOrNull false
                if (command.mesh == null) {
                    val inverse = android.graphics.Matrix()
                    if (!command.matrix.invert(inverse)) return@firstOrNull false
                    val p = floatArrayOf(point.x, point.y)
                    inverse.mapPoints(p)
                    val x = kotlin.math.floor(p[0]).toInt()
                    val y = kotlin.math.floor(p[1]).toInt()
                    x in 0 until bitmap.width && y in 0 until bitmap.height &&
                        android.graphics.Color.alpha(bitmap.getPixel(x, y)) > 24
                } else {
                    meshHit(point, command, bitmap)
                }
            }?.boneId
        }

    // Test the deformed triangles, not their bounding box: bent elbows contain empty space.
    private fun meshHit(point: Offset, command: PuppetRenderCommand, bitmap: android.graphics.Bitmap): Boolean {
        val mesh = requireNotNull(command.mesh)
        val vertices = PuppetArtworkRenderer.outline(command, bitmap.width, bitmap.height)
        fun triangle(a: Int, b: Int, c: Int): Boolean {
            val ax = vertices[a * 2]
            val ay = vertices[a * 2 + 1]
            val bx = vertices[b * 2]
            val by = vertices[b * 2 + 1]
            val cx = vertices[c * 2]
            val cy = vertices[c * 2 + 1]
            val denominator = (by - cy) * (ax - cx) + (cx - bx) * (ay - cy)
            if (kotlin.math.abs(denominator) < .0001f) return false
            val u = ((by - cy) * (point.x - cx) + (cx - bx) * (point.y - cy)) / denominator
            val v = ((cy - ay) * (point.x - cx) + (ax - cx) * (point.y - cy)) / denominator
            val w = 1f - u - v
            if (u < 0f || v < 0f || w < 0f) return false
            fun x(i: Int) = (i % (mesh.columns + 1)).toFloat() * bitmap.width / mesh.columns
            fun y(i: Int) = (i / (mesh.columns + 1)).toFloat() * bitmap.height / mesh.rows
            val sx = (u * x(a) + v * x(b) + w * x(c)).toInt().coerceIn(0, bitmap.width - 1)
            val sy = (u * y(a) + v * y(b) + w * y(c)).toInt().coerceIn(0, bitmap.height - 1)
            return android.graphics.Color.alpha(bitmap.getPixel(sx, sy)) > 24
        }
        for (row in 0 until mesh.rows) {
            for (column in 0 until mesh.columns) {
                val a = row * (mesh.columns + 1) + column
                val b = a + 1
                val c = a + mesh.columns + 1
                val d = c + 1
                if (triangle(a, b, c) || triangle(b, d, c)) return true
            }
        }
        return false
    }
}
