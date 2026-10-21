// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

internal class BoundedCompilationCache<T : Any>(private val capacity: Int) {
    private val entries = ArrayDeque<T>()

    init {
        if (capacity <= 0) throw CompilationCacheException(CompilationCacheIssue.INVALID_CAPACITY)
    }

    internal inline fun getOrPut(matches: (T) -> Boolean, compile: () -> T): T = synchronized(entries) {
        entries.firstOrNull(matches) ?: compile().also {
            entries.addLast(it)
            if (entries.size > capacity) entries.removeFirst()
        }
    }

    fun clear() = synchronized(entries) { entries.clear() }
}

enum class CompilationCacheIssue { INVALID_CAPACITY }
class CompilationCacheException(val issue: CompilationCacheIssue) : IllegalArgumentException(issue.name)
