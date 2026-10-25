package com.donnclab.kinetiframe.studio.reference

/** Session-only slots. Insertions move existing references; new/duplicated frames start empty.
 * Admission, not eviction: importing a guide must never silently erase another frame's guide. */
class FrameReferenceIndex<T>(private val budgetBytes: Long, private val bytes: (T) -> Long) {
    private var entries = mutableMapOf<Int, T>()
    fun getOrPut(frame: Int, create: () -> T): T = entries.getOrPut(frame, create)
    fun contains(value: T): Boolean = entries.values.any { it === value }
    fun canReplace(value: T, replacementBytes: Long): Boolean = contains(value) &&
        replacementBytes <= budgetBytes - entries.values.filter { it !== value }.sumOf(bytes)
    fun insert(frame: Int) {
        entries = entries.mapKeys { (key, _) -> if (key >= frame) key + 1 else key }.toMutableMap()
    }
    fun delete(frame: Int): T? {
        val removed = entries.remove(frame)
        entries = entries.mapKeys { (key, _) -> if (key > frame) key - 1 else key }.toMutableMap()
        return removed
    }
    fun clear(): List<T> = entries.values.toList().also { entries.clear() }
}
