package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import com.donnclab.kinetiframe.core.document.ProjectDocument
import com.donnclab.kinetiframe.core.document.VideoClipPlacement
import com.donnclab.kinetiframe.core.document.VideoFit
import com.donnclab.kinetiframe.video.VideoFrameDecoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun VideoFramingDialog(
    clip: VideoClipPlacement,
    project: ProjectDocument,
    frame: Int,
    onDismiss: () -> Unit,
    onSave: (VideoFit, Float, Float) -> Unit,
) {
    var fit by remember(clip.clipId) { mutableStateOf(clip.fit) }
    var x by remember(clip.clipId) { mutableFloatStateOf(clip.cropX) }
    var y by remember(clip.clipId) { mutableFloatStateOf(clip.cropY) }
    var size by remember { mutableStateOf(IntSize.Zero) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var error by remember { mutableStateOf(false) }
    val previewFrame = remember(clip.clipId) { frame.coerceIn(clip.timelineStartFrame, clip.timelineEndFrame - 1) }
    // Decode only once while opening the dialog, never inside drawing or drag gestures.
    LaunchedEffect(clip.clipId) {
        var decoded: android.graphics.Bitmap? = null
        var published = false
        try {
            withContext(Dispatchers.IO) {
                decoded =
                    VideoFrameDecoder(640).use { decoder ->
                        runCatching { decoder.decode(project, previewFrame) }.getOrNull()
                    }
            }
            bitmap = decoded
            published = true
            error = decoded == null
        } finally {
            if (!published) decoded?.recycle()
        }
    }
    DisposableEffect(Unit) { onDispose { bitmap?.recycle() } }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Video framing") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(clip.width / clip.height).onSizeChanged { size = it }
                    .pointerInput(fit, bitmap, size) {
                        detectDragGestures { change, pan ->
                            change.consume()
                            val image = bitmap ?: return@detectDragGestures
                            if (fit != VideoFit.FILL) return@detectDragGestures
                            val scale = maxOf(size.width.toFloat() / image.width, size.height.toFloat() / image.height)
                            val overflowX = image.width * scale - size.width
                            val overflowY = image.height * scale - size.height
                            if (overflowX > 0) x = (x - pan.x / overflowX).coerceIn(0f, 1f)
                            if (overflowY > 0) y = (y - pan.y / overflowY).coerceIn(0f, 1f)
                        }
                    },
            ) {
                bitmap?.let {
                    Image(
                        it.asImageBitmap(),
                        "Video crop preview",
                        Modifier.fillMaxSize(),
                        alignment = BiasAlignment(x * 2 - 1, y * 2 - 1),
                        contentScale = when (fit) {
                            VideoFit.FIT -> ContentScale.Fit
                            VideoFit.FILL -> ContentScale.Crop
                            VideoFit.STRETCH -> ContentScale.FillBounds
                        },
                    )
                }
                if (error) Text("Preview unavailable. Your source is unchanged.")
            }
            Row {
                VideoFit.entries.forEach { option ->
                    TextButton({ fit = option }) { Text((if (fit == option) "• " else "") + option.name.lowercase()) }
                }
            }
            Text("Fill: drag the picture to choose the crop. Use Transform video to move or resize it on stage.")
        }
    }, confirmButton = {
        TextButton({ onSave(fit, x, y) }) { Text("Apply") }
    }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Preview
@Composable
private fun VideoFramingPreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        VideoFramingDialog(VideoClipPlacement("video", "source", "Video", 0, 24), ProjectDocument(), 0, {
        }, { _, _, _ -> })
    }
}
