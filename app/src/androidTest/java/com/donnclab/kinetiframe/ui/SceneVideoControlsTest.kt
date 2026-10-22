package com.donnclab.kinetiframe.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.engine.model.VideoClipPlacement
import com.donnclab.kinetiframe.ui.components.timeline.TimelineTracksPanel
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
        val performances = PerformanceRepository(ActorFileStore(context.filesDir))
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
