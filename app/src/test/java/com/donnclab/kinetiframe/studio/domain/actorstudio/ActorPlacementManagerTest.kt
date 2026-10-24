package com.donnclab.kinetiframe.studio.domain.actorstudio

import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.document.ActorPlacementReference
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.studio.StudioState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ActorPlacementManagerTest {
    private var state = StudioState()
    private val manager = ActorPlacementManager({ state }, { state = it(state) })

    @Test fun `placing repeated performance extends timeline without touching drawn layers`() {
        val layers = state.layers
        manager.add(ActorPlacementReference("grow", "wave", "actor", "revision", "Wave", 10, 24, repeatCount = 3))
        assertEquals(82, state.animation.totalFrames)
        assertEquals(layers, state.layers)
        manager.remove("grow")
        assertEquals(82, state.animation.totalFrames)
    }

    @Test fun `moving duplicating and switching clips extend their visible frame range`() {
        manager.add(ActorPlacementReference("grow", "wave", "actor", "revision", "Wave", 0, 24))
        manager.move("grow", 40)
        assertEquals(64, state.animation.totalFrames)
        manager.duplicate("grow")
        assertEquals(88, state.animation.totalFrames)
        manager.switchPerformance("grow", "long", "revision", "Long", 120)
        assertEquals(160, state.animation.totalFrames)
    }

    @Test fun `overflowing clip is rejected before mutating project`() {
        val before = state
        assertThrows(IllegalArgumentException::class.java) {
            manager.add(ActorPlacementReference("overflow", "wave", "actor", "revision", "Wave", Int.MAX_VALUE, 24))
        }
        assertEquals(before, state)
    }

    @Test fun `invalid clip timing cannot extend or mutate scene`() {
        val before = state
        for (clip in listOf(
            { ActorPlacementReference("bad", "wave", "actor", "revision", "Wave", -1, 24) },
            { ActorPlacementReference("bad", "wave", "actor", "revision", "Wave", 0, 0) },
            { ActorPlacementReference("bad", "wave", "actor", "revision", "Wave", 0, 24, repeatCount = 0) },
        )) {
            assertThrows(IllegalArgumentException::class.java) { manager.add(clip()) }
        }
        assertEquals(before, state)
    }

    @Test fun `duplicate creates an independent later placement`() {
        manager.add(
            ActorPlacementReference(
                "placement-a",
                "performance-wave",
                "actor-alice",
                "revision-a",
                "Alice · Wave",
                3,
                12,
            ),
        )
        manager.duplicate("placement-a")
        assertEquals(2, state.project.actorPlacements.size)
        assertEquals(15, state.project.actorPlacements.values.single { it.placementId != "placement-a" }.startFrame)
    }

    @Test fun `transform changes belong only to placement`() {
        manager.add(
            ActorPlacementReference(
                "placement-b",
                "performance-wave",
                "actor-alice",
                "revision-a",
                "Alice · Wave",
                0,
                12,
            ),
        )
        manager.updateTransform(
            "placement-b",
            Transform(
                translation = Point2(42f, 12f),
                rotationDegrees = 0f,
                stretch = Point2(1.5f, 1.5f),
            ),
        )
        assertEquals(42f, state.project.actorPlacements.getValue("placement-b").transform.x)
        assertEquals(1.5f, state.project.actorPlacements.getValue("placement-b").transform.scaleX)
    }

    @Test fun `trimming a track clip updates its start and duration`() {
        manager.add(
            ActorPlacementReference(
                "placement-c",
                "performance-wave",
                "actor-alice",
                "revision-a",
                "Alice · Wave",
                2,
                10,
            ),
        )
        manager.trim("placement-c", 5, 6)
        val placement = state.project.actorPlacements.getValue("placement-c")
        assertEquals(5, placement.startFrame)
        assertEquals(6, placement.durationFrames)
    }

    @Test fun `trimming clamps a negative start and a non-positive duration`() {
        manager.add(
            ActorPlacementReference(
                "placement-d",
                "performance-wave",
                "actor-alice",
                "revision-a",
                "Alice · Wave",
                2,
                10,
            ),
        )
        manager.trim("placement-d", -4, 0)
        val placement = state.project.actorPlacements.getValue("placement-d")
        assertEquals(2, placement.startFrame)
        assertEquals(1, placement.durationFrames)
    }

    @Test fun `trimming an unknown placement id is a no-op`() {
        manager.trim("missing", 3, 4)
        assertTrue(state.project.actorPlacements.isEmpty())
    }

    @Test fun `switching performance updates performance, revision, label and duration but not start or transform`() {
        manager.add(
            ActorPlacementReference(
                "placement-e",
                "performance-wave",
                "actor-alice",
                "revision-a",
                "Alice · Wave",
                4,
                12,
                transform = Transform(x = 10f, y = 5f),
            ),
        )
        manager.switchPerformance("placement-e", "performance-jump", "revision-b", "Alice · Jump", 8)
        val placement = state.project.actorPlacements.getValue("placement-e")
        assertEquals("performance-jump", placement.performanceId)
        assertEquals("revision-b", placement.actorRevisionId)
        assertEquals("Alice · Jump", placement.label)
        assertEquals(8, placement.durationFrames)
        assertEquals(4, placement.startFrame)
        assertEquals(10f, placement.transform.x)
    }

    @Test fun `switching performance clamps a non-positive duration`() {
        manager.add(
            ActorPlacementReference(
                "placement-f",
                "performance-wave",
                "actor-alice",
                "revision-a",
                "Alice · Wave",
                0,
                12,
            ),
        )
        manager.switchPerformance("placement-f", "performance-jump", "revision-b", "Alice · Jump", 0)
        assertEquals(1, state.project.actorPlacements.getValue("placement-f").durationFrames)
    }

    @Test fun `switching performance on an unknown placement id is a no-op`() {
        manager.switchPerformance("missing", "performance-jump", "revision-b", "Alice · Jump", 8)
        assertTrue(state.project.actorPlacements.isEmpty())
    }

    @Test fun `splitting a clip produces two adjacent placements that keep the original id on the first half`() {
        manager.add(
            ActorPlacementReference(
                "placement-g",
                "performance-wave",
                "actor-alice",
                "revision-a",
                "Alice · Wave",
                4,
                10,
                transform = Transform(x = 3f, y = 9f),
            ),
        )
        val secondId = manager.split("placement-g", 7)
        assertNotNull(secondId)
        assertEquals(2, state.project.actorPlacements.size)

        val first = state.project.actorPlacements.getValue("placement-g")
        assertEquals(4, first.startFrame)
        assertEquals(3, first.durationFrames)
        assertEquals(1, first.repeatCount)
        assertEquals(3f, first.transform.x)

        val second = state.project.actorPlacements.getValue(secondId!!)
        assertEquals(7, second.startFrame)
        assertEquals(7, second.durationFrames)
        assertEquals(1, second.repeatCount)
        assertEquals("performance-wave", second.performanceId)
        assertEquals("Alice · Wave", second.label)
        assertEquals(3f, second.transform.x)
        assertNotEquals("placement-g", secondId)
    }

    @Test fun `splitting forces both halves to repeatCount 1 even when the original repeated`() {
        manager.add(
            ActorPlacementReference(
                "placement-h",
                "performance-wave",
                "actor-alice",
                "revision-a",
                "Alice · Wave",
                0,
                10,
                repeatCount = 3,
            ),
        )
        val secondId = manager.split("placement-h", 25)
        val first = state.project.actorPlacements.getValue("placement-h")
        val second = state.project.actorPlacements.getValue(secondId!!)
        assertEquals(1, first.repeatCount)
        assertEquals(1, second.repeatCount)
        assertEquals(25, first.durationFrames)
        assertEquals(5, second.durationFrames)
    }

    @Test fun `splitting at or beyond either edge is a no-op`() {
        manager.add(
            ActorPlacementReference(
                "placement-i",
                "performance-wave",
                "actor-alice",
                "revision-a",
                "Alice · Wave",
                4,
                10,
            ),
        )
        assertNull(manager.split("placement-i", 4))
        assertNull(manager.split("placement-i", 14))
        assertNull(manager.split("placement-i", 2))
        assertNull(manager.split("placement-i", 20))
        assertEquals(1, state.project.actorPlacements.size)
    }

    @Test fun `splitting an unknown placement id is a no-op`() {
        assertNull(manager.split("missing", 5))
        assertTrue(state.project.actorPlacements.isEmpty())
    }
}
