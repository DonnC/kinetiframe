package com.donnclab.kinetiframe.studio.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PendingOperationsTest {
    @Test fun `save waits for accepted work even before that work is dispatched`() = runTest {
        val pending = PendingOperations()
        val dispatcher = StandardTestDispatcher(testScheduler)
        var baked = false
        pending.launch(this, dispatcher) {
            delay(20)
            baked = true
        }
        val save = async {
            pending.awaitIdle()
            baked
        }
        runCurrent()
        assertFalse(save.isCompleted)
        assertTrue(save.await())
    }
}
