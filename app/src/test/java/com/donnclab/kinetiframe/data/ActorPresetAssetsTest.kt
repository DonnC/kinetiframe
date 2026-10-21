package com.donnclab.kinetiframe.data

import com.donnclab.kinetiframe.engine.rigging.presets.ActorMotionPreset
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class ActorPresetAssetsTest {
    @Test fun `renamed filename determines preview rather than recipe id`() {
        assertEquals(
            "actor/presets/human/wave-hand.png",
            ActorPresetAssets.previewPath(
                "actor/presets/human/wave-hand.json",
                listOf("wave-hand.png", "human-wave.png"),
            ),
        )
    }

    @Test fun `missing matching preview fails with file diagnostic`() {
        val error = assertThrows(IllegalArgumentException::class.java) {
            ActorPresetAssets.previewPath("human/nod.json", listOf("wave.png"))
        }
        assertTrue(error.message.orEmpty().contains("human/nod.json"))
    }

    @Test fun `jpg pairing is supported`() {
        assertEquals("human/nod.jpg", ActorPresetAssets.previewPath("human/nod.json", listOf("nod.jpg")))
    }

    @Test fun `shipped human recipes have paired previews and presentation paths stay out of math`() {
        val root = listOf(File("src/main/assets"), File("app/src/main/assets")).first { it.isDirectory }
        val recipes = File(root, "actor/presets/views").walkTopDown().filter { it.extension == "json" }.toList()
        assertTrue(recipes.isNotEmpty())
        recipes.forEach { file ->
            val path = file.relativeTo(root).invariantSeparatorsPath
            val preview = ActorPresetAssets.previewPath(path, requireNotNull(file.parentFile).list().orEmpty().toList())
            assertTrue(File(root, preview).isFile)
            val recipe = Json.decodeFromString<ActorMotionPreset>(file.readText()).validate()
            val encoded = Json.encodeToString(recipe.copy(previewAssetPath = preview))
            assertFalse(encoded.contains("previewAssetPath"))
            assertEquals(recipe, Json.decodeFromString<ActorMotionPreset>(encoded))
        }
        assertFalse(File(root, "actor/human-template.jpg").exists())
    }
}
