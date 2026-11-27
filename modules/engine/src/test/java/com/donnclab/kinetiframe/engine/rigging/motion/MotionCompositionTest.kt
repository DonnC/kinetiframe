// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.engine.rigging.edit.RigEditFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MotionCompositionTest {
    private val rest = RigEditFixtures.graph.let { graph ->
        val upper = graph.bones.getValue("upper")
        graph.copy(
            bones =
            graph.bones +
                (upper.id to upper.withRest(localTransform = upper.localTransform.copy(rotationDegrees = 20f))),
        )
    }

    @Test fun `preset offsets add to rest rather than sampled keys and recordings override zero too`() {
        val keys = mapOf("upper" to 100f, "lower" to 30f)
        val preset = mapOf("upper" to 5f, "missing" to 7f)
        assertEquals(
            mapOf("upper" to 25f, "lower" to 30f, "missing" to 7f),
            MotionComposition.rotations(rest, keys, preset, emptyMap()),
        )
        assertEquals(0f, MotionComposition.rotations(rest, keys, preset, mapOf("upper" to 0f)).getValue("upper"))
        assertEquals(100f, keys.getValue("upper"))
        assertEquals(20f, rest.bones.getValue("upper").localTransform.rotationDegrees)
    }

    @Test fun `root channels override whole displacement values instead of adding axes`() {
        val keys = mapOf("root" to RootTranslation(10f, 20f), "other" to RootTranslation(1f, 2f))
        val preset = mapOf("root" to RootTranslation(30f, 40f))
        val recorded = mapOf("root" to RootTranslation())
        assertEquals(
            RootTranslation(30f, 40f),
            MotionComposition.rootTranslations(keys, preset, emptyMap()).getValue("root"),
        )
        assertEquals(
            mapOf("root" to RootTranslation(), "other" to RootTranslation(1f, 2f)),
            MotionComposition.rootTranslations(keys, preset, recorded),
        )
    }

    @Test fun `overlay retains map order and missing channels do not erase lower layers`() {
        val result = MotionComposition.rotations(
            rest,
            linkedMapOf("upper" to 1f, "lower" to 2f),
            linkedMapOf("lower" to 3f, "hand" to 4f),
            linkedMapOf("upper" to 5f, "other" to 6f),
        )
        assertEquals(listOf("upper", "lower", "hand", "other"), result.keys.toList())
        assertEquals(4f, result.getValue("hand"))
        assertTrue(MotionComposition.rotations(rest, emptyMap(), emptyMap(), emptyMap()).isEmpty())
        assertTrue(MotionComposition.rootTranslations(emptyMap(), emptyMap(), emptyMap()).isEmpty())
    }
}
