// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import kotlinx.serialization.Serializable

data class SkinSourceSize(val width: Int, val height: Int)
data class SkinGrid(val columns: Int, val rows: Int)

// Bind coordinates stay actor-space; viewport movement must not alter weights or source pixels.
@Serializable(with = ArtworkSkinSerializer::class)
data class ArtworkSkin(
    val source: SkinSourceSize,
    val grid: SkinGrid,
    val mesh: SkinMesh,
    val attachmentBlend: Boolean = false,
) {
    val sourceWidth get() = source.width
    val sourceHeight get() = source.height
    val columns get() = grid.columns
    val rows get() = grid.rows
    val bones get() = mesh.bones
    val vertices get() = mesh.vertices
}
