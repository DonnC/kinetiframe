// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.validation

import com.donnclab.kinetiframe.core.actor.artwork.ActorDrawing
import com.donnclab.kinetiframe.core.actor.artwork.ActorDrawingRegion
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkSkin
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkVariant
import com.donnclab.kinetiframe.core.actor.artwork.DrawingBinding
import com.donnclab.kinetiframe.core.actor.artwork.DrawingComposition
import com.donnclab.kinetiframe.core.actor.artwork.DrawingExtent
import com.donnclab.kinetiframe.core.actor.artwork.DrawingPlacement
import com.donnclab.kinetiframe.core.actor.artwork.DrawingPoint
import com.donnclab.kinetiframe.core.actor.artwork.DrawingSource
import com.donnclab.kinetiframe.core.actor.artwork.SkinAffine
import com.donnclab.kinetiframe.core.actor.artwork.SkinBone
import com.donnclab.kinetiframe.core.actor.artwork.SkinGrid
import com.donnclab.kinetiframe.core.actor.artwork.SkinMesh
import com.donnclab.kinetiframe.core.actor.artwork.SkinSourceSize
import com.donnclab.kinetiframe.core.actor.artwork.SkinVertex
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform
import com.donnclab.kinetiframe.core.geometry.Point2

internal object GraphValidationFixtures {
    val root = BoneNode("root", name = "Root", length = 10f)
    val child = BoneNode("child", "root", "Child", 5f)
    val graph = SceneGraph(listOf("root"), linkedMapOf("root" to root, "child" to child))
    private val alternative = ArtworkVariant("Open", TextureBinding("open", 0f, 0f))
    val singular = SkinAffine(Point2(0f, 0f), Point2(0f, 0f), Point2(0f, 0f))
    private val nonFinite = SkinAffine.Identity.copy(origin = Point2(Float.NaN, 0f))

    fun skin(ids: List<String> = listOf("root"), matrix: SkinAffine = SkinAffine.Identity): ArtworkSkin = ArtworkSkin(
        SkinSourceSize(10, 10),
        SkinGrid(1, 1),
        SkinMesh(
            ids.map { SkinBone(it, matrix) },
            List(4) { SkinVertex(0f, 0f, List(ids.size) { 1f / ids.size }) },
        ),
    )

    fun replace(bone: BoneNode): SceneGraph = graph.copy(bones = graph.bones + (bone.id to bone))

    fun cutout(mesh: ArtworkSkin = skin()): SceneGraph =
        replace(root.withArtwork(textureBinding = TextureBinding("body", 0f, 0f).copy(skin = mesh)))

    fun drawing(mesh: ArtworkSkin = skin()): ActorDrawing = ActorDrawing(
        DrawingSource("ink", "colour", SkinSourceSize(10, 10)),
        DrawingPlacement(Point2(0f, 0f), DrawingExtent(10f, 10f)),
        DrawingBinding(mesh),
    )

    fun tiled(count: Int): SceneGraph {
        val ids = (0 until count).map { "part-$it" }
        val bones = ids.associateWith { id ->
            val mesh = skin(listOf(id)).copy(
                grid = SkinGrid(32, 32),
                mesh = SkinMesh(
                    listOf(SkinBone(id, SkinAffine.Identity)),
                    List(1089) {
                        SkinVertex(0f, 0f, listOf(1f))
                    },
                ),
            )
            BoneNode(
                id,
                name = id,
                length = 1f,
            ).withArtwork(textureBinding = TextureBinding(id, 0f, 0f).copy(skin = mesh))
        }
        return SceneGraph(ids, bones)
    }

    fun invalidCases(): Map<GraphValidationIssue, SceneGraph> = linkedMapOf(
        GraphValidationIssue.PART_COUNT to SceneGraph(),
        GraphValidationIssue.ROOTS to graph.copy(rootBoneIds = listOf("root", "root")),
        GraphValidationIssue.PART_IDENTITY to
            graph.copy(bones = graph.bones + ("child" to child.withHierarchy(id = "different"))),
        GraphValidationIssue.VARIANT_COUNT to
            replace(root.withArtwork(artworkVariants = (0..24).associate { "v$it" to alternative })),
        GraphValidationIssue.VARIANT_IDENTITY to
            replace(root.withArtwork(artworkVariants = mapOf("bad key" to alternative))),
        GraphValidationIssue.ARTWORK_ADDRESS to
            replace(root.withArtwork(artworkVariants = mapOf("open" to alternative))).let {
                it.copy(bones = it.bones + ("root#open" to BoneNode("root#open", "root", "Collision", 1f)))
            },
        GraphValidationIssue.ARTWORK_ALIGNMENT to replace(
            root.withArtwork(textureBinding = TextureBinding("", 0f, 0f)),
        ),
        GraphValidationIssue.ATTACHMENT_PAIR to cutout(skin().copy(attachmentBlend = true)),
        GraphValidationIssue.SKIN_GRID to cutout(skin().copy(grid = SkinGrid(0, 1))),
        GraphValidationIssue.SKIN_BONES to cutout(skin(listOf("child"))),
        GraphValidationIssue.SKIN_BIND_MATRIX to cutout(skin(matrix = nonFinite)),
        GraphValidationIssue.SINGULAR_BIND to cutout(skin(matrix = singular)),
        GraphValidationIssue.SKIN_WEIGHTS to cutout(
            skin().let {
                it.copy(mesh = it.mesh.copy(vertices = List(4) { SkinVertex(0f, 0f, listOf(0f)) }))
            },
        ),
        GraphValidationIssue.PART_LENGTH to replace(root.withRest(length = 0f)),
        GraphValidationIssue.PART_TRANSFORM to
            replace(root.withRest(localTransform = Transform().copy(stretch = Point2(0f, 1f)))),
        GraphValidationIssue.UNKNOWN_PARENT to replace(child.withHierarchy(parentId = "missing")),
        GraphValidationIssue.IK_ROOT to replace(child.withRest(ikChainRootId = "missing")),
        GraphValidationIssue.HIERARCHY_CYCLE to replace(child.withHierarchy(parentId = "child")),
        GraphValidationIssue.GRID_BUDGET to tiled(16),
        GraphValidationIssue.DRAWING_SOURCES to
            graph.copy(drawing = drawing().let { it.copy(source = it.source.copy(colourAssetId = "ink")) }),
        GraphValidationIssue.DRAWING_ADDRESS to graph.copy(
            drawing = drawing(),
            bones = graph.bones +
                (ActorDrawing.INK_ADDRESS to BoneNode(ActorDrawing.INK_ADDRESS, "root", "Collision", 1f)),
        ),
        GraphValidationIssue.DRAWING_PLACEMENT to graph.copy(
            drawing = drawing().let {
                it.copy(placement = it.placement.copy(extent = DrawingExtent(0f, 10f)))
            },
        ),
        GraphValidationIssue.DRAWING_OVERLAYS to
            graph.copy(drawing = drawing().copy(composition = DrawingComposition(overlayParts = setOf("missing")))),
        GraphValidationIssue.DRAWING_UNDERLAYS to graph.copy(
            drawing = drawing().copy(
                composition = DrawingComposition(overlayParts = setOf("root"), underlayParts = setOf("root")),
            ),
        ),
        GraphValidationIssue.DRAWING_CORRECTIONS to graph.copy(
            drawing = drawing().let {
                it.copy(
                    binding = it.binding.copy(
                        regions = listOf(
                            ActorDrawingRegion(
                                "missing",
                                listOf(DrawingPoint(0f, 0f), DrawingPoint(1f, 0f), DrawingPoint(1f, 1f)),
                            ),
                        ),
                    ),
                )
            },
        ),
        GraphValidationIssue.DRAWING_GRID to graph.copy(
            drawing = drawing(skin().copy(source = SkinSourceSize(11, 10))),
        ),
        GraphValidationIssue.DRAWING_BONES to graph.copy(drawing = drawing(skin(listOf("missing")))),
        GraphValidationIssue.DRAWING_BIND_MATRIX to graph.copy(drawing = drawing(skin(matrix = nonFinite))),
        GraphValidationIssue.DRAWING_WEIGHTS to graph.copy(
            drawing = drawing(
                skin().let {
                    it.copy(mesh = it.mesh.copy(vertices = List(4) { SkinVertex(0f, 0f, listOf(0f)) }))
                },
            ),
        ),
        GraphValidationIssue.SURFACE_DESCENDANTS to replace(
            child.withArtwork(
                textureBinding = TextureBinding("child", 0f, 0f).copy(skin = skin(listOf("root", "child"))),
            ),
        ),
        GraphValidationIssue.SURFACE_OVERLAP to cutout(skin(listOf("root", "child"))).let {
            it.copy(
                bones = it.bones + (
                    "child" to child.withArtwork(
                        textureBinding = TextureBinding("child", 0f, 0f).copy(skin = skin(listOf("child"))),
                    )
                    ),
            )
        },
    )
}
