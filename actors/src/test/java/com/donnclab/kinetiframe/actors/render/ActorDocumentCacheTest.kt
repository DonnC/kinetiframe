package com.donnclab.kinetiframe.actors.render

import com.donnclab.kinetiframe.data.storage.DurableFile
import com.donnclab.kinetiframe.data.storage.StoreResult
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ActorDocumentCacheTest {
    @TempDir lateinit var root: File

    @Test fun `unchanged documents decode once and committed edits invalidate cache`() {
        val file = File(root, "performance.json")
        DurableFile.write(file) { it.write("first".toByteArray()) }
        val cache = ActorDocumentCache<String>(2)
        var reads = 0
        fun load() = cache.load(file) {
            reads++
            StoreResult.Value(file.readText())
        }
        repeat(100) { assertEquals(StoreResult.Value("first"), load()) }
        assertEquals(1, reads)
        DurableFile.write(file) { it.write("changed".toByteArray()) }
        assertEquals(StoreResult.Value("changed"), load())
        assertEquals(2, reads)
        cache.clear()
        load()
        assertEquals(3, reads)
    }
}
