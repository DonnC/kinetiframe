package com.donnclab.kinetiframe.studio.camera

import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.core.document.camera.CameraPose
import com.donnclab.kinetiframe.core.document.camera.CameraShot
import com.donnclab.kinetiframe.core.document.camera.CameraShots
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SceneCameraTest {
    @Test fun `every effect is temporary unless final state is explicitly held`() {
        CameraAction.entries.forEach { action ->
            val shot = SceneCamera.preset(action.name, action, 8, 21, CameraPose())
            for (frame in 21 until 96) {
                assertEquals(
                    CameraPose(),
                    SceneCamera.evaluate(listOf(shot), frame, 12),
                    action.name,
                )
            }
            if (action != CameraAction.SHAKE && action != CameraAction.HANDHELD) {
                assertEquals(shot.end, SceneCamera.evaluate(listOf(shot.copy(holdEnd = true)), 95, 12))
            }
        }
    }

    @Test fun `impact shake on frames 9 to 21 ends exactly and never becomes later permanent framing`() {
        val shot = SceneCamera.preset("impact", CameraAction.SHAKE, 8, 21, CameraPose())
        assertNotEquals(CameraPose(), SceneCamera.evaluate(listOf(shot), 12, 12))
        assertEquals(CameraPose(), SceneCamera.evaluate(listOf(shot), 20, 12))
        for (frame in 21 until 96) assertEquals(CameraPose(), SceneCamera.evaluate(listOf(shot), frame, 12))
        val stable = SceneCamera.stablePose(listOf(shot), 12, 12)
        assertEquals(CameraPose(), stable)
        val later = SceneCamera.preset("later", CameraAction.FADE_OUT, 30, 40, stable)
        assertEquals(.5f, later.end.x)
        assertEquals(.5f, later.end.y)
    }

    @Test fun `inserting and deleting scene frames preserves nonoverlapping camera ranges`() {
        val a = CameraShot("a", CameraAction.PAN, 0, 10)
        val b = CameraShot("b", CameraAction.PAN, 10, 20)
        val inserted = CameraFrameStructure.reindex(listOf(a, b), 5, emptyList())
        assertEquals(11, inserted[0].endFrame)
        assertEquals(11, inserted[1].startFrame)
        CameraShots.validate(inserted)
        val removed = CameraFrameStructure.reindex(listOf(a, b), null, (0 until 10).toList())
        assertEquals(listOf(b.copy(startFrame = 0, endFrame = 10)), removed)
    }

    @Test fun `identity framing preserves stage coordinates`() {
        val matrix = SceneCamera.matrix(CameraPose(), 400, 200)
        assertEquals(32f, matrix.mapX(32f, 81f), .001f)
        assertEquals(81f, matrix.mapY(32f, 81f), .001f)
    }

    @Test fun `start end and gaps hold exact framing`() {
        val shot = SceneCamera.preset("zoom", CameraAction.PUSH_IN, 2, 7, CameraPose())
        assertEquals(CameraPose(), SceneCamera.evaluate(listOf(shot), 1, 24))
        assertEquals(shot.start, SceneCamera.evaluate(listOf(shot), 2, 24))
        assertEquals(shot.end, SceneCamera.evaluate(listOf(shot), 6, 24))
        assertEquals(CameraPose(), SceneCamera.evaluate(listOf(shot), 100, 24))
        assertEquals(shot.end, SceneCamera.evaluate(listOf(shot.copy(holdEnd = true)), 100, 24))
        assertEquals(1.25f, SceneCamera.evaluate(listOf(shot), 4, 24).zoom, .001f)
    }

    @Test fun `single frame shot reaches its endpoint`() {
        val shot = SceneCamera.preset("one", CameraAction.FADE_OUT, 0, 1, CameraPose())
        assertEquals(1f, SceneCamera.evaluate(listOf(shot), 0, 24).black)
    }

    @Test fun `shake is deterministic without altering held framing`() {
        val zoom = SceneCamera.preset("zoom", CameraAction.PUSH_IN, 0, 10, CameraPose()).copy(holdEnd = true)
        val shake = SceneCamera.preset("shake", CameraAction.SHAKE, 10, 40, zoom.end)
        val shots = listOf(zoom, shake)
        val expected = SceneCamera.evaluate(shots, 20, 24)
        assertNotEquals(zoom.end, expected)
        SceneCamera.evaluate(shots, 32, 24)
        assertEquals(expected, SceneCamera.evaluate(shots, 20, 24))
        assertEquals(zoom.end, SceneCamera.evaluate(shots, 40, 24))
        assertEquals(zoom.end, SceneCamera.evaluate(shots, 10, 24))
    }

    @Test fun `overlap and duplicate ids reject but adjacency is allowed`() {
        val a = CameraShot("a", CameraAction.PAN, 0, 10)
        CameraShots.validate(listOf(a, a.copy(id = "b", startFrame = 10, endFrame = 20)))
        assertThrows(IllegalArgumentException::class.java) {
            CameraShots.validate(listOf(a, a.copy(id = "b", startFrame = 9)))
        }
        assertThrows(IllegalArgumentException::class.java) {
            CameraShots.validate(listOf(a, a.copy(startFrame = 10, endFrame = 20)))
        }
    }

    @Test fun `camera saves and legacy documents default to identity`() {
        val json = Json { encodeDefaults = true }
        val project = ProjectDocument(cameraShots = listOf(CameraShot("pan", CameraAction.PAN, 0, 12)))
        assertEquals(
            project,
            json.decodeFromString<ProjectDocument>(json.encodeToString(ProjectDocument.serializer(), project)),
        )
        assertTrue(Json.decodeFromString<ProjectDocument>("{}").cameraShots.isEmpty())
    }

    @Test fun `touch framing moves opposite image drag and bounds zoom`() {
        val pose = CameraFraming.drag(CameraPose(), 100f, 0f, 2f, 0f, 400, 200)
        assertEquals(.25f, pose.x)
        assertEquals(2f, pose.zoom)
        val matrix = SceneCamera.matrix(pose, 400, 200)
        assertEquals(200f, matrix.mapX(100f, 100f))
        assertEquals(4f, CameraFraming.drag(pose, 0f, 0f, 100f, 0f, 400, 200).zoom)
        assertEquals(-90f, CameraFraming.drag(CameraPose(), 0f, 0f, 1f, 90f, 400, 200).rotation)
    }

    @Test fun `rotation follows shortest arc`() {
        val shot = CameraShot(
            "turn",
            CameraAction.TILT,
            0,
            3,
            CameraPose(rotation = 170f),
            CameraPose(rotation = -170f),
        )
        assertEquals(-180f, SceneCamera.evaluate(listOf(shot), 1, 24).rotation)
    }
}
