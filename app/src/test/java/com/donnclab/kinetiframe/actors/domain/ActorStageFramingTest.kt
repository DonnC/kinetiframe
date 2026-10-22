package com.donnclab.kinetiframe.actors.domain

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.project.model.StageDimensions
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorStageFramingTest {
    @Test fun `far away authored artwork fits landscape portrait and square stages`() {
        val points = listOf(Offset(900f, 200f), Offset(1200f, 900f))
        listOf(StageDimensions(640, 360), StageDimensions.Vertical, StageDimensions.Square).forEach { stage ->
            val fit = ActorStageFraming.fit(points, stage)
            points.forEach { point ->
                assertTrue(point.x * fit.scaleX + fit.x in 0f..stage.width.toFloat())
                assertTrue(point.y * fit.scaleY + fit.y in 0f..stage.height.toFloat())
            }
        }
    }

    @Test fun `legacy repair distinguishes wholly offstage from partially visible`() {
        val stage = StageDimensions(640, 360)
        assertTrue(ActorStageFraming.outside(listOf(Offset(900f, 200f), Offset(1200f, 900f)), stage))
        assertFalse(ActorStageFraming.outside(listOf(Offset(600f, 200f), Offset(1200f, 900f)), stage))
    }
}
