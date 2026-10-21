package com.donnclab.kinetiframe.domain.actor

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.model.Transform

/** Fit actual posed artwork, not its authoring origin, into a useful initial scene framing. */
object ActorStageFraming {
    fun fit(points: List<Offset>, stage: StageDimensions): Transform {
        require(points.isNotEmpty() && points.all { it.x.isFinite() && it.y.isFinite() })
        val left = points.minOf { it.x }
        val right = points.maxOf { it.x }
        val top = points.minOf { it.y }
        val bottom = points.maxOf { it.y }
        val scale = minOf(
            stage.width * .55f / (right - left).coerceAtLeast(1f),
            stage.height * .7f / (bottom - top).coerceAtLeast(1f),
        )
        return Transform(
            translation = Point2(
                stage.width / 2f - (left + right) / 2f * scale,
                stage.height * .85f - bottom * scale,
            ),
            stretch = Point2(scale, scale),
        )
    }
    fun outside(points: List<Offset>, stage: StageDimensions): Boolean = points.isNotEmpty() &&
        (
            points.maxOf { it.x } < 0 || points.minOf { it.x } > stage.width ||
                points.maxOf { it.y } < 0 || points.minOf { it.y } > stage.height
            )
}
