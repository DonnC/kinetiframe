package com.donnclab.kinetiframe.actors.rig.math

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkAlignment
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneHierarchy
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.BoneRest
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.actor.validation.ActorGraphValidation
import com.donnclab.kinetiframe.core.geometry.Point2
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ActorArtworkEditingTest {
    private fun graph() = SceneGraph(
        listOf("upper"),
        mapOf(
            "upper" to BoneNode(
                hierarchy = BoneHierarchy("upper", "Upper", null),
                rest = BoneRest(100f, localTransform = Transform(40f, 80f)),
            ),
            "lower" to BoneNode(
                hierarchy = BoneHierarchy("lower", "Lower", "upper"),
                rest = BoneRest(100f, localTransform = Transform(x = 100f)),
            ),
        ),
    )

    @Test fun `moving shared joint fits parent without detaching child`() {
        val moved = ActorPoseEditor.moveOrigin(graph(), "lower", Offset(80f, 160f))
        val world = RigGeometry.resolve(moved)
        assertEquals(world.getValue("upper").end, world.getValue("lower").start)
        assertEquals(80f, world.getValue("lower").start.x, .001f)
        assertEquals(160f, world.getValue("lower").start.y, .001f)
    }

    @Test fun `artwork translation leaves skeleton intact in rotated coordinates`() {
        val original = graph().let {
            it.copy(
                bones =
                it.bones +
                    (
                        "upper" to
                            it.bones.getValue(
                                "upper",
                            ).withRest(
                                localTransform = Transform(40f, 80f, rotationDegrees = 90f),
                            ).withArtwork(textureBinding = TextureBinding("eyes", 0f, 0f))
                        ),
            )
        }
        val moved = ActorPoseEditor.moveArtwork(original, "upper", Offset(0f, 10f))
        assertEquals(original.bones.getValue("upper").localTransform, moved.bones.getValue("upper").localTransform)
        assertEquals(10f, moved.bones.getValue("upper").textureBinding!!.offsetX, .001f)
        assertEquals(0f, moved.bones.getValue("upper").textureBinding!!.offsetY, .001f)
    }

    @Test fun `rebinding edited skeleton preserves the visible continuous surface`() {
        val original = graph()
        val binding = TextureBinding("arm", 0f, 20f)
        val skin = ArtworkSkinBuilder.bind(original, "upper", "lower", binding, 200, 40)
        val attached = original.copy(
            bones =
            original.bones +
                ("upper" to original.bones.getValue("upper").withArtwork(textureBinding = binding.copy(skin = skin))),
        )
        val edited = ActorPoseEditor.fit(attached, "lower", Offset(180f, 160f))
        val before = ArtworkSkinning.deform(skin, ArtworkSkinning.worldTransforms(edited))
        val rebound = ArtworkSkinRestBinding.rebase(edited)
        val after = ArtworkSkinning.deform(
            rebound.bones.getValue("upper").textureBinding!!.skin!!,
            ArtworkSkinning.worldTransforms(rebound),
        )
        assertArrayEquals(before, after, .001f)
        ActorGraphValidation.validate(rebound)
    }

    @Test fun `invalid target cannot corrupt skeleton or artwork`() {
        assertEquals(graph(), ActorPoseEditor.moveOrigin(graph(), "lower", Offset(Float.NaN, 0f)))
        assertEquals(graph(), ActorPoseEditor.moveArtwork(graph(), "upper", Offset(0f, Float.POSITIVE_INFINITY)))
    }

    @Test fun `expression alternatives follow artwork translation without changing their alignment`() {
        val binding =
            TextureBinding(
                "neutral",
                Point2(0f, 0f),
                ArtworkAlignment(Point2(3f, 5f)),
            )
        val original = graph().let {
            it.copy(
                bones =
                it.bones +
                    (
                        "upper" to
                            it.bones.getValue("upper").withArtwork(
                                textureBinding = binding,
                                artworkVariants = mapOf(
                                    "blink" to
                                        ArtworkVariant(
                                            "Blink",
                                            binding.copy(
                                                assetId = "blink",
                                                alignment = binding.alignment.copy(
                                                    offset = binding.alignment.offset.copy(y = 7f),
                                                ),
                                            ),
                                        ),
                                ),
                            )
                        ),
            )
        }
        val moved = ActorPoseEditor.moveArtwork(original, "upper", Offset(0f, 10f)).bones.getValue("upper")
        assertEquals(15f, moved.textureBinding!!.offsetY)
        assertEquals(17f, moved.artworkVariants.getValue("blink").binding.offsetY)
        assertEquals("blink", moved.artworkVariants.getValue("blink").binding.assetId)
    }
}
