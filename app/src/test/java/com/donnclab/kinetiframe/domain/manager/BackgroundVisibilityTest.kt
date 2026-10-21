package com.donnclab.kinetiframe.domain.manager

import com.donnclab.kinetiframe.engine.asset.ProjectAssetManager
import com.donnclab.kinetiframe.engine.model.AssetKind
import com.donnclab.kinetiframe.engine.model.ProjectAsset
import com.donnclab.kinetiframe.engine.model.ProjectDocument
import com.donnclab.kinetiframe.ui.CanvasState
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.TimelineState
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class BackgroundVisibilityTest {
    @Test fun `visibility toggles without changing saved metadata or cache ownership`() {
        val project = ProjectDocument(
            backgroundAssetId = "stage",
            assets = mapOf(
                "stage" to
                    ProjectAsset("stage", AssetKind.IMPORTED_IMAGE, "retained-stage.png", 100, 100),
            ),
        )
        var state = TimelineState(project = project)
        val assets = ProjectAssetManager()
        val dispatcher = StandardTestDispatcher()
        val manager = BackgroundManager(
            TestScope(dispatcher),
            dispatcher,
            assets,
            File("unused-test-storage"),
            { state },
            { state = it(state) },
            decode = { error("Visibility must not decode an image") },
        )
        val revision = assets.revision.value
        manager.handle(TimelineIntent.Background.ToggleVisibility)
        assertFalse(state.canvas.backgroundVisible)
        assertSame(project, state.project)
        assertEquals(revision, assets.revision.value)
        manager.handle(TimelineIntent.Background.ToggleVisibility)
        assertTrue(state.canvas.backgroundVisible)
        assertSame(project, state.project)
        assertEquals(revision, assets.revision.value)
        // The preference is not part of ProjectDocument; a new editor starts visible.
        assertTrue(TimelineState(project = state.project).canvas.backgroundVisible)
    }

    @Test fun `toggle without a background is a no-op`() {
        var state = TimelineState()
        val initial = state
        val dispatcher = StandardTestDispatcher()
        val manager =
            BackgroundManager(
                TestScope(dispatcher),
                dispatcher,
                ProjectAssetManager(),
                File("unused-test-storage"),
                { state },
                { state = it(state) },
            )
        manager.handle(TimelineIntent.Background.ToggleVisibility)
        assertSame(initial, state)
    }

    @Test fun `canvas equality includes background visibility so StateFlow publishes changes`() {
        val visible = CanvasState()
        val hidden = visible.copy(backgroundVisible = false)
        assertNotEquals(visible, hidden)
        assertNotEquals(visible.hashCode(), hidden.hashCode())
        assertEquals(hidden, hidden.copy(matrixValues = hidden.matrixValues.copyOf()))
    }
}
