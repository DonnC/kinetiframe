package com.donnclab.kinetiframe.core.coroutines

import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll

/** Tracks accepted work before it starts, and removes it after completion bookkeeping. */
class PendingJobs {
    private val lock = Any()
    private val jobs = mutableSetOf<Job>()

    fun register(job: Job, onCompletion: (Throwable?) -> Unit = {}) {
        synchronized(lock) { jobs.add(job) }
        job.invokeOnCompletion { error ->
            try {
                onCompletion(error)
            } finally {
                synchronized(lock) { jobs.remove(job) }
            }
        }
    }

    suspend fun awaitIdle() {
        while (true) {
            // Kotlin Collection.toList() reads size before creating its iterator. For a
            // concurrently shrinking set, its singleton fast path can call next() on an
            // empty iterator. Protect BOTH size and iteration, not just individual mutations.
            val snapshot = synchronized(lock) { jobs.toList() }
            if (snapshot.isEmpty()) return
            // Never hold the lock while suspending: completion needs it to remove jobs.
            snapshot.joinAll()
        }
    }
}
