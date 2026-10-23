package com.donnclab.kinetiframe.studio

import com.donnclab.kinetiframe.actors.domain.ActorStudioManager
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.actors.rig.presets.RotationChannel
import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.motion.RootMotionCurve
import com.donnclab.kinetiframe.core.actor.motion.RootMotionPoint
import com.donnclab.kinetiframe.core.actor.motion.RootTranslation
import com.donnclab.kinetiframe.core.actor.motion.RotationCurve
import com.donnclab.kinetiframe.core.actor.motion.RotationCurvePoint
import com.donnclab.kinetiframe.core.actor.presets.RootMotionChannel
import com.donnclab.kinetiframe.core.actor.presets.RotationPoint
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import com.donnclab.kinetiframe.core.actor.presets.RootMotionPoint as PresetRootPoint

class ActorStudioCompositionTest {
    @Test fun `studio recordings override preset and key values only from their take start`() {
        val state = state()
        val definition = requireNotNull(state.actorDefinition())
        val rest = definition.sceneGraph
        val root = rest.rootBoneIds.first()
        val original = requireNotNull(state.activeActor())
        val preset = preset(root)
        val instance = original.copy(
            pose = mapOf(root to Transform(rotationDegrees = 65f)),
            motionPreset = preset,
            poseKeys = listOf(ActorPoseKey(0, mapOf(root to 20f), mapOf(root to RootTranslation(3f, 4f)))),
            rotationCurves = listOf(RotationCurve(root, points = listOf(RotationCurvePoint(1_000_000, 0f)))),
            rootMotionCurves = listOf(RootMotionCurve(root, listOf(RootMotionPoint(1_000_000, 0f, 0f)))),
        )
        val source = state.copy(
            project = state.project.copy(
                puppetInstances =
                state.project.puppetInstances + (instance.id to instance),
            ),
        )
        val before = source.copy(
            actor = source.actor.copy(previewTimeMicros = 500_000),
        ).actorDefinition(true)!!.sceneGraph
        assertEquals(
            rest.bones.getValue(root).localTransform.rotationDegrees + preset.offsets(500_000).getValue(root),
            before.bones.getValue(root).localTransform.rotationDegrees,
        )
        val expectedRoot = preset.rootTranslations(500_000, rest).getValue(root)
        assertEquals(
            rest.bones.getValue(root).localTransform.x + expectedRoot.x,
            before.bones.getValue(root).localTransform.x,
        )
        val after = source.copy(
            actor = source.actor.copy(previewTimeMicros = 1_000_000),
        ).actorDefinition(true)!!.sceneGraph
        assertEquals(0f, after.bones.getValue(root).localTransform.rotationDegrees)
        assertEquals(
            rest.bones.getValue(root).localTransform.translation,
            after.bones.getValue(root).localTransform.translation,
        )
    }

    @Test fun `preset audition bypasses authored channels without publishing changes`() {
        val state = state()
        val rest = requireNotNull(state.actorDefinition()).sceneGraph
        val root = rest.rootBoneIds.first()
        val original = requireNotNull(state.activeActor())
        val instance = original.copy(
            poseKeys = listOf(ActorPoseKey(0, mapOf(root to 90f))),
            rotationCurves = listOf(RotationCurve(root, points = listOf(RotationCurvePoint(0, 80f)))),
            rootMotionCurves = listOf(RootMotionCurve(root, listOf(RootMotionPoint(0, 25f, 25f)))),
        )
        val preview = preset(root)
        val source = state.copy(
            project = state.project.copy(
                puppetInstances =
                state.project.puppetInstances + (instance.id to instance),
            ),
            actor = state.actor.copy(
                presetPreviewInstanceId = instance.id,
                presetPreview = preview,
                previewTimeMicros = 500_000,
            ),
        )
        val evaluated = requireNotNull(source.actorDefinition(true)).sceneGraph
        assertEquals(
            rest.bones.getValue(root).localTransform.rotationDegrees + preview.offsets(500_000).getValue(root),
            evaluated.bones.getValue(root).localTransform.rotationDegrees,
        )
        assertEquals(
            rest.bones.getValue(root).localTransform.x + preview.rootTranslations(500_000, rest).getValue(root).x,
            evaluated.bones.getValue(root).localTransform.x,
        )
        assertEquals(instance, source.activeActor())
    }

    private fun state(): StudioState {
        var state = StudioState(scene = SceneState(studioMode = StudioMode.RIG_SETUP))
        ActorStudioManager({ state }, { state = it(state) }).handle(StudioIntent.Actor.Create(ActorTemplate.HUMAN))
        return state.copy(scene = state.scene.copy(studioMode = StudioMode.ANIMATE_IK))
    }

    private fun preset(root: String): AppliedActorPreset = AppliedActorPreset(
        ActorMotionPreset(
            schemaVersion = 3,
            id = "studio",
            name = "Studio",
            description = "",
            durationSeconds = 2f,
            requiredBones = listOf(root),
            channels = listOf(RotationChannel(root, points = listOf(RotationPoint(0f, 10f), RotationPoint(1f, 10f)))),
            rootMotion = listOf(
                RootMotionChannel(
                    root,
                    listOf(PresetRootPoint(0f), PresetRootPoint(.5f, .1f, -.1f), PresetRootPoint(1f)),
                ),
            ),
        ),
    )
}
