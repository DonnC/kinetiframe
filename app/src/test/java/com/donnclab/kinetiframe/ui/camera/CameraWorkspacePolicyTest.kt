package com.donnclab.kinetiframe.ui.camera

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.components.camera.CameraWorkspacePolicy
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CameraWorkspacePolicyTest {
    private val navigation = TimelineIntent.View.UpdateTransform(Offset(10f, 0f), 1.2f, 0f, Offset.Zero)
    private val edit = TimelineIntent.Draw.SelectTool(DrawingTool.BRUSH)

    @Test fun `project opens editable even when it has camera shots`() {
        assertFalse(CameraWorkspacePolicy.PREVIEW_ON_OPEN)
        assertTrue(CameraWorkspacePolicy.acceptsIntent(false, false, edit))
        assertTrue(CameraWorkspacePolicy.acceptsIntent(false, false, navigation))
    }

    @Test fun `audience preview accepts workspace navigation but never artwork mutation`() {
        assertTrue(CameraWorkspacePolicy.acceptsIntent(true, false, navigation))
        assertFalse(CameraWorkspacePolicy.acceptsIntent(true, false, edit))
        assertFalse(CameraWorkspacePolicy.acceptsIntent(true, false, TimelineIntent.Draw.ClearAllHandwriting))
        assertFalse(CameraWorkspacePolicy.acceptsIntent(true, true, navigation))
        assertFalse(CameraWorkspacePolicy.acceptsIntent(false, true, edit))
    }

    @Test fun `explicit drawing tool choice returns to artwork without changing camera shots`() {
        assertTrue(CameraWorkspacePolicy.selectsArtwork(edit))
        assertTrue(CameraWorkspacePolicy.selectsArtwork(TimelineIntent.Draw.SelectTool(DrawingTool.ERASER)))
        assertFalse(CameraWorkspacePolicy.selectsArtwork(navigation))
        assertFalse(CameraWorkspacePolicy.selectsArtwork(TimelineIntent.Playback.Play))
    }
}
