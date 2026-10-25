package com.donnclab.kinetiframe.studio.domain

import android.graphics.Region
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import kotlin.math.ceil
import kotlin.math.floor

/** One mask per gesture. Region intersection also finds edge-crossings and enclosed areas. */
internal class SelectionAreaMask private constructor(private val region: Region, private val tap: Offset?) {
    val isTap: Boolean get() = tap != null
    fun matches(quads: List<List<Offset>>, partial: Boolean): Boolean {
        if (quads.isEmpty()) return false
        val objectRegion = Region()
        quads.forEach { quad ->
            if (quad.size < 3) return@forEach
            val outline = android.graphics.Path().apply {
                moveTo(quad[0].x, quad[0].y)
                quad.drop(1).forEach { lineTo(it.x, it.y) }
                close()
            }
            objectRegion.op(regionOf(outline), Region.Op.UNION)
        }
        tap?.let { return objectRegion.contains(it.x.toInt(), it.y.toInt()) }
        if (objectRegion.isEmpty) return false
        val remainder = Region(objectRegion)
        remainder.op(region, if (partial) Region.Op.INTERSECT else Region.Op.DIFFERENCE)
        return if (partial) !remainder.isEmpty else remainder.isEmpty
    }
    companion object {
        operator fun invoke(path: Path?, rect: Rect?): SelectionAreaMask? {
            if (rect !=
                null
            ) {
                return SelectionAreaMask(
                    Region(
                        floor(rect.left).toInt(),
                        floor(rect.top).toInt(),
                        ceil(rect.right).toInt(),
                        ceil(rect.bottom).toInt(),
                    ),
                    rect.center.takeIf {
                        rect.width <
                            3f &&
                            rect.height < 3f
                    },
                )
            }
            val outline = path?.asAndroidPath() ?: return null
            val bounds = android.graphics.RectF().also { outline.computeBounds(it, true) }
            return SelectionAreaMask(
                regionOf(outline),
                Offset(bounds.centerX(), bounds.centerY()).takeIf {
                    bounds.width() <
                        3f &&
                        bounds.height() < 3f
                },
            )
        }
        private fun regionOf(path: android.graphics.Path): Region {
            val bounds = android.graphics.RectF().also { path.computeBounds(it, true) }
            return Region().apply {
                setPath(
                    path,
                    Region(
                        floor(bounds.left).toInt(),
                        floor(bounds.top).toInt(),
                        ceil(bounds.right).toInt(),
                        ceil(bounds.bottom).toInt(),
                    ),
                )
            }
        }
    }
}
