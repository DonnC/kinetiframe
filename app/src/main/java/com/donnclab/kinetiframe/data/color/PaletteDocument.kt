package com.donnclab.kinetiframe.data.color

import kotlinx.serialization.Serializable

/** Exact sRGB values, not pixels sampled from a reference image. Alpha is preserved. */
@Serializable
data class ColorSwatch(val name: String, val argb: Long) {
    fun validate() = apply {
        require(name.isNotBlank() && name.length <= 40) { "Invalid swatch name" }
        require(argb in 0..0xFFFFFFFFL) { "Invalid ARGB colour" }
    }
}

@Serializable
data class PaletteDocument(
    val id: String,
    val name: String,
    val swatches: List<ColorSwatch>,
    val schemaVersion: Int = 1,
) {
    fun validate() = apply {
        require(schemaVersion == 1) { "Unsupported palette version" }
        require(id.matches(Regex("[a-zA-Z0-9][a-zA-Z0-9_-]{0,79}"))) { "Invalid palette ID" }
        require(name.isNotBlank() && name.length <= 60) { "Invalid palette name" }
        require(swatches.size in 1..128) { "Palette must contain 1–128 swatches" }
        require(swatches.map { it.name }.distinct().size == swatches.size) { "Duplicate swatch names" }
        swatches.forEach { it.validate() }
    }
}
