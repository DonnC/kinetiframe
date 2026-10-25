package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

class BundledActorSourceReaderTest {
    @TempDir lateinit var root: File

    @Test fun `starter actor source contains only reusable actor payloads`() {
        val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
        ZipFile(File(assets, BundledActorSourceReader.ASSET_PATH)).use { zip ->
            val entries = zip.entries().asSequence().map { it.name }.toList()
            assertTrue(entries.isNotEmpty())
            assertTrue(entries.all { it.startsWith("dependencies/actors/") })
            assertEquals(3, entries.count { it.endsWith("/manifest.json") })
            assertEquals(12, entries.count { it.contains("/revisions/") && it.endsWith(".json") })
        }
    }

    @Test fun `actor extraction does not install project frames or performances`() {
        val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
        val files = DocumentFileStore(File(root, "stage"))
        BundledActorSourceReader.read(
            File(assets, BundledActorSourceReader.ASSET_PATH),
            files,
            setOf("ls-tavi-all-views-v3"),
        )
        assertTrue(ActorRepository(files).load("ls-tavi-all-views-v3") is StoreResult.Value)
        assertTrue(files.projectsRoot().listFiles().orEmpty().isEmpty())
        assertFalse(files.actorDirectory("ls-mira-all-views-v3").exists())
    }

    @Test fun `an actor dependency cannot escape its staging root`() {
        val archive = File(root, "bad.zip")
        ZipOutputStream(archive.outputStream()).use { zip ->
            zip.putNextEntry(ZipEntry("dependencies/actors/actor/../../escaped.txt"))
            zip.write(byteArrayOf(1))
            zip.closeEntry()
        }
        assertThrows(IllegalArgumentException::class.java) {
            BundledActorSourceReader.read(archive, DocumentFileStore(File(root, "stage")), setOf("actor"))
        }
        assertFalse(File(root, "stage/escaped.txt").exists())
    }
}
