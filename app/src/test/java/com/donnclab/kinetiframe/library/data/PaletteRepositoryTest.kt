package com.donnclab.kinetiframe.library.data

import com.donnclab.kinetiframe.actors.data.ActorFileStore
import com.donnclab.kinetiframe.actors.data.ActorStoreResult
import com.donnclab.kinetiframe.actors.data.validate
import com.donnclab.kinetiframe.library.data.LibraryRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class PaletteRepositoryTest {
    @TempDir lateinit var root: File

    @Test fun `eight palettes preserve all 98 exact named swatches across restart`() {
        val files = ActorFileStore(root)
        val repository = PaletteRepository(files)
        repository.installStarters()
        assertEquals(8, repository.list().size)
        assertEquals(98, repository.list().sumOf { it.swatches.size })
        assertEquals(
            BundledPalettes.entries.associateBy {
                it.id
            },
            PaletteRepository(ActorFileStore(root)).list().associateBy { it.id },
        )
        repository.installStarters()
        assertEquals(8, repository.list().size)
        assertEquals(0xFF17999DL, repository.list().single { it.name == "Loose Signal" }.swatches.first().argb)
    }

    @Test fun `deleted starter is not resurrected and user-owned ID is untouched`() {
        val files = ActorFileStore(root)
        val palette = BundledPalettes.entries.first().copy(
            name = "My colours",
            swatches = listOf(ColorSwatch("Transparent", 0x00112233)),
        )
        val repository = PaletteRepository(files)
        repository.save(palette)
        repository.installStarters()
        assertEquals(palette, repository.list().single { it.id == palette.id })
        assertTrue(LibraryRepository(files).delete(palette.id) is ActorStoreResult.Value)
        PaletteRepository(ActorFileStore(root)).installStarters()
        assertEquals(7, repository.list().size)
    }

    @Test fun `invalid colours empty palettes and unsafe identities are rejected`() {
        assertThrows(IllegalArgumentException::class.java) { ColorSwatch("Bad", 0x100000000).validate() }
        assertThrows(IllegalArgumentException::class.java) {
            PaletteDocument("../escape", "Bad", listOf(ColorSwatch("Red", 0xFFFF0000))).validate()
        }
        assertThrows(IllegalArgumentException::class.java) { PaletteDocument("empty", "Empty", emptyList()).validate() }
        assertThrows(IllegalArgumentException::class.java) {
            PaletteDocument(
                "duplicate",
                "Duplicate",
                listOf(ColorSwatch("Red", 0xFFFF0000), ColorSwatch("Red", 0xFFFF0011)),
            ).validate()
        }
    }

    @Test fun `library copy is independent of original palette payload`() {
        val files = ActorFileStore(root)
        val repository = PaletteRepository(files)
        repository.installStarters()
        val library = LibraryRepository(files)
        val item = (library.list() as ActorStoreResult.Value).value.first()
        val destination = File(root, "copied-palette.json")
        assertTrue(library.copyAssetTo(item.libraryItemId, destination) is ActorStoreResult.Value)
        assertTrue(library.delete(item.libraryItemId) is ActorStoreResult.Value)
        assertTrue(destination.isFile && destination.readText().contains("swatches"))
    }
}
