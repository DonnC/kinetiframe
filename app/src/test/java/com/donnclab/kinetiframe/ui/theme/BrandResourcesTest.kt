package com.donnclab.kinetiframe.ui.theme

import androidx.compose.ui.graphics.toArgb
import com.donnclab.kinetiframe.designsystem.theme.BrandAmber
import com.donnclab.kinetiframe.designsystem.theme.BrandInk
import com.donnclab.kinetiframe.designsystem.theme.BrandIvory
import com.donnclab.kinetiframe.designsystem.theme.BrandMint
import com.donnclab.kinetiframe.designsystem.theme.BrandTeal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File
import javax.imageio.ImageIO
import javax.xml.parsers.DocumentBuilderFactory

class BrandResourcesTest {
    private val res = listOf(File("src/main/res"), File("app/src/main/res"))
        .first { it.isDirectory }
    private val factory = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    }

    @Test fun `XML and Compose share the five identity colours`() {
        val nodes = factory.newDocumentBuilder().parse(File(res, "values/colors.xml")).getElementsByTagName("color")
        val colours = (0 until nodes.length).associate {
            nodes.item(it).attributes.getNamedItem("name").nodeValue to nodes.item(it).textContent
        }
        mapOf(
            "brand_teal" to BrandTeal,
            "brand_amber" to BrandAmber,
            "brand_ivory" to BrandIvory,
            "brand_ink" to BrandInk,
            "brand_rig" to BrandMint,
        ).forEach { (name, colour) ->
            assertEquals(colour.toArgb() and 0xFFFFFF, colours.getValue(name).removePrefix("#").toInt(16))
        }
    }

    @Test fun `launcher and splash resources use Android canonical dimensions`() {
        mapOf(
            "brand_launcher_pixels" to 108,
            "brand_monochrome_pixels" to 108,
            "brand_splash_pixels" to 288,
        ).forEach { (file, size) ->
            mapOf(
                "mdpi" to 1.0,
                "hdpi" to 1.5,
                "xhdpi" to 2.0,
                "xxhdpi" to 3.0,
                "xxxhdpi" to 4.0,
            ).forEach { (density, scale) ->
                val image = ImageIO.read(File(res, "drawable-$density/$file.png"))
                assertEquals((size * scale).toInt(), image.width)
                assertEquals((size * scale).toInt(), image.height)
            }
        }
        listOf("ic_launcher.xml", "ic_launcher_round.xml").forEach { file ->
            val icon = factory.newDocumentBuilder().parse(File(res, "mipmap-anydpi/$file"))
            val monochrome = icon.getElementsByTagName("monochrome").item(0)
            assertEquals(
                "@drawable/ic_launcher_monochrome",
                monochrome.attributes.getNamedItem("android:drawable").nodeValue,
            )
        }
        assertTrue(File(res, "values-night/colors.xml").isFile)
    }
}
