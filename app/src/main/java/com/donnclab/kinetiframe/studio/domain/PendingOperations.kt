package com.donnclab.kinetiframe.studio.domain

import com.donnclab.kinetiframe.core.coroutines.PendingJobs
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Registers work before scheduling it, so Save cannot overtake an accepted editor operation. */
class PendingOperations {
    private val jobs = PendingJobs()

    @Volatile private var failure: Throwable? = null

    fun launch(scope: CoroutineScope, dispatcher: CoroutineDispatcher, block: suspend CoroutineScope.() -> Unit): Job {
        val job = scope.launch(dispatcher, start = CoroutineStart.LAZY, block = block)
        jobs.register(job) { error ->
            if (error != null && error !is kotlinx.coroutines.CancellationException) failure = error
        }
        job.start()
        return job
    }

    suspend fun awaitIdle() {
        jobs.awaitIdle()
        failure?.let { throw IllegalStateException("An editor operation failed; save was not committed", it) }
    }
}
