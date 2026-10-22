package com.donnclab.kinetiframe.engine.rigging.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorManifest
import com.donnclab.kinetiframe.data.actor.ActorRevision
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.PerformanceDocument
import com.donnclab.kinetiframe.data.actor.PerformancePlayback
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.data.actor.validate
import com.donnclab.kinetiframe.domain.actor.PerformanceAuthoring
import com.donnclab.kinetiframe.domain.actor.PerformanceCompatibility
import com.donnclab.kinetiframe.domain.actor.PerformanceEvaluator
import com.donnclab.kinetiframe.engine.model.StageDimensions
import com.donnclab.kinetiframe.engine.rigging.model.ActorPoseKey
import com.donnclab.kinetiframe.engine.rigging.model.ActorPoseTrack
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplates
import com.donnclab.kinetiframe.engine.rigging.model.JointContact
import com.donnclab.kinetiframe.engine.rigging.model.RootTranslation
import com.donnclab.kinetiframe.ui.actorDefinition
import com.donnclab.kinetiframe.ui.actors.actorStudioInitialState
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.math.abs

class NaturalActorMotionTest {
    @TempDir lateinit var directory: File
    private val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions.LandscapeHd)
    private val manifest = ActorManifest(
        actorId = "actor-test",
        displayName = "Test",
        templateId = "human-v1",
        currentRevisionId = "revision-test",
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    )
    private val revision =
        ActorRevision(revisionId = "revision-test", actorId = "actor-test", sceneGraph = rest, createdAtEpochMs = 1)
    private fun document(keys: List<ActorPoseKey>) = PerformanceDocument(
        performanceId = "performance-test", projectId = "project-test",
        actorId = "actor-test", actorRevisionId = "revision-test", displayName = "Motion", durationMicros = 1_000_000,
        playback = PerformancePlayback.END_HOLD, poseKeys = keys,
        rotationCurves = PerformanceAuthoring.rotationCurves(
            keys,
            1_000_000,
        ),
        createdAtEpochMs = 1, updatedAtEpochMs = 1,
    )

    @Test fun `smooth curve hits keys without overshoot or velocity jumps and preserves holds`() {
        val curve = CompiledMotionCurve(listOf(0, 200_000, 700_000, 1_000_000), listOf(0f, 30f, 60f, 60f))
        assertEquals(30f, curve.sample(200_000))
        for (t in 0L..1_000_000L step 1000) assertTrue(curve.sample(t) in 0f..60f)
        assertEquals(60f, curve.sample(800_000))
        val before = curve.sample(200_000) - curve.sample(199_900)
        val after = curve.sample(200_100) - curve.sample(200_000)
        assertEquals(before, after, .00005f)
        assertTrue(curve.sample(100) < .0001f)
        assertNotEquals(15f, curve.sample(100_000))
    }

    @Test fun `angular seam and matching periodic loop have continuous velocity`() {
        val angular = CompiledMotionCurve(listOf(0, 1_000_000), listOf(170f, -170f), angular = true)
        assertEquals(180f, angular.sample(500_000), .001f)
        assertEquals(-170f, angular.sample(1_000_000))
        val cycle = CompiledMotionCurve(listOf(0, 250_000, 750_000, 1_000_000), listOf(0f, 20f, -20f, 0f), loop = true)
        assertEquals(cycle.sample(100) - cycle.sample(0), cycle.sample(1_000_000) - cycle.sample(999_900), .00005f)
    }

    @Test fun `linear and stepped authored curves keep their exact behavior`() {
        val curve = CompiledMotionCurve(listOf(0, 1_000_000), listOf(0f, 40f))
        assertEquals(10f, curve.sample(250_000, CompiledMotionCurve.Interpolation.LINEAR))
        assertEquals(0f, curve.sample(999_999, CompiledMotionCurve.Interpolation.STEP))
        assertEquals(40f, curve.sample(1_000_000, CompiledMotionCurve.Interpolation.STEP))
    }

    @Test fun `two bone chain keeps side and lengths across extension root and unreachable targets`() {
        val chain = listOf(Offset.Zero, Offset(10f, 0f), Offset(20f, 0f))
        for (side in listOf(-1, 1)) {
            for (target in listOf(
                Offset.Zero,
                Offset(.00001f, 0f),
                Offset(19.999f, 0f),
                Offset(200f, 30f),
                Offset(-3f, 6f),
            )) {
                val result = TwoBoneSolver.solve(chain, target, side)
                assertEquals(10f, (result[1] - result[0]).getDistance(), .001f)
                assertEquals(10f, (result[2] - result[1]).getDistance(), .001f)
                assertTrue(result.all { it.x.isFinite() && it.y.isFinite() })
                val axis = result[2] - result[0]
                assertTrue((axis.x * result[1].y - axis.y * result[1].x) * side >= -.001f)
            }
        }
    }

    @Test fun `root weight shift keeps planted foot fixed with random seeking studio export and restart parity`() {
        val anchor = ActorMotionPose.captureContact(rest, "left_shin", "left_foot")
        val contact = mapOf("left_shin" to anchor)
        val shifted = ActorPoseEditor.moveRoot(
            rest,
            "torso",
            Offset(
                rest.bones.getValue("torso").localTransform.x + 12f,
                rest.bones.getValue("torso").localTransform.y + 6f,
            ),
        )
        val keys =
            listOf(
                ActorPoseTrack.capture(0, rest, rest, contact),
                ActorPoseTrack.capture(1_000_000, shifted, rest, contact),
            )
        val document = document(keys).validate()
        PerformanceCompatibility.validate(document, revision)
        val files = ActorFileStore(directory)
        assertTrue(PerformanceRepository(files).save(document) is ActorStoreResult.Value)
        val loaded = (
            PerformanceRepository(
                ActorFileStore(directory),
            ).load("project-test", "performance-test") as ActorStoreResult.Value
            ).value
        assertEquals(document, loaded)
        val studio = actorStudioInitialState(manifest, revision, loaded)
        for (time in listOf(800_000L, 0L, 500_000L, 1_000_000L, 200_000L, 500_000L)) {
            val posed = PerformanceEvaluator.pose(loaded, time, rest)
            val world = RigGeometry.resolve(posed)
            assertEquals(0f, (world.getValue("left_shin").end - Offset(anchor.x, anchor.y)).getDistance(), .02f)
            assertEquals(anchor.followWorldAngle, world.getValue("left_foot").angle, .001f)
            assertEquals(rest, revision.sceneGraph)
            val preview = studio.copy(
                actor = studio.actor.copy(previewTimeMicros = time),
            ).actorDefinition(true)!!.sceneGraph
            assertEquals(posed, preview, "Studio and saved rendering must use identical math")
            assertEquals(posed, PerformanceEvaluator.pose(loaded, time, rest))
        }
        assertEquals(12f, loaded.poseKeys.last().rootTranslations.getValue("torso").x)
    }

    @Test fun `release key stops contact and legacy keys decode without new data`() {
        val anchor = ActorMotionPose.captureContact(rest, "left_shin", "left_foot")
        val keys =
            listOf(
                ActorPoseTrack.capture(0, rest, rest, mapOf("left_shin" to anchor)),
                ActorPoseTrack.capture(500_000, rest, rest),
            )
        assertEquals(1, ActorPoseTrack.contacts(keys, 499_999).size)
        assertTrue(ActorPoseTrack.contacts(keys, 500_000).isEmpty())
        val legacy = Json.decodeFromString<ActorPoseKey>("""{"timeMicros":0,"rotations":{"head":20}}""")
        assertTrue(legacy.rootTranslations.isEmpty() && legacy.contacts.isEmpty())
        assertEquals(keys, Json.decodeFromString<List<ActorPoseKey>>(Json.encodeToString(keys)))
    }

    @Test fun `invalid motion metadata rejects nonfinite values and nonroot or invalid chain IDs`() {
        assertThrows(IllegalArgumentException::class.java) {
            document(listOf(ActorPoseKey(0, emptyMap(), mapOf("torso" to RootTranslation(Float.NaN, 0f))))).validate()
        }
        assertThrows(IllegalArgumentException::class.java) {
            PerformanceCompatibility.validate(
                document(listOf(ActorPoseKey(0, emptyMap(), mapOf("head" to RootTranslation())))),
                revision,
            )
        }
        assertThrows(IllegalArgumentException::class.java) {
            PerformanceCompatibility.validate(
                document(listOf(ActorPoseKey(0, emptyMap(), contacts = mapOf("head" to JointContact(0f, 0f, 1))))),
                revision,
            )
        }
    }

    @Test fun `new walk run and jump root motion is scale aware deterministic and preserves old snapshots`() {
        val assets =
            File("src/main/assets/actor/presets/views").takeIf(File::isDirectory)
                ?: File("app/src/main/assets/actor/presets/views")
        for (name in listOf("profile-walk", "profile-run", "profile-jump")) {
            val recipe = Json.decodeFromString<com.donnclab.kinetiframe.engine.rigging.presets.ActorMotionPreset>(
                File(assets, "$name.json").readText(),
            ).validate()
            val applied = com.donnclab.kinetiframe.engine.rigging.presets.AppliedActorPreset(recipe)
            val sampled = (0..100).map {
                applied.rootTranslations(it * applied.durationMicros / 100, rest).getValue("torso").y
            }
            assertTrue(sampled.max() - sampled.min() > 5f, name)
            val half = rest.copy(bones = rest.bones.mapValues { (_, bone) -> bone.withRest(length = bone.length / 2) })
            assertEquals(
                applied.rootTranslations(applied.durationMicros / 3, rest).getValue("torso").y / 2,
                applied.rootTranslations(applied.durationMicros / 3, half).getValue("torso").y,
                .001f,
            )
            assertEquals(
                applied.rootTranslations(200_000, rest),
                applied.copy(speed = 2f).rootTranslations(100_000, rest),
            )
            if (recipe.loop) {
                assertEquals(
                    applied.rootTranslations(123_456, rest),
                    applied.rootTranslations(applied.durationMicros + 123_456, rest),
                )
            } else {
                assertEquals(
                    applied.rootTranslations(applied.durationMicros, rest),
                    applied.rootTranslations(applied.durationMicros * 10, rest),
                )
                assertTrue(sampled.min() < -rest.bones.getValue("torso").length * .5)
            }
            val legacy = applied.copy(recipe = recipe.copy(schemaVersion = 2, rootMotion = emptyList()).validate())
            assertTrue(legacy.rootTranslations(200_000, rest).isEmpty())
            assertEquals(legacy.offsets(200_000), applied.offsets(200_000))
        }
    }

    @Test fun `root drag pin release and undo persist motion without changing the rest graph`() {
        var state = actorStudioInitialState(manifest, revision, performanceAuthoring = true)
        val manager = com.donnclab.kinetiframe.domain.manager.ActorStudioManager({ state }, { state = it(state) })
        val motion = com.donnclab.kinetiframe.domain.manager.ActorMotionManager(
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined),
            { state },
            { state = it(state) },
            manager::checkpoint,
        )
        state = state.copy(scene = state.scene.copy(selectedBoneId = "left_foot"))
        motion.handle(com.donnclab.kinetiframe.ui.StudioIntent.ActorMotion.ToggleContact)
        assertEquals(1, state.project.puppetInstances.values.single().poseKeys.first().contacts.size)
        val origin = rest.bones.getValue("torso").localTransform
        manager.handle(
            com.donnclab.kinetiframe.ui.StudioIntent.Actor.Drag(
                "origin:torso",
                Offset(
                    origin.x + 12f,
                    origin.y + 6f,
                ),
            ),
        )
        manager.handle(com.donnclab.kinetiframe.ui.StudioIntent.Actor.CommitDrag)
        assertEquals(rest, state.actorDefinition()!!.sceneGraph)
        assertEquals(
            12f,
            state.project.puppetInstances.values.single().poseKeys.first().rootTranslations.getValue("torso").x,
        )
        motion.handle(com.donnclab.kinetiframe.ui.StudioIntent.ActorMotion.ToggleContact)
        assertTrue(state.project.puppetInstances.values.single().poseKeys.first().contacts.isEmpty())
        manager.handle(com.donnclab.kinetiframe.ui.StudioIntent.Actor.Undo)
        assertEquals(1, state.project.puppetInstances.values.single().poseKeys.first().contacts.size)
        manager.handle(com.donnclab.kinetiframe.ui.StudioIntent.Actor.Undo)
        assertEquals(
            0f,
            state.project.puppetInstances.values.single().poseKeys.first().rootTranslations.getValue("torso").x,
        )
    }
}
