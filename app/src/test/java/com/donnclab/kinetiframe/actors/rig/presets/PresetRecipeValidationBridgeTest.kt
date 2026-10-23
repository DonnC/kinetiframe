package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.core.actor.presets.RootMotionChannel
import com.donnclab.kinetiframe.core.actor.presets.RootMotionPoint
import com.donnclab.kinetiframe.core.actor.presets.RotationPoint
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.File

class PresetRecipeValidationBridgeTest {
    private val root = RootMotionChannel("root", listOf(RootMotionPoint(0f), RootMotionPoint(1f)))
    private val rotation = RotationChannel("root", points = listOf(RotationPoint(0f, 0f), RotationPoint(1f, 0f)))
    private val prototype =
        ActorMotionPreset(3, "test", "Test", "", 1f, listOf("root"), listOf(rotation), rootMotion = listOf(root))

    @Test fun `metadata root and channel failures match former acceptance precedence and messages`() {
        val candidates =
            listOf(
                prototype, prototype.copy(schemaVersion = 0, id = ""), prototype.copy(schemaVersion = 4),
                prototype.copy(id = "BAD"), prototype.copy(name = " "), prototype.copy(durationSeconds = Float.NaN),
                prototype.copy(durationSeconds = .249f), prototype.copy(durationSeconds = 30.1f),
                prototype.copy(channels = emptyList()), prototype.copy(channels = listOf(rotation, rotation)),
                prototype.copy(requiredBones = emptyList()), prototype.copy(requiredBones = listOf("root", "root")),
                prototype.copy(
                    rootMotion = listOf(root, root),
                ),
                prototype.copy(rootMotion = listOf(root.copy(boneId = "missing"))),
                prototype.copy(rootMotion = listOf(root.copy(points = emptyList()))),
                prototype.copy(
                    rootMotion = listOf(root.copy(points = listOf(RootMotionPoint(.1f), RootMotionPoint(1f)))),
                ),
                prototype.copy(
                    rootMotion = listOf(
                        root.copy(points = listOf(RootMotionPoint(0f, Float.NaN), RootMotionPoint(1f))),
                    ),
                ),
                prototype.copy(
                    rootMotion = listOf(
                        root.copy(
                            points = listOf(RootMotionPoint(0f), RootMotionPoint(.0000009f), RootMotionPoint(1f)),
                        ),
                    ),
                ),
                prototype.copy(
                    rootMotion = listOf(root.copy(points = listOf(RootMotionPoint(0f), RootMotionPoint(1f, 1f)))),
                ),
                prototype.copy(channels = listOf(rotation.copy(boneId = ""))),
                prototype.copy(
                    channels = listOf(rotation.copy(boneId = "")),
                    rootMotion = listOf(root.copy(points = emptyList())),
                ),
                prototype.copy(requiredBones = listOf(""), rootMotion = emptyList()),
            )
        candidates.forEach { candidate ->
            listOf(false, true).forEach { loop ->
                compare(candidate.copy(loop = loop))
                compare(candidate.copy(loop = loop, schemaVersion = 1))
                compare(candidate.copy(loop = loop, schemaVersion = 2))
            }
        }
    }

    @Test fun `all shipped recipe snapshots retain identity wire and validation result`() {
        val defaults = Json { encodeDefaults = true }
        val directory =
            File("src/main/assets/actor/presets").takeIf { it.isDirectory } ?: File("app/src/main/assets/actor/presets")
        directory.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
            val recipe = Json.decodeFromString<ActorMotionPreset>(file.readText())
            val encoded = defaults.encodeToString(recipe)
            assertSame(recipe, recipe.validate())
            compare(recipe)
            assertEquals(encoded, defaults.encodeToString(recipe))
            assertEquals(recipe, defaults.decodeFromString<ActorMotionPreset>(encoded).validate())
        }
    }

    @Test fun `metadata and count failures never traverse rejected channel input`() {
        val oversized = object : AbstractList<RotationChannel>() {
            override val size: Int get() = 129
            override fun get(index: Int): RotationChannel = throw AssertionError("Rejected input must not be adapted")
        }
        val count =
            assertThrows(IllegalArgumentException::class.java) { prototype.copy(channels = oversized).validate() }
        assertEquals("Failed requirement.", count.message)
        val schema =
            assertThrows(IllegalArgumentException::class.java) {
                prototype.copy(schemaVersion = 4, channels = oversized).validate()
            }
        assertEquals("Unsupported motion preset schema: 4", schema.message)
    }

    private fun compare(recipe: ActorMotionPreset) {
        val expected = runCatching { LegacyPresetRecipeValidationReference.validate(recipe) }.exceptionOrNull()
        val actual = runCatching { recipe.validate() }.exceptionOrNull()
        assertEquals(expected?.javaClass, actual?.javaClass)
        assertEquals(expected?.message, actual?.message)
    }
}
