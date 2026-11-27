package com.donnclab.kinetiframe.actors.rig.math

import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.engine.rigging.math.BendLimits
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import kotlin.random.Random

class RigidMathBridgeTest {
    @Test fun `neutral IK adapter matches frozen Point2 results for both sides and limits`() {
        val random = Random(14)
        repeat(200) {
            val chain = listOf(
                Point2(3f, 4f),
                Point2(3f + random.nextFloat() * 80f + 10f, 4f),
                Point2(100f + random.nextFloat() * 30f, 20f + random.nextFloat() * 30f),
            )
            val target = if (it % 10 ==
                0
            ) {
                chain.first()
            } else {
                Point2(random.nextFloat() * 400f - 200f, random.nextFloat() * 400f - 200f)
            }
            val limits = if (it % 2 == 0) BendLimits() else BendLimits(10f, 120f)
            listOf(-1, 1).forEach { side ->
                val expected = LegacyTwoBoneReference.solve(chain, target, side, limits)
                val actual = TwoBoneSolver.solve(chain, target, side, limits)
                expected.zip(actual).forEach { (old, value) ->
                    assertEquals(old.x.toRawBits(), value.x.toRawBits())
                    assertEquals(old.y.toRawBits(), value.y.toRawBits())
                }
            }
        }
    }

    @Test fun `solver rejection diagnostics and first checks retain legacy behavior`() {
        val chain = listOf(Point2.Zero, Point2(10f, 0f), Point2(20f, 0f))
        listOf(0, 2).forEach { side ->
            assertEquals(
                "Failed requirement.",
                assertThrows(IllegalArgumentException::class.java) {
                    TwoBoneSolver.solve(chain, Point2.Zero, side)
                }.message,
            )
        }
        assertEquals(
            "Failed requirement.",
            assertThrows(IllegalArgumentException::class.java) {
                TwoBoneSolver.solve(chain, Point2(Float.NaN, 0f), 1)
            }.message,
        )
        assertEquals(
            "Failed requirement.",
            assertThrows(IllegalArgumentException::class.java) {
                TwoBoneSolver.solve(chain, Point2.Zero, 1, BendLimits(30f, 10f))
            }.message,
        )
    }

    @Test fun `rigid cycle bridge keeps old diagnostic`() {
        val graph = SceneGraph(bones = mapOf("a" to BoneNode("a", "a", "A", 1f)))
        assertEquals(
            "Cyclic skeleton",
            assertThrows(IllegalStateException::class.java) {
                RigGeometry.resolve(graph)
            }.message,
        )
    }
}
