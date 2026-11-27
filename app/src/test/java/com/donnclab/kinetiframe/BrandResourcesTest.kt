package com.donnclab.kinetiframe

import androidx.compose.ui.graphics.toArgb
import com.donnclab.kinetiframe.designsystem.theme.BrandCarmine
import com.donnclab.kinetiframe.designsystem.theme.BrandGraphite
import com.donnclab.kinetiframe.designsystem.theme.BrandPaper
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.studio.OnionSkinColors
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

class BrandResourcesTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res"))
        .first { it.isDirectory }
    private val factory = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }

    @Test fun `XML and Compose share the three identity colours`() {
        val nodes = factory.newDocumentBuilder().parse(File(res, "values/colors.xml")).getElementsByTagName("color")
        val colours = (0 until nodes.length).associate {
            nodes.item(it).attributes.getNamedItem("name").nodeValue to nodes.item(it).textContent
        }
        mapOf(
            "brand_carmine" to BrandCarmine,
            "brand_graphite" to BrandGraphite,
            "brand_paper" to BrandPaper,
        ).forEach { (name, colour) ->
            assertEquals(colour.toArgb() and 0xFFFFFF, colours.getValue(name).removePrefix("#").toInt(16))
        }
    }

    @Test fun `launcher, themed and splash icons are vectors`() {
        listOf(
            "ic_launcher_foreground.xml" to "108dp",
            "ic_launcher_monochrome.xml" to "108dp",
            "ic_splash_mark.xml" to "240dp",
        ).forEach { (file, size) ->
            val vector = factory.newDocumentBuilder().parse(File(res, "drawable/$file")).documentElement
            assertEquals("vector", vector.tagName, file)
            assertEquals(size, vector.getAttribute("android:width"), file)
            assertTrue(vector.getElementsByTagName("path").length > 0, file)
        }
        // A themed icon is tinted by the system: one shape, no brand colours of its own.
        val monochrome = File(res, "drawable/ic_launcher_monochrome.xml").readText()
        assertFalse(monochrome.contains("@color/"))
        assertEquals(1, Regex("<path").findAll(monochrome).count())
        listOf("ic_launcher.xml", "ic_launcher_round.xml").forEach { file ->
            val icon = factory.newDocumentBuilder().parse(File(res, "mipmap-anydpi/$file"))
            val monochromeLayer = icon.getElementsByTagName("monochrome").item(0)
            assertEquals(
                "@drawable/ic_launcher_monochrome",
                monochromeLayer.attributes.getNamedItem("android:drawable").nodeValue,
            )
        }
        assertTrue(File(res, "values-night/colors.xml").isFile)
        // No raster brand art remains to drift from the vectors.
        assertTrue(res.walkTopDown().none { it.name.startsWith("brand_") && it.extension == "png" })
    }

    @Test fun `onion skins default to the editor's carmine and blue`() {
        assertEquals(EditorTokens.onionBefore.toArgb(), OnionSkinColors.BEFORE)
        assertEquals(EditorTokens.onionAfter.toArgb(), OnionSkinColors.AFTER)
    }
}
