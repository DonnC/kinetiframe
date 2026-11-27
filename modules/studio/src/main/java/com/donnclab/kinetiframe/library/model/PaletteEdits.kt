// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.library.model

import java.util.Locale
import java.util.UUID

/** Edits to palettes the user owns. Each returns a new, valid palette document. */
object PaletteEdits {
    const val MAX_SWATCHES = 128

    const val MAX_NAME = 60

    /** A new palette holding [argb], named [name]. */
    fun create(name: String, argb: Long, id: String = newId()) = create(name, listOf(argb), id)

    /**
     * A new palette named [name] holding [colours] in order, without repeats and at most
     * [MAX_SWATCHES] of them. [colours] must not be empty.
     */
    fun create(name: String, colours: List<Long>, id: String = newId()): PaletteDocument {
        require(colours.isNotEmpty()) { "A palette needs a colour" }
        val swatches = colours.distinct().take(MAX_SWATCHES)
            .fold(emptyList<ColorSwatch>()) { made, argb -> made + swatch(argb, made) }
        return PaletteDocument(id, name.trim().take(MAX_NAME), swatches).validate()
    }

    /** [palette] named [name] instead. */
    fun rename(palette: PaletteDocument, name: String): PaletteDocument =
        palette.copy(name = name.trim().take(MAX_NAME)).validate()

    /** A new palette of the user's own with [palette]'s colours, named [name]. */
    fun duplicate(palette: PaletteDocument, name: String, id: String = newId()): PaletteDocument =
        PaletteDocument(id, name.trim().take(MAX_NAME), palette.swatches).validate()

    private fun newId() = "palette-${UUID.randomUUID()}"

    /** [palette] with [argb] added at the end, unless it already holds that exact colour. */
    fun add(palette: PaletteDocument, argb: Long): PaletteDocument {
        if (palette.swatches.any { it.argb == argb }) return palette
        require(palette.swatches.size < MAX_SWATCHES) { "This palette is full" }
        return palette.copy(swatches = palette.swatches + swatch(argb, palette.swatches)).validate()
    }

    /** [palette] without the swatch named [name], or null when that was its last colour. */
    fun remove(palette: PaletteDocument, name: String): PaletteDocument? {
        val left = palette.swatches.filterNot { it.name == name }
        return if (left.isEmpty()) null else palette.copy(swatches = left).validate()
    }

    /** "#RRGGBB" for an opaque colour, "#AARRGGBB" otherwise. */
    fun hex(argb: Long): String = if (argb ushr 24 == 0xFFL) {
        String.format(Locale.ROOT, "#%06X", argb and 0xFFFFFF)
    } else {
        String.format(Locale.ROOT, "#%08X", argb and 0xFFFFFFFFL)
    }

    /**
     * What a screen reader says for a swatch: its name and hex, or the hex alone when the name is
     * just the hex, as for colours the user saved, which used to be read twice (R3-15).
     */
    fun spokenName(name: String, argb: Long): String {
        val hex = hex(argb)
        return if (name.isBlank() || parseHex(name) == argb) hex else "$name, $hex"
    }

    /** Parses "#RGB", "#RRGGBB" or "#AARRGGBB", with or without "#". Null when it isn't a colour. */
    fun parseHex(text: String): Long? {
        val digits = text.trim().removePrefix("#")
        if (digits.isEmpty() || digits.any { it.digitToIntOrNull(16) == null }) return null
        return when (digits.length) {
            3 -> 0xFF000000L or digits.map { "$it$it" }.joinToString("").toLong(16)
            6 -> 0xFF000000L or digits.toLong(16)
            8 -> digits.toLong(16)
            else -> null
        }
    }

    // Swatch names must be unique in a palette, so a repeated hex gets a number.
    private fun swatch(argb: Long, existing: List<ColorSwatch>): ColorSwatch {
        val base = hex(argb)
        val names = existing.map { it.name }.toSet()
        val name = generateSequence(1) { it + 1 }.map { if (it == 1) base else "$base $it" }.first { it !in names }
        return ColorSwatch(name, argb)
    }
}
