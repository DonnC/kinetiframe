package com.donnclab.kinetiframe.library.bundled

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DemoAssetFixturesTest {
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")

    @Test fun `shipping catalog has no retired projects or archive fallback`() {
        val historical = listOf(
            "demos/v1",
            "loose-signal/street",
            "loose-signal/cast-views/v1",
            "loose-signal/cast-views/v2",
            "loose-signal/cast-views/v3",
            "loose-signal/curated/v1",
            "loose-signal/curated/v2",
        )
        historical.forEach { assertFalse(File(assets, it).exists(), it) }
        BundledDemoInstaller.projectIds.forEach {
            val path = BundledDemoInstaller.projectAssetPath(it)
            assertTrue(File(assets, path).isFile, path)
            assertEquals(File(assets, path), DemoAssetFixtures.file(assets, path))
        }
        assertFalse(File(assets, "demos/v1/demo-stubborn-ball-v1.kframe").exists())
        assertTrue(DemoAssetFixtures.file(assets, BundledActorSourceReader.ASSET_PATH).isFile)
        assertFalse(File(assets, "loose-signal/education").exists())
        assertFalse(File(assets, "loose-signal/cast-views").exists())
        for (name in listOf("tavi", "mira", "kuda")) {
            assertTrue(File(assets, "loose-signal/curated/v4/ls-$name-cutouts-v6.kpack").isFile)
            assertTrue(File(assets, "loose-signal/curated/v3/ls-$name-cutouts-v5.kpack").isFile)
        }
        for (name in listOf("expressions", "eyes", "mouths", "studio-items")) {
            assertTrue(File(assets, "loose-signal/curated/v3/ls-$name-v3.kpack").isFile)
            assertFalse(File(assets, "loose-signal/curated/v4/ls-$name-v3.kpack").exists())
        }
    }
}
