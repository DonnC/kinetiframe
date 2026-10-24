package com.donnclab.kinetiframe.studio

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.donnclab.kinetiframe.actors.data.PerformanceRepository
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.VideoClipPlacement
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.studio.ui.timeline.TimelineTracksPanel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SceneVideoControlsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun clipContextTransformsVideoRatherThanOpeningImportOrPaintingFrames() {
        val clip = VideoClipPlacement("clip", "asset", "Screen story", 0, 24, isSceneContent = true)
        val state = StudioState(
            project = ProjectDocument(videoClips = mapOf("clip" to clip)),
            animation = AnimationState(totalFrames = 24),
        )
        var dispatched: StudioIntent? = null
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val performances = PerformanceRepository(DocumentFileStore(context.filesDir))
        compose.setContent {
            KinetiFrameTheme {
                TimelineTracksPanel(
                    state = state,
                    onIntent = { dispatched = it },
                    performanceRepository = performances,
                    onEditPlacement = {},
                    onAddToLibrary = {},
                    workspaceMode = true,
                )
            }
        }
        compose.onNodeWithText("Screen story").performClick()
        compose.onNodeWithContentDescription("Transform video").performClick()
        compose.runOnIdle { assertEquals(StudioIntent.Video.Select("clip"), dispatched) }
    }
}
