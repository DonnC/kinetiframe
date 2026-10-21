package com.donnclab.kinetiframe.ui.actors.sketch

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Move
import com.composables.icons.lucide.Pen
import com.donnclab.kinetiframe.ui.ActorEditTarget

/** Whole surfaces have no per-cutout stacking or artwork handles. Keep actual joint controls. */
@Composable
fun ActorSketchJointControls(
    onTarget: (ActorEditTarget) -> Unit,
    onSketch: () -> Unit,
    artwork: @Composable () -> Unit = {},
) {
    Surface(shape = MaterialTheme.shapes.large) {
        Row {
            IconButton({ onTarget(ActorEditTarget.JOINT_START) }) { Icon(Lucide.Bone, "Move attachment joint") }
            IconButton({ onTarget(ActorEditTarget.JOINT_END) }) { Icon(Lucide.Move, "Fit bone endpoint") }
            IconButton(onSketch) { Icon(Lucide.Pen, "Edit continuous drawing") }
            artwork()
        }
    }
}

@Preview
@Composable
private fun SketchJointPreview() {
    MaterialTheme { ActorSketchJointControls({}, {}) }
}
