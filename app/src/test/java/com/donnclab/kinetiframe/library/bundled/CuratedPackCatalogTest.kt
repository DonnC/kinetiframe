package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.library.model.PackManifest
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.io.File
import java.util.zip.ZipFile

class CuratedPackCatalogTest {
    @Test fun `four curated packs have valid names and their drawings`() {
        val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
        assertEquals(
            listOf(BundledDemoInstaller.TAVI_PRESENTATION_ID),
            BundledDemoInstaller.projectIds,
        )
        assertEquals(4, BundledDemoInstaller.packIds.size)
        BundledDemoInstaller.packIds.forEach { id ->
            ZipFile(
                com.donnclab.kinetiframe.library.bundled.DemoAssetFixtures.file(
                    assets,
                    BundledDemoInstaller.packAssetPath(id),
                ),
            ).use { zip ->
                val manifest = Json.decodeFromString<PackManifest>(
                    zip.getInputStream(zip.getEntry("meta.json")).bufferedReader().readText(),
                ).validate()
                manifest.items.forEach { assertNotNull(zip.getEntry(it.file), "$id/${it.file}") }
                if (id == "ls-expressions-v3") assertEquals(12, manifest.totalItems)
                if (id == "ls-eyes-v3") assertEquals(6, manifest.totalItems)
                if (id == "ls-mouths-v3") assertEquals(14, manifest.totalItems)
                if (id == "ls-studio-items-v3") assertEquals(11, manifest.totalItems)
            }
        }
    }
}
