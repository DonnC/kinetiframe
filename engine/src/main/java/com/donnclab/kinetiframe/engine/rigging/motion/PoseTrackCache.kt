// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

/** Caller owns this bounded cache and supplies immutable revision snapshots. Hits preserve FIFO order. */
class PoseTrackCache(private val capacity: Int = DEFAULT_CAPACITY) {
    private data class Source(val keys: List<ActorPoseKey>, val rest: SceneGraph, val loop: Boolean)
    private data class Entry(val source: Source, val compiled: CompiledPoseTrack)
    init {
        if (capacity <= 0) throw PoseCacheException(PoseCacheIssue.INVALID_CAPACITY)
    }

    private val entries = BoundedCompilationCache<Entry>(capacity)

    fun compiled(keys: List<ActorPoseKey>, rest: SceneGraph, loop: Boolean = false): CompiledPoseTrack =
        entries.getOrPut({
            val source = it.source
            source.loop == loop && (source.keys === keys || source.keys == keys) &&
                (source.rest === rest || source.rest == rest)
        }) { Entry(Source(keys, rest, loop), CompiledPoseTrack(keys, rest, loop)) }.compiled

    fun clear() = entries.clear()

    companion object {
        private const val DEFAULT_CAPACITY = 4
    }
}

enum class PoseCacheIssue { INVALID_CAPACITY }
class PoseCacheException(val issue: PoseCacheIssue) : IllegalArgumentException(issue.name)
