package com.donnclab.kinetiframe.actors.data

import com.donnclab.kinetiframe.actors.rig.math.RigGeometry
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplates
import com.donnclab.kinetiframe.actors.rig.model.ActorView
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.actors.rig.presets.AppliedActorPreset
import com.donnclab.kinetiframe.actors.ui.rigging.PresetVectorIcon
import com.donnclab.kinetiframe.core.document.StageDimensions
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ViewPresetCollectionTest {
    private val root =
        File("src/main/assets/actor/presets/views").takeIf(File::isDirectory)
            ?: File("app/src/main/assets/actor/presets/views")

    @Test fun `thirteen distinct motions cover four authored views with valid vector icons`() {
        val files = root.listFiles()!!.filter { it.extension == "json" }
        val recipes = files.map { Json.decodeFromString<ActorMotionPreset>(it.readText()).validate() }
        assertEquals(13, recipes.size)
        assertEquals(13, recipes.map { it.id }.distinct().size)
        assertEquals(
            setOf(ActorView.FRONT, ActorView.PROFILE, ActorView.THREE_QUARTER, ActorView.BACK),
            recipes.map {
                it.authoredView
            }.toSet(),
        )
        assertEquals(13, files.map { File(root, "${it.nameWithoutExtension}.svg").readText() }.distinct().size)
        files.forEach {
            assertEquals(
                64f,
                PresetVectorIcon.decode(File(root, "${it.nameWithoutExtension}.svg").readText()).viewportWidth,
            )
        }
        recipes.forEach { recipe ->
            val rest = ActorTemplates.create(ActorTemplate.HUMAN, StageDimensions(1000, 800), recipe.authoredView)
            assertTrue(recipe.missingBones(rest.bones.keys).isEmpty())
            ActorView.entries.filter {
                it != ActorView.ORIGINAL && it != recipe.authoredView
            }.forEach { assertFalse(recipe.supportsView(it)) }
            val applied = AppliedActorPreset(recipe)
            val baseline = RigGeometry.resolve(rest)
            val samples = (0..100).map { applied.offsets(applied.durationMicros * it / 100) }
            assertTrue(samples.distinct().size > 30)
            samples.forEach { offsets ->
                assertTrue(offsets.values.all(Float::isFinite))
                val posed = rest.copy(
                    bones = rest.bones.mapValues { (id, bone) ->
                        bone.withRest(
                            localTransform = bone.localTransform.copy(
                                rotationDegrees =
                                bone.localTransform.rotationDegrees + (offsets[id] ?: 0f),
                            ),
                        )
                    },
                )
                val joints = RigGeometry.resolve(posed)
                if (recipe.id !in setOf("profile-walk", "profile-run", "profile-jump")) {
                    listOf("left_foot", "right_foot").forEach {
                        assertEquals(baseline.getValue(it), joints.getValue(it), recipe.id)
                    }
                }
            }
            if (!recipe.loop) {
                assertEquals(applied.offsets(applied.durationMicros), applied.offsets(applied.durationMicros * 3))
            } else {
                assertEquals(applied.offsets(0), applied.offsets(applied.durationMicros))
            }
        }
    }
}
