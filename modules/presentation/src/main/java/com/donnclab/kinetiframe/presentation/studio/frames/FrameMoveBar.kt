// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.frames

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ArrowRight
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.presentation.R

/** Moves the one selected frame a place earlier or later. [onMove] gets -1 or +1. */
@Composable
fun FrameMoveBar(canMoveEarlier: Boolean, canMoveLater: Boolean, onMove: (Int) -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
        shape = RoundedCornerShape(Dimens.radiusPill),
        tonalElevation = Dimens.elevationFloating,
        border = BorderStroke(Dimens.borderThin, EditorTokens.border.copy(alpha = 0.25f)),
    ) {
        Row(Modifier.padding(horizontal = Dimens.spaceXs)) {
            IconButton(onClick = { onMove(-1) }, enabled = canMoveEarlier) {
                Icon(Lucide.ArrowLeft, stringResource(R.string.frame_studio_move_earlier))
            }
            IconButton(onClick = { onMove(1) }, enabled = canMoveLater) {
                Icon(Lucide.ArrowRight, stringResource(R.string.frame_studio_move_later))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun FrameMoveBarPreview() {
    KinetiFrameTheme { FrameMoveBar(canMoveEarlier = false, canMoveLater = true, onMove = {}) }
}
