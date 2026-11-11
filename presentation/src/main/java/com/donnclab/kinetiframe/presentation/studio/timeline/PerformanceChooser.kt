package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.core.actor.document.PerformanceDocument
import com.donnclab.kinetiframe.presentation.R
import kotlin.math.ceil

/** How many timeline frames a performance occupies once placed, at the project's fps. Shared by
 *  the add-actor flow and the inline switch-performance picker so both round the same way. */
fun durationFramesFor(performance: PerformanceDocument, fps: Int): Int =
    ceil(performance.durationMicros * fps / 1_000_000.0).toInt().coerceAtLeast(1)

/**
 * Shared performance-list menu body for a known actor. Used both when placing a brand-new actor
 * on the FxF timeline ([ActorPerformancePicker]) and when switching an already-placed clip's
 * performance inline ([TimelineTracksPanel]) -- neither path needs a trip into Actor Studio
 * unless the user actually wants to add or edit a performance's content.
 */
@Composable
fun PerformanceChooserItems(
    performances: List<PerformanceDocument>,
    excludePerformanceId: String? = null,
    onCreatePerformance: (() -> Unit)?,
    onChoose: (PerformanceDocument) -> Unit,
) {
    val choices = if (excludePerformanceId ==
        null
    ) {
        performances
    } else {
        performances.filter { it.performanceId != excludePerformanceId }
    }
    if (choices.isEmpty()) {
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        if (performances.isEmpty()) R.string.performances_none else R.string.performances_no_others,
                    ),
                )
            },
            enabled = false,
            onClick = {},
        )
    } else {
        choices.forEach { performance ->
            DropdownMenuItem(text = { Text(performance.displayName) }, onClick = { onChoose(performance) })
        }
    }
    HorizontalDivider()
    DropdownMenuItem(
        text = { Text(stringResource(R.string.performances_new)) },
        enabled = onCreatePerformance != null,
        onClick = { onCreatePerformance?.invoke() },
    )
}
