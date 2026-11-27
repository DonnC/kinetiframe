// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.timeline

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R

/** The frames a hold covers while it is being picked: from [source], the drawing, to [other]. */
data class HoldRangePick(val source: Int, val other: Int) {
    val range: IntRange get() = minOf(source, other)..maxOf(source, other)
}

/**
 * Shown while picking a hold on the timeline (R3-24): tap frames on either side of the drawing to
 * set the range, then confirm. It replaced a dialog that only held forward and wanted a typed
 * frame number.
 */
@Composable
fun HoldRangeBar(pick: HoldRangePick, onConfirm: () -> Unit, onCancel: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(Dimens.radiusL),
        tonalElevation = Dimens.elevationFloating,
        border = BorderStroke(Dimens.borderThin, EditorTokens.border.copy(alpha = 0.25f)),
    ) {
        Row(Modifier.padding(start = Dimens.spaceM), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.padding(vertical = Dimens.spaceXs)) {
                Text(
                    stringResource(R.string.hold_range_title, pick.range.first + 1, pick.range.last + 1),
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    stringResource(R.string.hold_range_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onConfirm, enabled = pick.range.first != pick.range.last) {
                Icon(Icons.Default.Check, stringResource(R.string.hold_drawing_confirm))
            }
            IconButton(onClick = onCancel) { Icon(Icons.Default.Close, stringResource(R.string.action_cancel)) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HoldRangeBarPreview() {
    KinetiFrameTheme { HoldRangeBar(HoldRangePick(source = 4, other = 1), {}, {}) }
}
