package com.donnclab.kinetiframe.engine.rigging.render

import android.graphics.Matrix
import com.donnclab.kinetiframe.engine.model.PuppetDefinition
import com.donnclab.kinetiframe.engine.model.PuppetInstance
import com.donnclab.kinetiframe.engine.rigging.math.ArtworkSkinning
import com.donnclab.kinetiframe.engine.rigging.math.ForwardKinematicsSolver

/**
 * A bitmap-independent render instruction for rigid cutouts and continuous multi-bone skins.
 *
 * The plan contains asset IDs rather than Bitmaps, so the persisted puppet graph stays
 * independent of the runtime bitmap cache and every renderer gets the same input.
 */
data class PuppetRenderCommand(
    val assetId: String,
    val matrix: Matrix,
    val mesh: PuppetSkinMesh? = null,
    val boneId: String? = null,
)

data class PuppetSkinMesh(val width: Int, val height: Int, val columns: Int, val rows: Int, val vertices: FloatArray)

object PuppetRenderPlan {
    fun create(definition: PuppetDefinition, instance: PuppetInstance): List<PuppetRenderCommand> {
        val instanceMatrix = instance.transform.toMatrix()
        definition.sceneGraph.drawing?.takeIf { it.enabled }?.let { drawing ->
            val vertices = ArtworkSkinning.deform(drawing.skin, ArtworkSkinning.worldTransforms(definition.sceneGraph))
            val mesh = PuppetSkinMesh(drawing.width, drawing.height, drawing.skin.columns, drawing.skin.rows, vertices)
            val visibleSlots = drawing.overlayParts + drawing.underlayParts
            val overlays = if (visibleSlots.isEmpty()) {
                emptyList()
            } else {
                create(
                    definition.copy(
                        sceneGraph =
                        definition.sceneGraph.copy(
                            drawing = null,
                            bones = definition.sceneGraph.bones.mapValues { (id, bone) ->
                                bone.withDisplay(visible = bone.visible && id in visibleSlots)
                            },
                        ),
                    ),
                    instance,
                )
            }
            return overlays.filter { it.boneId in drawing.underlayParts } +
                listOf(
                    PuppetRenderCommand(drawing.colourAssetId, Matrix(instanceMatrix), mesh),
                    PuppetRenderCommand(drawing.inkAssetId, Matrix(instanceMatrix), mesh),
                ) +
                overlays.filter { it.boneId in drawing.overlayParts }
        }
        val world = if (definition.sceneGraph.bones.values.any { it.visible && it.textureBinding?.skin != null }) {
            ArtworkSkinning.worldTransforms(definition.sceneGraph)
        } else {
            emptyMap()
        }
        // A continuous image owns the paired limb's surface, not its skeleton or hand children.
        // Preserve covered cutout bindings in the document for reversible attachment/variants.
        val covered = mutableSetOf<String>()
        // worldTransforms is parent-first. A covered surface must not itself hide grandchildren
        // when a discrete variant selects another skin beneath the active continuous owner.
        world.keys.forEach { id ->
            val owner = definition.sceneGraph.bones.getValue(id)
            if (owner.visible &&
                id !in covered
            ) {
                owner.textureBinding?.skin?.takeUnless { it.attachmentBlend }?.bones.orEmpty()
                    .filter { it.boneId != id }.forEach { covered += it.boneId }
            }
        }
        return ForwardKinematicsSolver.resolveWorldStates(definition.sceneGraph)
            .entries.sortedBy { definition.sceneGraph.bones[it.key]?.drawOrder ?: 0 }
            .mapNotNull { (boneId, worldState) ->
                val bone = definition.sceneGraph.bones[boneId] ?: return@mapNotNull null
                if (!bone.visible || boneId in covered) return@mapNotNull null
                val binding = bone.textureBinding ?: return@mapNotNull null
                binding.skin?.let { skin ->
                    return@mapNotNull PuppetRenderCommand(
                        binding.assetId,
                        Matrix(instanceMatrix),
                        PuppetSkinMesh(
                            skin.sourceWidth,
                            skin.sourceHeight,
                            skin.columns,
                            skin.rows,
                            ArtworkSkinning.deform(skin, world),
                        ),
                        boneId,
                    )
                }
                PuppetRenderCommand(
                    assetId = binding.assetId,
                    boneId = boneId,
                    // The bone origin is the attachment pivot. Offset source pixels first, then
                    // place the resulting rigid texture through the bone and instance transforms.
                    matrix = Matrix(worldState.worldMatrix).apply {
                        preTranslate(binding.offsetX, binding.offsetY)
                        preRotate(binding.rotationDegrees)
                        preScale(binding.scale, binding.scale)
                        preTranslate(-binding.pivotX, -binding.pivotY)
                        postConcat(instanceMatrix)
                    },
                )
            }
    }
}
