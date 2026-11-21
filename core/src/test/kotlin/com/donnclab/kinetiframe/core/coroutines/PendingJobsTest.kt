// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.coroutines

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicInteger

@OptIn(ExperimentalCoroutinesApi::class)
class PendingJobsTest {
    @Test fun `old size then iterator conversion reproduces the reported exception`() {
        // Deterministically model the set losing its last completed job between size and
        // iterator acquisition. ConcurrentHashMap guarantees neither a stable size nor snapshot.
        val shrinkingSet = object : AbstractSet<Job>() {
            override val size: Int get() = 1
            override fun iterator(): Iterator<Job> = emptyList<Job>().iterator()
        }
        assertThrows(NoSuchElementException::class.java) { shrinkingSet.toList() }
    }

    @Test fun `waiting cannot overtake registered work and includes completion bookkeeping`() = runTest {
        val pending = PendingJobs()
        val job = Job()
        var recorded = false
        pending.register(job) { recorded = true }
        val waiter = async {
            pending.awaitIdle()
            recorded
        }
        runCurrent()
        assertFalse(waiter.isCompleted)
        job.complete()
        assertTrue(waiter.await())
    }

    @Test fun `already completed and cancelled jobs do not remain pending`() = runTest {
        val pending = PendingJobs()
        pending.register(Job().apply { complete() })
        pending.register(Job().apply { cancel() })
        pending.awaitIdle()
    }

    @Test fun `concurrent completion and snapshot waiters cannot throw or lose bookkeeping`() = runBlocking {
        withTimeout(20000) {
            val pending = PendingJobs()
            val completions = AtomicInteger()
            val gate = CompletableDeferred<Unit>()
            val producers = List(4) {
                launch(Dispatchers.Default) {
                    gate.await()
                    repeat(4000) { index ->
                        val job = Job()
                        pending.register(job) { completions.incrementAndGet() }
                        if (index % 2 == 0) yield()
                        if (index % 3 == 0) job.cancel() else job.complete()
                    }
                }
            }
            val readers = List(4) {
                launch(Dispatchers.Default) {
                    gate.await()
                    repeat(4000) {
                        pending.awaitIdle()
                        yield()
                    }
                }
            }
            gate.complete(Unit)
            producers.joinAll()
            readers.joinAll()
            pending.awaitIdle()
            assertEquals(16000, completions.get())
        }
    }
}
