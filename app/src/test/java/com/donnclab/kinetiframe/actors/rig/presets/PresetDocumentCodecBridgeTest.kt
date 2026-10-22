package com.donnclab.kinetiframe.actors.rig.presets

import com.donnclab.kinetiframe.actors.rig.model.ActorView
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeDocument
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalSerializationApi::class)
class PresetDocumentCodecBridgeTest {
    @Test fun `public recipe codec matches generated product codec across schemas defaults and views`() {
        val formats = listOf(Json, Json { encodeDefaults = true })
        val prototype = ActorMotionPreset(
            id = "test",
            name = "Test",
            description = "",
            durationSeconds = 1f,
            requiredBones = listOf("head"),
            channels = listOf(RotationChannel("head")),
        )
        for (schema in 1..3) {
            for (view in ActorView.entries) {
                for (loop in listOf(false, true)) {
                    val recipe = prototype.copy(
                        schemaVersion = schema,
                        authoredView = view,
                        loop = loop,
                        previewAssetPath = "private-preview.png",
                        rootMotion = listOf(RootMotionChannel("root", listOf(RootMotionPoint(0f)))),
                    )
                    formats.forEach { json ->
                        val wire = json.encodeToString(recipe)
                        assertEquals(wire, json.encodeToString(recipe.document()))
                        assertEquals(wire, json.encodeToString(json.decodeFromString<PresetRecipeDocument>(wire)))
                        assertEquals(
                            recipe.copy(previewAssetPath = null),
                            json.decodeFromString<ActorMotionPreset>(wire),
                        )
                    }
                }
            }
        }
        val old = ActorMotionPreset.serializer().descriptor
        val shared = PresetRecipeDocument.serializer().descriptor
        assertEquals(old.serialName, shared.serialName)
        assertEquals(old.elementsCount, shared.elementsCount)
        repeat(old.elementsCount) { index ->
            assertEquals(old.getElementName(index), shared.getElementName(index))
            assertEquals(old.isElementOptional(index), shared.isElementOptional(index))
            assertEquals(old.getElementDescriptor(index).serialName, shared.getElementDescriptor(index).serialName)
        }
    }
}
