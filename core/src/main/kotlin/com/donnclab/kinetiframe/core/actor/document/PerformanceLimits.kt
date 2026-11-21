// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.document

/** Time is not a memory budget. Short capture transactions can compose a longer performance,
 * while point/channel limits continue to bound decoded documents and undo history.
 */
object PerformanceLimits {
    const val MAX_DURATION_MICROS = 120_000_000L
    const val MAX_TAKE_MICROS = 30_000_000L
}
