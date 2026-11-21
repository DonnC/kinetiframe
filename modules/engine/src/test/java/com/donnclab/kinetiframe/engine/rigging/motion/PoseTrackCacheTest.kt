// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

import com.donnclab.kinetiframe.core.actor.motion.ActorPoseKey
import com.donnclab.kinetiframe.engine.rigging.edit.RigEditFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PoseTrackCacheTest {
    private val rest = RigEditFixtures.graph
    private val keys = listOf(ActorPoseKey(0, mapOf("upper" to 20f)), ActorPoseKey(1000, mapOf("upper" to 40f)))

    @Test fun `identity and content hits reuse compiled tracks without refreshing FIFO order`() {
        val cache = PoseTrackCache(2)
        val first = cache.compiled(keys, rest)
        assertSame(first, cache.compiled(keys.toList(), rest.copy(bones = rest.bones.toMap())))
        cache.compiled(keys, rest, true)
        assertSame(first, cache.compiled(keys, rest))
        cache.compiled(keys.map { it.copy(rotations = mapOf("upper" to 10f)) }, rest)
        assertNotSame(first, cache.compiled(keys, rest))
    }

    @Test fun `loop keys and rest revisions are part of identity and clear releases reuse`() {
        val cache = PoseTrackCache()
        val first = cache.compiled(keys, rest)
        assertNotSame(first, cache.compiled(keys, rest, true))
        assertNotSame(first, cache.compiled(keys, rest.copy(rootBoneIds = listOf("root"))))
        cache.clear()
        assertNotSame(first, cache.compiled(keys, rest))
    }

    @Test fun `cache ownership is isolated and capacity failures are typed`() {
        assertNotSame(PoseTrackCache().compiled(keys, rest), PoseTrackCache().compiled(keys, rest))
        listOf(0, -1).forEach { capacity ->
            assertEquals(
                PoseCacheIssue.INVALID_CAPACITY,
                assertThrows(PoseCacheException::class.java) {
                    PoseTrackCache(capacity)
                }.issue,
            )
        }
    }

    @Test fun `default cache retains at most four tracks`() {
        val cache = PoseTrackCache()
        val first = cache.compiled(keys, rest)
        repeat(4) { value -> cache.compiled(keys.map { it.copy(rotations = mapOf("upper" to value.toFloat())) }, rest) }
        assertNotSame(first, cache.compiled(keys, rest))
    }
}
