package com.donnclab.kinetiframe.domain.render

import java.lang.ref.WeakReference

/**
 * Bounds strong cache ownership, not ownership by frames currently being displayed.
 * Eviction must not destroy a resource borrowed by a render snapshot or the Android display
 * list. Weak entries also let subsequent frames reuse those still-live resources after eviction.
 */
internal class SharedResourceCache<K, V : Any>(
    private val maxEntries: Int,
    private val maxBytes: Long,
    private val sizeOf: (V) -> Long,
) {
    init {
        require(maxEntries > 0 && maxBytes > 0)
    }

    private val strong = LinkedHashMap<K, V>(16, 0.75f, true)
    private val borrowed = HashMap<K, WeakReference<V>>()
    private var bytes = 0L

    internal val retainedBytes: Long
        @Synchronized get() = bytes

    internal val retainedEntries: Int
        @Synchronized get() = strong.size

    @Synchronized
    fun getOrLoad(key: K, load: () -> V?): V? {
        strong[key]?.let { return it }
        val value = borrowed[key]?.get() ?: load() ?: return null
        borrowed.entries.removeAll { it.value.get() == null }
        borrowed[key] = WeakReference(value)
        val size = sizeOf(value).coerceAtLeast(0)
        if (size <= maxBytes) {
            strong[key] = value
            bytes += size
            val iterator = strong.entries.iterator()
            while (strong.size > maxEntries || bytes > maxBytes) {
                bytes -= sizeOf(iterator.next().value).coerceAtLeast(0)
                iterator.remove()
            }
        }
        return value
    }

    @Synchronized
    fun clear() {
        strong.clear()
        borrowed.clear()
        bytes = 0
    }
}
