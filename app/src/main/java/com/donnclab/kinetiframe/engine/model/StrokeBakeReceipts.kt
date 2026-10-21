package com.donnclab.kinetiframe.engine.model

/** Transient, bounded completion receipts, including deliberately rejected/no-op edits. */
object StrokeBakeReceipts {
    fun acknowledge(previous: Set<Long>, seed: Long?): Set<Long> =
        if (seed == null) previous else (previous + seed).toList().takeLast(128).toSet()
}
