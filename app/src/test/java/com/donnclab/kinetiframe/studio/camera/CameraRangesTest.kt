package com.donnclab.kinetiframe.studio.camera

import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.core.document.camera.CameraPose
import com.donnclab.kinetiframe.core.document.camera.CameraShot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CameraRangesTest {
    private val shake = CameraShot("shake", CameraAction.SHAKE, 8, 21)

    @Test fun `new ranges skip occupied frames and stop at next effect`() {
        assertEquals(21 until 45, CameraRanges.available(listOf(shake), 96, 9, 24))
        assertEquals(0 until 8, CameraRanges.available(listOf(shake), 96, 0, 24))
    }

    @Test fun `range selection wraps and full timelines cannot create another effect`() {
        assertEquals(0 until 8, CameraRanges.available(listOf(shake.copy(endFrame = 96)), 96, 90, 24))
        assertNull(CameraRanges.available(listOf(shake.copy(startFrame = 0, endFrame = 96)), 96, 0, 24))
    }

    @Test fun `editing excludes own range but detects others immediately`() {
        assertTrue(CameraRanges.conflicts(shake, listOf(shake)).isEmpty())
        val other = CameraShot("pan", CameraAction.PAN, 21, 48)
        assertTrue(CameraRanges.conflicts(shake, listOf(shake, other)).isEmpty())
        assertEquals(listOf(other), CameraRanges.conflicts(shake.copy(endFrame = 22), listOf(shake, other)))
    }

    @Test fun `poster skips black opening but has bounded fallback for wholly faded projects`() {
        val fade = SceneCamera.preset("fade", CameraAction.FADE_IN, 0, 12, CameraPose())
        val frame = CameraPosterFrame.select(listOf(fade), 96, 12)
        assertTrue(frame in 1..11)
        assertTrue(SceneCamera.evaluate(listOf(fade), frame, 12).black <= .02f)
        assertEquals(0, CameraPosterFrame.select(emptyList(), 96, 12))
        assertEquals(
            0,
            CameraPosterFrame.select(
                listOf(
                    fade.copy(
                        action = CameraAction.FADE_OUT,
                        start = CameraPose(black = 1f),
                        end = CameraPose(black = 1f),
                    ),
                ),
                12,
                12,
            ),
        )
    }
}
