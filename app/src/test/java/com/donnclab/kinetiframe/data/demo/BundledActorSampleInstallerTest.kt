package com.donnclab.kinetiframe.data.demo

import com.donnclab.kinetiframe.data.actor.ActorBundleRepository
import com.donnclab.kinetiframe.data.actor.ActorFileStore
import com.donnclab.kinetiframe.data.actor.ActorRepository
import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import com.donnclab.kinetiframe.data.actor.LibraryItemKind
import com.donnclab.kinetiframe.data.actor.PerformanceRepository
import com.donnclab.kinetiframe.data.library.LibraryRepository
import com.donnclab.kinetiframe.data.project.KframeArchiver
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class BundledActorSampleInstallerTest {
    @TempDir lateinit var root: File
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")
    private fun <T> value(result: ActorStoreResult<T>): T = when (result) {
        is ActorStoreResult.Value -> result.value
        is ActorStoreResult.Failure -> throw AssertionError(result.message, result.cause)
    }

    @Test fun `library samples use originals retain views and never reinstall deleted items`() {
        val files = ActorFileStore(root)
        val installer =
            BundledActorSampleInstaller(files) {
                com.donnclab.kinetiframe.data.demo.DemoAssetFixtures.file(assets, it).inputStream()
            }
        assertEquals(BundledDemoInstaller.Result(3, emptyList()), installer.install())
        val library = LibraryRepository(files)
        val entries = value(library.list())
        assertEquals(3, entries.count { it.kind == LibraryItemKind.ACTOR })
        assertEquals(0, entries.count { it.kind == LibraryItemKind.PERFORMANCE })
        assertEquals(0, files.projectsRoot().listFiles().orEmpty().size)
        val actors = ActorRepository(files)
        val bundles = ActorBundleRepository(files, actors, PerformanceRepository(files))
        val imported =
            value(bundles.importActor("starter-ls-tavi-all-views-v3", "copy", "revision-copy", 1791158400001L))
        assertEquals(4, imported.authoredViews().size)
        imported.authoredViews().values.forEach { id ->
            val revision = value(actors.loadRevision("copy", id))
            assertTrue(revision.artworkAssetPaths.size >= 20)
            revision.artworkAssetPaths.values.forEach { assertTrue(File(files.actorDirectory("copy"), it).isFile) }
        }
        val continuous =
            value(bundles.importActor("starter-ls-mira-all-views-v3", "mira-copy", "mira-revision", 1791158400001L))
        val mira = value(actors.loadRevision("mira-copy", continuous.currentRevisionId))
        assertEquals(5, mira.sceneGraph.bones.values.count { it.textureBinding?.skin != null })
        mira.artworkAssetPaths.values.forEach { assertTrue(File(files.actorDirectory("mira-copy"), it).isFile) }
        value(library.delete("starter-ls-tavi-all-views-v3"))
        assertEquals(0, installer.install().installed)
        assertFalse(files.libraryDirectory("starter-ls-tavi-all-views-v3").exists())
        assertTrue(File(root, "demo-receipts/actor-samples-v1").listFiles().orEmpty().none { it.isDirectory })
    }
}
