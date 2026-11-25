// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.library.model

import java.util.Locale
import java.util.UUID

/** Edits to palettes the user owns. Each returns a new, valid palette document. */
object PaletteEdits {
    const val MAX_SWATCHES = 128

    /** A new palette holding [argb], named [name]. */
    fun create(name: String, argb: Long, id: String = "palette-${UUID.randomUUID()}") =
        PaletteDocument(id, name.trim().take(60), listOf(swatch(argb, emptyList()))).validate()

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
