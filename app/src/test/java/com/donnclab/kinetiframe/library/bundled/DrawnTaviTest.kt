package com.donnclab.kinetiframe.library.bundled

import com.donnclab.kinetiframe.actors.data.ActorRepository
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.core.contracts.StoreResult
import com.donnclab.kinetiframe.data.storage.DocumentFileStore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/** The drawn Tavi is curves on a skeleton: no artwork files, every piece valid against its bones. */
class DrawnTaviTest {
    @TempDir lateinit var root: File
    private val assets = File("src/main/assets").takeIf(File::isDirectory) ?: File("app/src/main/assets")

    @Test fun `the bundled drawn Tavi loads as a figure with no image artwork`() {
        val files = DocumentFileStore(root)
        val id = BundledActorSampleInstaller.DRAWN_TAVI_ID
        BundledActorSourceReader.read(File(assets, BundledActorSourceReader.ASSET_PATH), files, setOf(id))
        val loaded = ActorRepository(files).load(id)
        assertTrue(loaded is StoreResult.Value, loaded.toString())
        val (manifest, revision) = (loaded as StoreResult.Value).value
        assertEquals(setOf(ActorView.FRONT), manifest.viewRevisions.keys)
        revision.validate()
        assertTrue(revision.artworkAssetPaths.isEmpty())
        val figure = requireNotNull(revision.sceneGraph.figure)
        assertEquals(4, figure.tubes.size)
        assertEquals(
            setOf("default", "fist", "point", "palm"),
            figure.parts.first {
                it.id == "right_hand"
            }.variants.keys,
        )
        assertEquals(setOf("default", "laugh", "skeptical"), figure.parts.first { it.id == "eyes" }.variants.keys)
        // Arms grow out of the shirt and carry cuffs; legs don't.
        assertTrue(figure.tubes.filter { it.id.endsWith("arm") }.all { it.cuff && it.rootPartId == "shirt" })
        assertTrue(figure.tubes.filter { it.id.endsWith("leg") }.none { it.cuff })
    }
}
