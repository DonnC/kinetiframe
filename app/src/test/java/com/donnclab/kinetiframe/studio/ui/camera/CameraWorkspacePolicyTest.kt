package com.donnclab.kinetiframe.studio.ui.camera

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.document.DrawingTool
import com.donnclab.kinetiframe.studio.StudioIntent
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class CameraWorkspacePolicyTest {
    private val navigation = StudioIntent.View.UpdateTransform(Offset(10f, 0f), 1.2f, 0f, Offset.Zero)
    private val edit = StudioIntent.Draw.SelectTool(DrawingTool.BRUSH)

    @Test fun `project opens editable even when it has camera shots`() {
        assertFalse(CameraWorkspacePolicy.PREVIEW_ON_OPEN)
        assertTrue(CameraWorkspacePolicy.acceptsIntent(false, false, edit))
        assertTrue(CameraWorkspacePolicy.acceptsIntent(false, false, navigation))
    }

    @Test fun `audience preview accepts workspace navigation but never artwork mutation`() {
        assertTrue(CameraWorkspacePolicy.acceptsIntent(true, false, navigation))
        assertFalse(CameraWorkspacePolicy.acceptsIntent(true, false, edit))
        assertFalse(CameraWorkspacePolicy.acceptsIntent(true, false, StudioIntent.Draw.ClearAllHandwriting))
        assertFalse(CameraWorkspacePolicy.acceptsIntent(true, true, navigation))
        assertFalse(CameraWorkspacePolicy.acceptsIntent(false, true, edit))
    }

    @Test fun `explicit drawing tool choice returns to artwork without changing camera shots`() {
        assertTrue(CameraWorkspacePolicy.selectsArtwork(edit))
        assertTrue(CameraWorkspacePolicy.selectsArtwork(StudioIntent.Draw.SelectTool(DrawingTool.ERASER)))
        assertFalse(CameraWorkspacePolicy.selectsArtwork(navigation))
        assertFalse(CameraWorkspacePolicy.selectsArtwork(StudioIntent.Playback.Play))
    }
}
