// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.mvi

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class StateStoreTest {
    @Test fun `state reducers and ordered effects share one implementation`() = runTest {
        val store = StateStore<Int, String>(0)
        store.update { it + 1 }
        assertEquals(1, store.state.value)
        assertTrue(store.send("one"))
        assertTrue(store.send("two"))
        assertEquals("one", store.effect.first())
        assertEquals("two", store.effect.first())
        store.close()
        assertFalse(store.send("closed"))
    }
}
