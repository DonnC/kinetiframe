package com.donnclab.kinetiframe.presentation.studio.actorstudio.sketch

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Move
import com.composables.icons.lucide.Pen
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.studio.ActorEditTarget

/** Whole surfaces have no per-cutout stacking or artwork handles. Keep actual joint controls. */
@Composable
fun ActorSketchJointControls(
    onTarget: (ActorEditTarget) -> Unit,
    onSketch: () -> Unit,
    artwork: @Composable () -> Unit = {},
) {
    Surface(shape = MaterialTheme.shapes.large) {
        Row {
            IconButton({
                onTarget(ActorEditTarget.JOINT_START)
            }) { Icon(Lucide.Bone, stringResource(R.string.part_move_joint)) }
            IconButton({
                onTarget(ActorEditTarget.JOINT_END)
            }) { Icon(Lucide.Move, stringResource(R.string.sketch_fit_bone_end)) }
            IconButton(onSketch) { Icon(Lucide.Pen, stringResource(R.string.sketch_edit_drawing)) }
            artwork()
        }
    }
}

@Preview
@Composable
private fun SketchJointPreview() {
    MaterialTheme { ActorSketchJointControls({}, {}) }
}
