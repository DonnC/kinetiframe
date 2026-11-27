// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.studio.camera

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.core.document.camera.CameraAction
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/**
 * The camera moves as tiles: each draws and loops what it does (push in, pan, shake…) above its
 * name. A plain list of names didn't say what each move would look like.
 */
@Composable
fun CameraActionGrid(enabled: (CameraAction) -> Boolean, onChoose: (CameraAction) -> Unit) {
    FlowRow(
        Modifier.widthIn(max = EditorTokens.cameraGridWidth).padding(Dimens.spaceS),
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceS),
    ) {
        CameraAction.entries.forEach { action ->
            val on = enabled(action)
            Surface(
                onClick = { onChoose(action) },
                enabled = on,
                shape = RoundedCornerShape(Dimens.radiusM),
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    Modifier.width(EditorTokens.cameraTileWidth).padding(Dimens.spaceS),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs),
                ) {
                    val tint = if (on) EditorTokens.dark else EditorTokens.inactive
                    CameraActionIcon(action, tint, playing = on, modifier = Modifier.size(EditorTokens.cameraIcon))
                    Text(
                        action.label(),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraActionGridPreview() {
    KinetiFrameTheme { CameraActionGrid(enabled = { it != CameraAction.FOLLOW }, onChoose = {}) }
}
