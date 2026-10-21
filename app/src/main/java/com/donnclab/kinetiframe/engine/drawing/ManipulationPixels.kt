package com.donnclab.kinetiframe.engine.drawing

import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt

/** Android-independent premultiplied RGBA math. Transparent RGB must never create dark fringes. */
internal class ManipulationPixels {
    private var a = FloatArray(0)
    private var b = FloatArray(0)

    fun clear() {
        a = FloatArray(0)
        b = FloatArray(0)
    }

    fun apply(
        input: IntArray,
        output: IntArray,
        width: Int,
        height: Int,
        cx: Float,
        cy: Float,
        radius: Float,
        dx: Float,
        dy: Float,
        amount: Float,
        blur: Boolean,
        kernel: Int,
    ) {
        val count = width * height
        if (a.size < count * 4) {
            a = FloatArray(count * 4)
            b = FloatArray(count * 4)
        }
        for (i in 0 until count) {
            val pixel = input[i]
            val alpha = (pixel ushr 24).toFloat()
            val offset = i * 4
            a[offset] = alpha
            a[offset + 1] = (pixel ushr 16 and 255) * alpha / 255f
            a[offset + 2] = (pixel ushr 8 and 255) * alpha / 255f
            a[offset + 3] = (pixel and 255) * alpha / 255f
        }
        if (blur) {
            repeat(3) {
                box(a, b, width, height, kernel, horizontal = true)
                box(b, a, width, height, kernel, horizontal = false)
            }
        }
        val exposure = if (blur) (1f - exp(-3f * amount * hypot(dx, dy) / radius)) else amount
        for (y in 0 until height) {
            for (x in 0 until width) {
                val index = y * width + x
                val distance = hypot(x + 0.5f - cx, y + 0.5f - cy) / radius
                if (distance >= 1f || exposure <= 0f) {
                    output[index] = input[index]
                    continue
                }
                val t = ((distance - 0.3f) / 0.7f).coerceIn(0f, 1f)
                val mix = exposure * (1f - t * t * (3f - 2f * t))
                val pixel = input[index]
                val oldAlpha = (pixel ushr 24).toFloat()
                val sampleX = (x - dx).coerceIn(0f, (width - 1).toFloat())
                val sampleY = (y - dy).coerceIn(0f, (height - 1).toFloat())
                val alpha =
                    oldAlpha +
                        ((if (blur) a[index * 4] else sample(sampleX, sampleY, width, height, 0)) - oldAlpha) * mix
                var packed = alpha.roundToInt().coerceIn(0, 255) shl 24
                if (alpha >= 0.5f) {
                    for (channel in 1..3) {
                        val shift = (3 - channel) * 8
                        val original = (pixel ushr shift and 255) * oldAlpha / 255f
                        val changed = if (blur) {
                            a[index * 4 + channel]
                        } else {
                            sample(
                                sampleX,
                                sampleY,
                                width,
                                height,
                                channel,
                            )
                        }
                        val straight = ((original + (changed - original) * mix) * 255f / alpha).roundToInt().coerceIn(
                            0,
                            255,
                        )
                        packed = packed or (straight shl shift)
                    }
                }
                output[index] = packed
            }
        }
    }

    private fun sample(x: Float, y: Float, width: Int, height: Int, channel: Int): Float {
        val x0 = x.toInt()
        val y0 = y.toInt()
        val x1 = min(x0 + 1, width - 1)
        val y1 = min(y0 + 1, height - 1)
        val fx = x - x0
        val fy = y - y0
        val top = a[(y0 * width + x0) * 4 + channel] * (1 - fx) + a[(y0 * width + x1) * 4 + channel] * fx
        val bottom = a[(y1 * width + x0) * 4 + channel] * (1 - fx) + a[(y1 * width + x1) * 4 + channel] * fx
        return top * (1 - fy) + bottom * fy
    }

    /** Sliding sums make each pass O(pixels), independent of the kernel width. */
    private fun box(source: FloatArray, target: FloatArray, width: Int, height: Int, radius: Int, horizontal: Boolean) {
        val length = if (horizontal) width else height
        val lines = if (horizontal) height else width
        val stride = if (horizontal) 4 else width * 4
        val divisor = (radius * 2 + 1).toFloat()
        for (line in 0 until lines) {
            val base = if (horizontal) line * width * 4 else line * 4
            for (channel in 0..3) {
                var sum = 0f
                for (i in -radius..radius) sum += source[base + i.coerceIn(0, length - 1) * stride + channel]
                for (i in 0 until length) {
                    target[base + i * stride + channel] = sum / divisor
                    sum -= source[base + (i - radius).coerceIn(0, length - 1) * stride + channel]
                    sum += source[base + (i + radius + 1).coerceIn(0, length - 1) * stride + channel]
                }
            }
        }
    }
}
