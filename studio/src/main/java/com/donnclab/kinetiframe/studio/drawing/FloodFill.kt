package com.donnclab.kinetiframe.studio.drawing

import android.graphics.Bitmap
import java.util.ArrayDeque
import java.util.BitSet

/** Fills the region around a pixel whose colours are within a tolerance of the starting colour. */
internal object FloodFill {
    /**
     * Fills [bitmap] starting at ([x], [y]) with [targetColor]. When [reference] is given, the
     * region is found in [reference] and only the filled pixels are written to [bitmap].
     */
    fun fill(bitmap: Bitmap, x: Int, y: Int, targetColor: Int, tolerance: Float, reference: Bitmap? = null) {
        val source = reference ?: bitmap
        if (x !in 0 until source.width || y !in 0 until source.height) return

        val startColor = source.getPixel(x, y)
        if (startColor == targetColor && reference == null) return

        val width = source.width
        val height = source.height
        val pixels = IntArray(width * height)
        source.getPixels(pixels, 0, width, 0, 0, width, height)

        val resultPixels = if (reference != null) IntArray(width * height) else pixels

        val queue = ArrayDeque<Int>()
        queue.add(y * width + x)

        val toleranceSq = (tolerance * 4.41f).let { it * it }
        val visited = BitSet(width * height)
        visited.set(y * width + x)

        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()

            if (isWithinTolerance(pixels[index], startColor, toleranceSq)) {
                resultPixels[index] = targetColor

                val currX = index % width
                val currY = index / width

                if (currX > 0 && !visited.get(index - 1)) {
                    queue.add(index - 1)
                    visited.set(index - 1)
                }
                if (currX < width - 1 && !visited.get(index + 1)) {
                    queue.add(index + 1)
                    visited.set(index + 1)
                }
                if (currY > 0 && !visited.get(index - width)) {
                    queue.add(index - width)
                    visited.set(index - width)
                }
                if (currY < height - 1 && !visited.get(index + width)) {
                    queue.add(index + width)
                    visited.set(index + width)
                }
            }
        }

        bitmap.setPixels(resultPixels, 0, width, 0, 0, width, height)
    }

    private fun isWithinTolerance(c1: Int, c2: Int, toleranceSq: Float): Boolean {
        if (c1 == c2) return true
        val a1 = (c1 shr 24) and 0xFF
        val r1 = (c1 shr 16) and 0xFF
        val g1 = (c1 shr 8) and 0xFF
        val b1 = c1 and 0xFF
        val a2 = (c2 shr 24) and 0xFF
        val r2 = (c2 shr 16) and 0xFF
        val g2 = (c2 shr 8) and 0xFF
        val b2 = c2 and 0xFF
        val da = a1 - a2
        val dr = r1 - r2
        val dg = g1 - g2
        val db = b1 - b2
        return (da * da + dr * dr + dg * dg + db * db) <= toleranceSq
    }
}
