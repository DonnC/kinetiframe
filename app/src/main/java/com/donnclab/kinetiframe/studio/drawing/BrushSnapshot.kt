// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.drawing

import kotlinx.serialization.Serializable

@Serializable
enum class BrushId(val displayName: String) {
    PEN("Pen"),
    PENCIL("Pencil"),
    PAINT("Paint Brush"),
    AIRBRUSH("Airbrush"),
    HIGHLIGHTER("Highlighter"),
    CRAYON("Crayon"),
    INK("Ink"),
    HAIR("Hair"),
    FUR("Fur"),
    SKETCH("Sketch"),
    CALLIGRAPHY("Calligraphy"),
    SPRAY("Spray"),
    STIPPLE("Stipple"),
    ROUGH_PEN("Rough Pen"),
    GRAIN_PEN("Grain Pen"),
    DASH("Dash"),
    GEAR("Gear"),
    SAW("Saw"),
}

/** Immutable recipe carried by every completed stroke for deterministic replay. */
@Serializable
data class BrushSnapshot(val id: BrushId = BrushId.PEN, val rendererVersion: Int = 1, val seed: Long = 0L)

object BrushCatalog {
    val drawingBrushes: List<BrushId> = listOf(
        BrushId.PEN, BrushId.PENCIL, BrushId.PAINT, BrushId.AIRBRUSH,
        BrushId.HIGHLIGHTER, BrushId.CRAYON, BrushId.INK,
        BrushId.HAIR, BrushId.FUR, BrushId.SKETCH, BrushId.CALLIGRAPHY,
        BrushId.SPRAY, BrushId.STIPPLE, BrushId.ROUGH_PEN, BrushId.GRAIN_PEN,
        BrushId.DASH, BrushId.GEAR, BrushId.SAW,
    )
}
