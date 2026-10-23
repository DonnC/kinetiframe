// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.donnclab.kinetiframe.studio.StudioIntent
import com.donnclab.kinetiframe.studio.StudioViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/** System pickers for the images, audio and video the animator brings into the editor. */
internal class EditorMediaPickers(
    private val context: Context,
    private val background: ManagedActivityResultLauncher<String, Uri?>,
    private val frameImage: ManagedActivityResultLauncher<String, Uri?>,
    private val audio: ManagedActivityResultLauncher<String, Uri?>,
    private val video: ManagedActivityResultLauncher<String, Uri?>,
    private val recordPermission: ManagedActivityResultLauncher<String, Boolean>,
    private val onRecord: () -> Unit,
) {
    fun pickBackground() = background.launch("image/*")

    fun pickFrameImage() = frameImage.launch("image/*")

    fun importAudio() = audio.launch("audio/*")

    fun importVideo() = video.launch("video/*")

    fun recordAudio() {
        val permission = Manifest.permission.RECORD_AUDIO
        val granted = ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        if (granted) onRecord() else recordPermission.launch(permission)
    }
}

/**
 * The pickers return content:// URIs. Each one is read off the main thread here, where the
 * Context is available, and the intent carries the decoded result: the managers never touch a
 * Context, Uri or ContentResolver.
 */
@Composable
internal fun rememberEditorMediaPickers(
    viewModel: StudioViewModel,
    media: EditorMediaImporter,
    ui: EditorUiState,
    scope: CoroutineScope,
): EditorMediaPickers {
    val messages = ui.messages
    val background = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bitmap = media.decodeImage(uri)
            if (bitmap != null) {
                viewModel.handleIntent(StudioIntent.Background.SetImage(bitmap))
            } else {
                messages.backgroundImageError = "Could not read that image. Try picking a different one."
            }
        }
    }
    val frameImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val bitmap = media.decodeFrameImage(uri)
                if (bitmap == null) {
                    messages.backgroundImageError = "Could not read that image. Try another file."
                } else {
                    viewModel.handleIntent(StudioIntent.Selection.StampBitmap(bitmap))
                }
            }
        }
    }
    val audio = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            media.importAudio(uri).fold(
                onSuccess = { imported ->
                    // Decoding the waveform is slow, so the clip is placed once it is ready.
                    scope.launch {
                        val peaks = media.waveformPeaks(imported)
                        viewModel.handleIntent(
                            viewModel.state.value.addAudioClip(
                                imported.assetId,
                                imported.file.absolutePath,
                                imported.durationMicros,
                                "Imported audio",
                                peaks,
                            ),
                        )
                    }
                },
                onFailure = { messages.audioError = it.message ?: "Could not import that audio file." },
            )
        }
    }
    val video = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            media.importVideo(uri).fold(
                onSuccess = { imported ->
                    val add = viewModel.state.value.addVideoClip(imported)
                    viewModel.handleIntent(add)
                    if (imported.metadata.hasAudio && add.sourceAudio == null) {
                        messages.videoError = "Video added without sound because all audio lanes are locked. " +
                            "Unlock a lane and import again to include sound."
                    }
                    viewModel.handleIntent(StudioIntent.Video.Select(add.placement.clipId))
                },
                onFailure = { messages.videoError = it.message ?: "Could not import that video file." },
            )
        }
    }
    val recordPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            ui.panels.showRecordAudioDialog = true
        } else {
            messages.audioError = "Microphone permission is needed to record audio."
        }
    }
    return EditorMediaPickers(LocalContext.current, background, frameImage, audio, video, recordPermission) {
        ui.panels.showRecordAudioDialog = true
    }
}
