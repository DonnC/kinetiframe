package com.donnclab.kinetiframe.ui.components.rigging

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.math.RigGeometry
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplates

/** Draw the actual starter graph, not unrelated character art. Geometry is compiled off draw. */
@Composable
fun RigBlueprintThumbnail(template: ActorTemplate, modifier: Modifier = Modifier) {
    val segments = remember(template) {
        val joints = RigGeometry.resolve(ActorTemplates.create(template, StageDimensions(1000, 800)))
        val points = joints.values.flatMap { listOf(it.start, it.end) }
        val left = points.minOf { it.x }
        val top = points.minOf { it.y }
        val span = maxOf(points.maxOf { it.x } - left, points.maxOf { it.y } - top, 1f)
        joints.values.map { (it.start - Offset(left, top)) / span to (it.end - Offset(left, top)) / span }
    }
    val color = MaterialTheme.colorScheme.primary
    Canvas(modifier) {
        val scale = minOf(size.width, size.height) * .85f
        val origin = Offset(size.width * .08f, size.height * .08f)
        segments.forEach { (a, b) ->
            val start = origin + a * scale
            val end = origin + b * scale
            drawLine(color, start, end, strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            drawCircle(color, radius = 2.dp.toPx(), center = start)
            drawCircle(color, radius = 2.dp.toPx(), center = end)
        }
    }
}

@Preview
@Composable
private fun RigBlueprintPreview() = RigBlueprintThumbnail(ActorTemplate.HUMAN, Modifier.size(80.dp))
