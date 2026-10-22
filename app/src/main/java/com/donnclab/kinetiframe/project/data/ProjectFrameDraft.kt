package com.donnclab.kinetiframe.project.data

import com.donnclab.kinetiframe.data.storage.DurableFile
import java.io.File

/** Raster cache evictions are draft writes, not saves. Copy on IO before opening the engine;
 * only an explicit Save publishes draft pixels. Discard simply abandons the private cache.
 * Streaming copies keep large projects off the heap. Saved cels are never deleted here. */
object ProjectFrameDraft {
    fun open(saved: File, draft: File) {
        require(saved.canonicalFile != draft.canonicalFile)
        require(!draft.exists()) { "Frame draft already exists" }
        copy(saved, draft, durable = false)
    }

    fun publish(draft: File, saved: File) {
        require(saved.canonicalFile != draft.canonicalFile)
        copy(draft, saved, durable = true)
    }

    private fun copy(source: File, destination: File, durable: Boolean) {
        require(destination.mkdirs() || destination.isDirectory)
        if (!source.exists()) return
        val root = source.canonicalFile.toPath()
        source.walkTopDown().filter(File::isFile).forEach { file ->
            val resolved = file.canonicalFile.toPath()
            require(resolved.startsWith(root)) { "Unsafe frame cache path" }
            val target = File(destination, file.relativeTo(source).path)
            require(target.canonicalFile.toPath().startsWith(destination.canonicalFile.toPath()))
            if (durable) {
                DurableFile.write(target) { output -> file.inputStream().buffered().use { it.copyTo(output) } }
            } else {
                requireNotNull(target.parentFile).mkdirs()
                file.copyTo(target)
            }
        }
    }
}
