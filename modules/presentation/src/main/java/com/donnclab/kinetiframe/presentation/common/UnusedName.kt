// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

/**
 * [base], or [base] followed by the first number that makes it unused, ignoring case and
 * surrounding spaces: "My series", "My series 2"… Used where two things with one name would merge
 * or look the same, such as collections and an actor's performances.
 */
fun unusedName(base: String, taken: Collection<String>): String {
    val trimmed = base.trim()
    val used = taken.mapTo(mutableSetOf()) { it.trim().lowercase() }
    if (trimmed.lowercase() !in used) return trimmed
    return generateSequence(2) { it + 1 }.map { "$trimmed $it" }.first { it.lowercase() !in used }
}
