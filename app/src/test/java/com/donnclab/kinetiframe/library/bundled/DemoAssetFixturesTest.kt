package com.donnclab.kinetiframe.library.bundled

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class DemoAssetFixturesTest {
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")

    @Test fun `shipping assets hold only the current unversioned files`() {
        listOf("demos", "loose-signal", "backgrounds/v1", "library/v1").forEach {
            assertFalse(File(assets, it).exists(), it)
        }
        BundledDemoInstaller.projectIds.forEach {
            val path = BundledDemoInstaller.projectAssetPath(it)
            assertTrue(File(assets, path).isFile, path)
            assertEquals(File(assets, path), DemoAssetFixtures.file(assets, path))
        }
        assertTrue(DemoAssetFixtures.file(assets, BundledActorSourceReader.ASSET_PATH).isFile)
        val packs = BundledDemoInstaller.packIds.map { BundledDemoInstaller.packAssetPath(it) }
        packs.forEach { assertTrue(File(assets, it).isFile, it) }
        assertEquals(packs.map { it.substringAfter('/') }.toSet(), File(assets, "packs").list()!!.toSet())
    }
}
