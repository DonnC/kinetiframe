package com.donnclab.kinetiframe.engine.camera

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

class CameraAdvancedTest {
    @Test fun `wait move settle remain inside the selected range`() {
        val shot =
            CameraShot("pan", CameraAction.PAN, 10, 20, end = CameraPose(x = .8f), waitFrames = 3, settleFrames = 2)
        for (frame in 10..13) assertEquals(shot.start, SceneCamera.evaluate(listOf(shot), frame, 12))
        assertEquals(.65f, SceneCamera.evaluate(listOf(shot), 15, 12).x, .0001f)
        for (frame in 17..19) assertEquals(shot.end, SceneCamera.evaluate(listOf(shot), frame, 12))
        assertEquals(CameraPose(), SceneCamera.evaluate(listOf(shot), 20, 12))
    }

    @Test fun `shortened ranges safely clamp phase budgets`() {
        val shot =
            CameraShot("pan", CameraAction.PAN, 0, 2, waitFrames = 20, settleFrames = 20, end = CameraPose(x = .8f))
        assertEquals(shot.start, SceneCamera.evaluate(listOf(shot), 0, 12))
        assertEquals(shot.end, SceneCamera.evaluate(listOf(shot), 1, 12))
    }

    @Test fun `direction frequency and rotation are deterministic and bounded`() {
        val shot = CameraShot(
            "shake", CameraAction.SHAKE, 0, 48, frequencyHz = 3f,
            shakeDirection = ShakeDirection.HORIZONTAL, rotationStrength = 1f, waitFrames = 4, settleFrames = 4,
        )
        val pose = SceneCamera.evaluate(listOf(shot), 20, 12)
        assertEquals(.5f, pose.y)
        assertNotEquals(.5f, pose.x)
        assertNotEquals(0f, pose.rotation)
        assertEquals(pose, SceneCamera.evaluate(listOf(shot), 20, 12))
        for (frame in listOf(
            0,
            3,
            43,
            47,
            48,
            100,
        )) {
            assertEquals(CameraPose(), SceneCamera.evaluate(listOf(shot), frame, 12))
        }
    }

    @Test fun `presets serialize and retime without extending occupied range`() {
        val reveal = CameraRecipes.builtIn(12).first().place("reveal", 0, 36, CameraPose())
        assertEquals(1.8f, reveal.start.zoom, .0001f)
        assertEquals(1f, reveal.end.zoom, .0001f)
        CameraRecipes.builtIn(12).forEach { recipe ->
            assertEquals(recipe, Json.decodeFromString<CameraRecipe>(Json.encodeToString(recipe)))
            val placed = recipe.retime(24).place("new", 8, 16, CameraPose())
            assertEquals(8, placed.startFrame)
            assertEquals(16, placed.endFrame)
            assertEquals(CameraPose(), SceneCamera.evaluate(listOf(placed), 16, 24))
        }
    }

    @Test fun `follow missing target is safe and target frames respect phases`() {
        val shot =
            CameraShot(
                "follow",
                CameraAction.FOLLOW,
                0,
                12,
                waitFrames = 2,
                settleFrames = 3,
                targetPlacementId = "actor",
            )
        assertEquals(CameraPose(), SceneCamera.evaluate(listOf(shot), 4, 12))
        var sampled = -1
        val pose = SceneCamera.evaluate(listOf(shot), 11, 12) { _, frame ->
            sampled = frame
            .7f to .6f
        }
        assertEquals(8, sampled)
        assertEquals(.7f, pose.x, .0001f)
        assertEquals(CameraPose(), SceneCamera.evaluate(listOf(shot), 12, 12) { _, _ -> .7f to .6f })
    }
}
