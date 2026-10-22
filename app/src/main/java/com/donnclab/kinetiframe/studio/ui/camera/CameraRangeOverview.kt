package com.donnclab.kinetiframe.studio.ui.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.studio.camera.CameraAction
import com.donnclab.kinetiframe.studio.camera.CameraRanges
import com.donnclab.kinetiframe.studio.camera.CameraShot

/** Occupied ranges share the slider's frame scale, with readable labels below it. */
@Composable
internal fun CameraRangeOverview(shot: CameraShot, shots: List<CameraShot>, totalFrames: Int) {
    val occupied = remember(shot.id, shots) { shots.filterNot { it.id == shot.id }.sortedBy { it.startFrame } }
    val collisions = remember(shot, shots) { CameraRanges.conflicts(shot, shots) }
    val occupiedColor = MaterialTheme.colorScheme.secondary.copy(alpha = .45f)
    val selectedColor = if (collisions.isEmpty()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    val summary = occupied.joinToString { "${it.action.label()} ${it.startFrame + 1}–${it.endFrame}" }
    Column {
        Canvas(
            Modifier.fillMaxWidth().height(14.dp).semantics {
                contentDescription =
                    "Selected frames ${shot.startFrame + 1}–${shot.endFrame}. Occupied: ${summary.ifEmpty { "none" }}"
            },
        ) {
            val unit = size.width / totalFrames.coerceAtLeast(1)
            occupied.forEach { range ->
                drawRect(
                    occupiedColor,
                    Offset(range.startFrame * unit, 0f),
                    Size((range.endFrame - range.startFrame) * unit, size.height),
                )
            }
            drawRect(
                selectedColor,
                Offset(shot.startFrame * unit, 4f),
                Size(
                    (shot.endFrame - shot.startFrame) * unit,
                    size.height - 4f,
                ),
            )
        }
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Selected", color = selectedColor, style = MaterialTheme.typography.labelSmall)
            if (occupied.isEmpty()) Text("All other frames available", style = MaterialTheme.typography.labelSmall)
            occupied.forEach { range ->
                Text(
                    "Occupied · ${range.action.label()} ${range.startFrame + 1}–${range.endFrame}",
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
        if (collisions.isNotEmpty()) {
            Text(
                "Range overlaps ${collisions.joinToString {
                    "${it.startFrame + 1}–${it.endFrame}"
                }}. Choose free frames.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.labelSmall,
            )
        }
    }
}

@Preview(widthDp = 400)
@Composable
private fun CameraRangeOverviewPreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        CameraRangeOverview(
            CameraShot("new", CameraAction.PAN, 24, 48),
            listOf(CameraShot("shake", CameraAction.SHAKE, 8, 21)),
            96,
        )
    }
}
