package com.donnclab.kinetiframe.data.storage

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors

class DurableFileTest {
    @TempDir lateinit var root: File

    @Test fun `failed replacement preserves previous committed bytes and cleans staging`() {
        val file = File(root, "resource.json")
        DurableFile.write(file) { it.write("old".toByteArray()) }
        assertThrows(IOException::class.java) {
            DurableFile.write(file) {
                it.write("partial".toByteArray())
                throw IOException("disk full")
            }
        }
        assertEquals("old", file.readText())
        assertEquals(listOf("resource.json"), root.listFiles()!!.map { it.name })
    }

    @Test fun `simultaneous writers publish whole files without sharing temporary paths`() {
        val file = File(root, "resource.json")
        val pool = Executors.newFixedThreadPool(4)
        try {
            pool.invokeAll(
                (0 until 20).map { index ->
                    java.util.concurrent.Callable {
                        DurableFile.write(file) { it.write(index.toString().repeat(1000).toByteArray()) }
                    }
                },
            ).forEach { it.get() }
        } finally {
            pool.shutdown()
        }
        assertTrue((0 until 20).any { file.readText() == it.toString().repeat(1000) })
        assertEquals(1, root.listFiles()!!.size)
    }
}
