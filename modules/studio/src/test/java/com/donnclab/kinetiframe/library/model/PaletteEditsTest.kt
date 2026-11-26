package com.donnclab.kinetiframe.library.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class PaletteEditsTest {
    private val red = 0xFFC9372CL

    @Test fun `adding keeps one swatch per colour and names it by hex`() {
        val palette = PaletteEdits.create("Tavi", red, id = "tavi")
        assertEquals("#C9372C", palette.swatches.single().name)
        assertSame(palette, PaletteEdits.add(palette, red))
        assertEquals(listOf("#C9372C", "#80C9372C"), PaletteEdits.add(palette, 0x80C9372CL).swatches.map { it.name })
    }

    @Test fun `removing the last colour leaves no palette`() {
        val palette = PaletteEdits.add(PaletteEdits.create("Tavi", red, id = "tavi"), 0xFF000000L)
        assertEquals(listOf("#000000"), PaletteEdits.remove(palette, "#C9372C")?.swatches?.map { it.name })
        assertNull(PaletteEdits.remove(PaletteEdits.create("One", red, id = "one"), "#C9372C"))
    }

    @Test fun `a full palette refuses more colours`() {
        var palette = PaletteEdits.create("Full", 0xFF000000L, id = "full")
        for (i in 1 until PaletteEdits.MAX_SWATCHES) palette = PaletteEdits.add(palette, 0xFF000000L + i)
        assertThrows<IllegalArgumentException> { PaletteEdits.add(palette, red) }
    }

    @Test fun `a new palette keeps several colours in order without repeats`() {
        val palette = PaletteEdits.create("Night", listOf(red, 0xFF000000L, red), id = "night")
        assertEquals(listOf("#C9372C", "#000000"), palette.swatches.map { it.name })
        assertThrows<IllegalArgumentException> { PaletteEdits.create("Empty", emptyList(), id = "empty") }
    }

    @Test fun `renaming and duplicating keep the colours`() {
        val palette = PaletteEdits.add(PaletteEdits.create("Tavi", red, id = "tavi"), 0xFF000000L)
        val renamed = PaletteEdits.rename(palette, "  Tavi skin  ")
        assertEquals("Tavi skin", renamed.name)
        assertEquals(palette.swatches, renamed.swatches)
        val copy = PaletteEdits.duplicate(palette, "Tavi copy", id = "tavi-copy")
        assertEquals("tavi-copy", copy.id)
        assertEquals(palette.swatches, copy.swatches)
    }

    @Test fun `hex reads short long and alpha forms`() {
        assertEquals(0xFFFF0000L, PaletteEdits.parseHex("#f00"))
        assertEquals(red, PaletteEdits.parseHex("C9372C"))
        assertEquals(0x80C9372CL, PaletteEdits.parseHex("#80C9372C"))
        assertNull(PaletteEdits.parseHex("#12"))
        assertNull(PaletteEdits.parseHex("zzzzzz"))
    }

    @Test fun `a swatch named after its hex is read once`() {
        assertEquals("#FF7C00", PaletteEdits.spokenName("#FF7C00", 0xFFFF7C00L))
        assertEquals("#FF7C00", PaletteEdits.spokenName("ff7c00", 0xFFFF7C00L))
        assertEquals("Skin, #F2C9A0", PaletteEdits.spokenName("Skin", 0xFFF2C9A0L))
    }
}
