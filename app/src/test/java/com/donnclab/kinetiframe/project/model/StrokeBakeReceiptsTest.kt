package com.donnclab.kinetiframe.project.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

class StrokeBakeReceiptsTest {
    @Test fun unrelated_updates_do_not_acknowledge_anything() {
        val previous = setOf(10L, 20L)
        assertSame(previous, StrokeBakeReceipts.acknowledge(previous, null))
    }

    @Test fun out_of_order_and_duplicate_completions_preserve_pending_identity() {
        var receipts = StrokeBakeReceipts.acknowledge(emptySet(), 20)
        receipts = StrokeBakeReceipts.acknowledge(receipts, 10)
        receipts = StrokeBakeReceipts.acknowledge(receipts, 20)
        assertEquals(setOf(10L, 20L), receipts)
        assertFalse(30L in receipts)
    }

    @Test fun receipts_do_not_grow_with_the_project_lifetime() {
        var receipts = emptySet<Long>()
        repeat(10000) { receipts = StrokeBakeReceipts.acknowledge(receipts, it.toLong()) }
        assertEquals((9872L..9999L).toSet(), receipts)
    }
}
