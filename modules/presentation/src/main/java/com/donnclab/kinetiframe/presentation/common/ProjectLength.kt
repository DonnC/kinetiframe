// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.presentation.R

private const val SECONDS_PER_MINUTE = 60

/**
 * A project's length as "time · N frames", on Home's cards and Export alike. Under a minute the
 * time is seconds to a tenth ("0.4 s"), since a short loop read as "0:00", like an empty project
 * (tablet reviews); longer takes show "m:ss". Cards once switched between a time and a frame count
 * by length, which read as two different measures (R3-36), so both always show.
 */
@Composable
fun projectLength(totalFrames: Int, fps: Int): String {
    val exact = if (fps > 0) totalFrames.toFloat() / fps else 0f
    val seconds = exact.toInt()
    val time = if (seconds < SECONDS_PER_MINUTE) {
        stringResource(R.string.length_seconds, "%.1f".format(exact))
    } else {
        "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE)
    }
    val frames = pluralStringResource(R.plurals.home_card_frames, totalFrames, totalFrames)
    return stringResource(R.string.home_card_length, time, frames)
}
