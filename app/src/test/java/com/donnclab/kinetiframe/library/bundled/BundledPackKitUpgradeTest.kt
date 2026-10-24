package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import com.donnclab.kinetiframe.data.storage.StoreResult
import com.donnclab.kinetiframe.library.data.LibraryRepository
import com.donnclab.kinetiframe.library.data.PackArchiver
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BundledPackKitUpgradeTest {
    @TempDir lateinit var root: File

    @Test fun `only unchanged old kits are archived after replacements exist`() {
        val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
        val files = DocumentFileStore(root)
        val library = LibraryRepository(files)
        listOf("tavi", "mira").forEach { name ->
            assertTrue(
                PackArchiver.import(
                    files,
                    library,
                    File(assets, "loose-signal/curated/v3/ls-$name-cutouts-v5.kpack"),
                    "ls-$name-cutouts-v5",
                    nowEpochMs = 1,
                ) is StoreResult.Value,
            )
            assertTrue(
                PackArchiver.import(
                    files,
                    library,
                    File(assets, "loose-signal/curated/v4/ls-$name-cutouts-v6.kpack"),
                    "ls-$name-cutouts-v6",
                    nowEpochMs = 2,
                ) is StoreResult.Value,
            )
        }
        // Simulate changed pixels. The upgrader must not confuse an edited old kit with shipped art.
        val source = library.packItemFile("ls-mira-cutouts-v5", "frame_000.png")
        source.appendBytes(byteArrayOf(7))
        val upgrade = BundledPackKitUpgrade(files) { File(assets, it).inputStream() }
        assertTrue(upgrade.upgrade().isEmpty())
        assertFalse(files.libraryDirectory("ls-tavi-cutouts-v5").exists())
        assertTrue(File(root, "archive/starter-kits-v4/library/ls-tavi-cutouts-v5").isDirectory)
        assertTrue(files.libraryDirectory("ls-mira-cutouts-v5").isDirectory)
        assertTrue(upgrade.upgrade().isEmpty())
    }
}
