package com.donnclab.kinetiframe.studio.render

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompletedPreviewFramesTest {
    @Test fun `slow cold pose completes then resolves newest tick without starvation or backlog`() = runTest {
        val ticks = MutableStateFlow(0)
        val gate = CompletableDeferred<Unit>()
        val requests = mutableListOf<Int>()
        val visible = mutableListOf<Int>()
        val job = launch {
            ticks.completedPreviewFrames { frame ->
                requests += frame
                if (frame == 0) gate.await()
                frame
            }.collect { (_, result) -> visible += result }
        }
        runCurrent()
        repeat(24) {
            ticks.value = it + 1
            runCurrent()
        }
        assertEquals(listOf(0), requests)
        assertEquals(emptyList<Int>(), visible)
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(0, 24), requests)
        assertEquals(listOf(0, 24), visible)
        job.cancel()
    }
}
