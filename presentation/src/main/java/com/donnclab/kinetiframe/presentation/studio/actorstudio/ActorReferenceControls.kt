package com.donnclab.kinetiframe.presentation.studio.actorstudio

import android.graphics.ImageDecoder
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Blend
import com.composables.icons.lucide.Box
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Eye
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.Image
import com.composables.icons.lucide.ImagePlus
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Maximize
import com.composables.icons.lucide.Move
import com.composables.icons.lucide.Spline
import com.composables.icons.lucide.Text
import com.composables.icons.lucide.Trash2
import com.donnclab.kinetiframe.presentation.actors.ReferenceImageSize
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.ContextSliderButton
import com.donnclab.kinetiframe.presentation.common.IconContextMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** One unobtrusive layer entry; controls appear only on demand, never a permanent inspector. */
@Composable
fun ActorReferenceControls(
    session: ActorReferenceSession,
    width: Int,
    height: Int,
    modifier: Modifier = Modifier,
    description: String = "Reference image layer (temporary)",
    onReplace: (android.graphics.Bitmap) -> Boolean = {
        session.replace(it, width, height)
        true
    },
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var expanded by session::menuOpen
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var importGeneration by remember(session) { mutableLongStateOf(session.importGeneration) }
    val latestReplace by rememberUpdatedState(onReplace)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            scope.launch {
                loading = true
                try {
                    val image = withContext(Dispatchers.IO) {
                        // ImageDecoder honors EXIF orientation. Decode bounded pixels, not a camera's
                        // full-resolution image; no URI permission or file is retained after import.
                        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) {
                                decoder,
                                info,
                                _,
                            ->
                            val (targetWidth, targetHeight) = ReferenceImageSize.target(
                                info.size.width,
                                info.size.height,
                                Runtime.getRuntime().maxMemory(),
                            )
                            decoder.setTargetSize(targetWidth, targetHeight)
                            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                            decoder.setTargetColorSpace(
                                android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.SRGB),
                            )
                        }
                    }
                    if (importGeneration == session.importGeneration && !latestReplace(image)) {
                        error = "Reference memory limit reached. Remove another frame's reference and try again."
                    }
                } catch (cancelled: kotlinx.coroutines.CancellationException) {
                    throw cancelled
                } catch (_: Exception) {
                    error = "Could not read this reference image."
                } finally {
                    loading = false
                }
            }
        }
    }
    Box(modifier) {
        Row {
            FilledTonalIconButton(onClick = { expanded = true }, enabled = !loading) {
                if (loading) {
                    CircularProgressIndicator(Modifier.size(20.dp))
                } else {
                    Icon(Lucide.Image, description)
                }
            }
            if (session.editing) {
                FilledTonalIconButton(onClick = { session.editing = false }) {
                    Icon(Lucide.Check, "Finish positioning reference")
                }
            }
        }
        IconContextMenu(
            expanded,
            { expanded = false },
            buildList {
                add(
                    ContextIconAction(
                        Lucide.ImagePlus,
                        if (session.bitmap ==
                            null
                        ) {
                            "Import image"
                        } else {
                            "Replace image"
                        },
                        {
                            expanded = false
                            importGeneration = session.importGeneration
                            picker.launch("image/*")
                        },
                        enabled = !loading,
                    ),
                )
                if (session.bitmap != null) {
                    add(
                        ContextIconAction(if (session.editing) Lucide.Check else Lucide.Move, "Position reference", {
                            session.visible = true
                            session.editing = !session.editing
                            expanded = false
                        }, selected = session.editing),
                    )
                    add(
                        ContextIconAction(if (session.visible) Lucide.Eye else Lucide.EyeOff, "Reference visibility", {
                            session.visible = !session.visible
                            session.editing = false
                        }, selected = session.visible),
                    )
                    add(ContextIconAction(Lucide.Maximize, "Fit to canvas", { session.fit(width, height) }))
                    add(
                        ContextIconAction(
                            Lucide.Lock,
                            "Lock proportions",
                            {
                                session.lockAspectRatio =
                                    !session.lockAspectRatio
                            },
                            selected = session.lockAspectRatio,
                        ),
                    )
                    add(
                        ContextIconAction(
                            Lucide.Spline,
                            "Skew side handles",
                            { session.skewMode = !session.skewMode },
                            selected = session.skewMode,
                        ),
                    )
                    add(
                        ContextIconAction(Lucide.Trash2, "Remove reference", {
                            session.remove()
                            expanded = false
                        }),
                    )
                }
            },
        ) {
            if (session.bitmap != null) {
                ContextSliderButton(
                    Lucide.Blend,
                    "Opacity",
                    session.opacity,
                    { session.opacity = it },
                    valueRange = .1f..1f,
                    onPreview = { session.opacity = it },
                )
            }
        }
    }
    error?.let { message ->
        AlertDialog(onDismissRequest = { error = null }, text = { Text(message) }, confirmButton = {
            TextButton(onClick = {
                error =
                    null
            }) { Text("OK") }
        })
    }
}

@Preview
@Composable
private fun ReferenceControlsPreview() {
    MaterialTheme { ActorReferenceControls(remember { ActorReferenceSession() }, 1920, 1080) }
}
