package com.donnclab.kinetiframe.domain.actor

/** Error-bounded iterative reduction. The caller supplies angular or positional error;
 * no recursion or pointer-frequency-dependent allocations during capture. */
internal object TimedSampleReduction {
    fun indices(times: List<Long>, tolerance: Float, error: (Int, Int, Int, Float) -> Float): List<Int> {
        if (times.size < 3) return times.indices.toList()
        val retained = mutableSetOf(0, times.lastIndex)
        val pending = ArrayDeque<Pair<Int, Int>>()
        pending.addLast(0 to times.lastIndex)
        while (pending.isNotEmpty()) {
            val (left, right) = pending.removeLast()
            var maximum = tolerance
            var split = -1
            for (i in left + 1 until right) {
                val fraction = (times[i] - times[left]).toFloat() / (times[right] - times[left])
                val distance = error(left, right, i, fraction)
                if (distance > maximum) {
                    maximum = distance
                    split = i
                }
            }
            if (split >= 0) {
                retained.add(split)
                pending.addLast(left to split)
                pending.addLast(split to right)
            }
        }
        return retained.sorted()
    }
}
