package com.donnclab.kinetiframe.domain.render

import com.donnclab.kinetiframe.data.actor.ActorStoreResult
import java.io.File
import java.nio.file.Files
import java.nio.file.attribute.FileTime

/** Bounded decoded documents; precise file stamps invalidate edited performance snapshots. */
internal class ActorDocumentCache<T>(private val capacity: Int = 24) {
    private data class Stamp(val modified: FileTime, val bytes: Long)
    private data class Entry<T>(val stamp: Stamp, val value: T)
    private val entries = LinkedHashMap<String, Entry<T>>(16, .75f, true)

    @Synchronized fun load(file: File, decode: () -> ActorStoreResult<T>): ActorStoreResult<T> {
        if (!file.isFile) return decode()
        // A concurrent replace/delete should resolve through the repository's normal error path.
        val stamp = try {
            Stamp(Files.getLastModifiedTime(file.toPath()), file.length())
        } catch (
            _: java.io.IOException,
        ) {
            return decode()
        }
        val key = file.absolutePath
        entries[key]?.takeIf { it.stamp == stamp }?.let { return ActorStoreResult.Value(it.value) }
        val result = decode()
        if (result is ActorStoreResult.Value) {
            entries[key] = Entry(stamp, result.value)
            while (entries.size > capacity) entries.remove(entries.keys.first())
        }
        return result
    }

    @Synchronized fun clear() = entries.clear()
}
