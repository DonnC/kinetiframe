package com.donnclab.kinetiframe.ui

import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.data.actor.ActorManifest
import com.donnclab.kinetiframe.data.actor.ActorRevision
import com.donnclab.kinetiframe.domain.manager.ActorStudioManager
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplates
import com.donnclab.kinetiframe.engine.rigging.model.DrawingBinding
import com.donnclab.kinetiframe.engine.rigging.model.DrawingComposition
import com.donnclab.kinetiframe.engine.rigging.model.DrawingExtent
import com.donnclab.kinetiframe.engine.rigging.model.DrawingPlacement
import com.donnclab.kinetiframe.engine.rigging.model.DrawingSource
import com.donnclab.kinetiframe.engine.rigging.model.SkinSourceSize
import com.donnclab.kinetiframe.models.Layer
import com.donnclab.kinetiframe.models.LayerType
import com.donnclab.kinetiframe.ui.actors.actorStudioInitialState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test

class ActorStudioStateTest {
    @Test fun `a bound continuous sketch suppresses the old raster reference without affecting cutout mode`() {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        ActorStudioManager({ state }, { state = it(state) }).handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        val definition = state.actorDefinition()!!
        val graph = definition.sceneGraph
        val drawing = com.donnclab.kinetiframe.engine.rigging.model.ActorDrawing(
            DrawingSource("ink", "colour", SkinSourceSize(32, 16)),
            DrawingPlacement(Point2(0f, 0f), DrawingExtent(32f, 16f)),
            DrawingBinding(
                com.donnclab.kinetiframe.engine.rigging.math.ActorDrawingBinder.bind(
                    graph,
                    SkinSourceSize(32, 16),
                    DrawingPlacement(Point2(0f, 0f), DrawingExtent(32f, 16f)),
                ),
            ),
        )
        state = state.copy(
            project = state.project.copy(
                puppetDefinitions = state.project.puppetDefinitions +
                    (definition.id to definition.copy(sceneGraph = graph.copy(drawing = drawing))),
            ),
        )
        assertFalse(state.showActorSourceArtwork())
        state = state.copy(
            project = state.project.copy(
                puppetDefinitions = state.project.puppetDefinitions +
                    (definition.id to definition.copy(sceneGraph = graph.copy(drawing = drawing.withEnabled(false)))),
            ),
        )
        org.junit.jupiter.api.Assertions.assertTrue(state.showActorSourceArtwork())
    }

    @org.junit.jupiter.api.Test fun `performance stage never shows duplicate source artwork`() {
        val setup = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        org.junit.jupiter.api.Assertions.assertTrue(setup.showActorSourceArtwork())
        val performance = setup.copy(scene = setup.scene.copy(studioMode = StudioMode.ANIMATE_IK))
        org.junit.jupiter.api.Assertions.assertFalse(performance.showActorSourceArtwork())
        org.junit.jupiter.api.Assertions.assertFalse(
            setup.copy(actor = setup.actor.copy(showSource = false)).showActorSourceArtwork(),
        )
    }

    @Test
    fun `a project-launched session starts as motion-only performance authoring`() {
        val state = actorStudioInitialState(
            manifest = ActorManifest(
                actorId = "actor-alice",
                displayName = "Alice",
                templateId = ActorManifest.HUMAN_TEMPLATE_ID,
                currentRevisionId = "revision-1",
                createdAtEpochMs = 1,
                updatedAtEpochMs = 1,
            ),
            revision = ActorRevision(
                revisionId = "revision-1",
                actorId = "actor-alice",
                sceneGraph = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd),
                createdAtEpochMs = 1,
            ),
            performanceAuthoring = true,
        )

        assertEquals(StudioMode.ANIMATE_IK, state.scene.studioMode)
        assertEquals(true, state.actor.isPerformanceAuthoring)
    }

    @Test
    fun `a selected artwork part still resolves its document actor`() {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        val manager = ActorStudioManager({ state }, { state = it(state) })
        manager.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        val actor = requireNotNull(state.activeActor())
        val partLayer = Layer(
            name = "Left Forearm",
            id = "part-left-forearm",
            type = LayerType.RASTER,
            boundBoneId = "left_forearm",
        )
        val layers = state.currentFrameLayers() + partLayer
        state = state.copy(
            layers = state.layers.copy(
                layers = layers,
                frameLayers = state.layers.frameLayers + (state.animation.currentFrame to layers),
                activeLayerIndex = layers.lastIndex,
            ),
        )

        assertEquals(actor.id, state.activeActor()?.id)
        assertEquals(actor.definitionId, state.actorDefinition()?.id)
    }

    @Test
    fun `performance authoring rejects structural actor commands`() {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.ANIMATE_IK))
        val manager = ActorStudioManager({ state }, { state = it(state) })
        manager.handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        state = state.copy(actor = state.actor.copy(isPerformanceAuthoring = true))
        val projectBefore = state.project

        manager.handle(StudioIntent.Actor.Unbind)
        manager.handle(StudioIntent.Actor.RenameActor("Different actor"))

        assertEquals(projectBefore, state.project)
        assertFalse(state.scene.studioMode == StudioMode.RASTER_DRAWING)
    }
}
