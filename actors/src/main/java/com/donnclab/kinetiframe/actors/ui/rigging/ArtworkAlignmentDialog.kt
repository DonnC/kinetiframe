package com.donnclab.kinetiframe.actors.ui.rigging

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.coerceAtLeast
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.isFinite
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.width
import com.donnclab.kinetiframe.actors.rig.texture.RigAssetManager
import com.donnclab.kinetiframe.core.actor.artwork.ArtworkAlignment
import com.donnclab.kinetiframe.core.actor.artwork.TextureBinding
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.geometry.Point2
import com.donnclab.kinetiframe.presentation.common.CompactTextField

/** A non-mutating touch audition; Check commits alignment and Cancel preserves the actor. */
@Composable
fun ArtworkAlignmentDialog(
    name: String,
    bitmap: Bitmap,
    boneLength: Float,
    worldAngle: Float,
    onDismiss: () -> Unit,
    onAttach: (String, TextureBinding, Boolean, String?) -> Unit,
    continuousChildren: List<Pair<String, String>> = emptyList(),
    initialAsVariant: Boolean = false,
    targetPartName: String? = null,
    initialBinding: TextureBinding? = null,
    editing: Boolean = false,
    allowAlternate: Boolean = true,
    showName: Boolean = true,
    graph: SceneGraph? = null,
    partId: String? = null,
    assets: RigAssetManager? = null,
    initialContinuousChildId: String? = null,
) {
    var label by remember { mutableStateOf(name.take(60)) }
    var binding by remember {
        mutableStateOf(
            initialBinding ?: TextureBinding(
                "pending",
                Point2(
                    bitmap.width / 2f,
                    bitmap.height / 2f,
                ),
                ArtworkAlignment(
                    Point2(boneLength / 2f, 0f),
                    -worldAngle,
                    (boneLength.coerceAtLeast(24f) * 1.2f / bitmap.width).coerceIn(.001f, 100f),
                ),
            ),
        )
    }
    var variant by remember { mutableStateOf(initialAsVariant) }
    var continuous by remember { mutableStateOf(continuousChildren.any { it.first == initialContinuousChildId }) }
    var child by remember(continuousChildren) {
        mutableStateOf(
            initialContinuousChildId?.takeIf { id -> continuousChildren.any { it.first == id } }
                ?: continuousChildren.firstOrNull()?.first,
        )
    }
    val valid = binding.scale.isFinite() && binding.scale in .001f..100f && binding.rotationDegrees.isFinite() &&
        binding.offsetX.isFinite() && binding.offsetY.isFinite() && label.isNotBlank()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editing) {
                    "Edit artwork"
                } else {
                    targetPartName?.let { "Attach to $it" }
                        ?: "Attach artwork"
                },
                style = MaterialTheme.typography.titleSmall,
            )
        },
        text = {
            Column(
                Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ArtworkPlacementPreview(
                    bitmap,
                    binding,
                    graph,
                    partId,
                    assets,
                    worldAngle,
                    onChange = { binding = it },
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                )
                Text("Drag · pinch to resize · twist to rotate", style = MaterialTheme.typography.labelSmall)
                if (showName) {
                    CompactTextField(label, {
                        label = it.take(60)
                    }, singleLine = true, label = { Text("Name") })
                }
                if (!editing &&
                    allowAlternate
                ) {
                    Row {
                        Checkbox(variant, { variant = it })
                        Text("Alternate artwork", style = MaterialTheme.typography.labelSmall)
                    }
                }
                if (continuousChildren.isNotEmpty()) {
                    Row {
                        Checkbox(continuous, { continuous = it })
                        Text("Continuous limb", style = MaterialTheme.typography.labelSmall)
                    }
                    if (continuous) {
                        Text("One uncut image spanning both bones", style = MaterialTheme.typography.labelSmall)
                        continuousChildren.forEach { (id, title) ->
                            Row {
                                RadioButton(child == id, { child = id })
                                Text(title, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            IconButton(enabled = valid, onClick = {
                onAttach(
                    label,
                    binding,
                    variant,
                    child.takeIf { continuous },
                )
            }) { Icon(Icons.Default.Check, if (editing) "Save artwork changes" else "Confirm artwork attachment") }
        },
        dismissButton = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, "Cancel attachment") } },
    )
}

@Preview
@Composable
private fun ArtworkAlignmentPreview() {
    val bitmap =
        remember {
            Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
        }
    DisposableEffect(bitmap) { onDispose { bitmap.recycle() } }
    MaterialTheme {
        ArtworkAlignmentDialog(
            "Arm",
            bitmap,
            80f,
            90f,
            {},
            { _, _, _, _ -> },
            continuousChildren = listOf("forearm" to "Forearm"),
        )
    }
}
