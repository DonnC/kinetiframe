// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.studio.editor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.donnclab.kinetiframe.media.audio.WaveformExtractor
import com.donnclab.kinetiframe.media.image.FrameImageDecoder
import com.donnclab.kinetiframe.media.video.ImportedVideoMetadata
import com.donnclab.kinetiframe.presentation.media.audio.PendingAudioTake
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.util.UUID

internal data class ImportedAudio(val assetId: String, val file: File, val durationMicros: Long)

internal data class ImportedVideo(val assetId: String, val file: File, val metadata: ImportedVideoMetadata)

/**
 * Copies picked media into the project's own `project-assets/` directory and reads what the
 * timeline needs from it. Every call does its file work on [ioDispatcher].
 */
internal class EditorMediaImporter(
    private val context: Context,
    private val filesDir: File,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    val audioDirectory: File get() = File(filesDir, "project-assets/audio").apply { mkdirs() }

    suspend fun decodeImage(uri: Uri): Bitmap? = withContext(ioDispatcher) {
        runCatching {
            context.contentResolver.openInputStream(uri)?.use { stream -> BitmapFactory.decodeStream(stream) }
        }.getOrNull()
    }

    suspend fun decodeFrameImage(uri: Uri): Bitmap? = withContext(ioDispatcher) {
        runCatching { FrameImageDecoder.decode { context.contentResolver.openInputStream(uri) } }.getOrNull()
    }

    suspend fun importAudio(uri: Uri): Result<ImportedAudio> = withContext(ioDispatcher) {
        runCatching {
            val assetId = "audio-${UUID.randomUUID()}"
            val destination = File(audioDirectory, "$assetId.audio")
            copy(uri, destination, "Could not read that audio file")
            ImportedAudio(assetId, destination, readAudioDurationMicros(destination.absolutePath))
        }
    }

    suspend fun waveformPeaks(audio: ImportedAudio): List<Float> = withContext(ioDispatcher) {
        WaveformExtractor.fromFile(audio.file.absolutePath, audio.durationMicros).peaks
    }

    /** Moves a recorded take out of the cache into the project's audio assets. */
    suspend fun keepTake(take: PendingAudioTake): Result<Pair<String, File>> = withContext(ioDispatcher) {
        runCatching {
            val assetId = "audio-${UUID.randomUUID()}"
            val destination = File(audioDirectory, "$assetId.m4a")
            take.file.copyTo(destination, overwrite = true)
            take.file.delete()
            assetId to destination
        }
    }

    /** Videos are stored whole, never split into frames. */
    suspend fun importVideo(uri: Uri): Result<ImportedVideo> = withContext(ioDispatcher) {
        runCatching {
            val projectVideoDir = File(filesDir, "project-assets/video").apply { mkdirs() }
            val assetId = "video-${UUID.randomUUID()}"
            val destination = File(projectVideoDir, "$assetId.video")
            copy(uri, destination, "Could not read that video file")
            ImportedVideo(assetId, destination, readVideoMetadata(destination))
        }
    }

    /** Reads an audio file's duration from its path, with no ContentResolver involved. */
    fun readAudioDurationMicros(path: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val millis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            millis * 1000L
        } finally {
            retriever.release()
        }
    }

    private fun copy(uri: Uri, destination: File, failure: String) {
        context.contentResolver.openInputStream(uri)?.use { input ->
            destination.outputStream().use { output -> input.copyTo(output) }
        } ?: throw IOException(failure)
    }

    private fun readVideoMetadata(file: File): ImportedVideoMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(file.absolutePath)
            fun read(key: Int) = retriever.extractMetadata(key)
            val duration = (read(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L) * 1000L
            val width = read(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull() ?: 0
            val height = read(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull() ?: 0
            val rotation = read(MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
            val hasAudio = read(MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO) == "yes"
            require(duration > 0L && width > 0 && height > 0) { "Could not read video duration or dimensions" }
            val sideways = rotation % 180 != 0
            ImportedVideoMetadata(
                durationMicros = duration,
                width = if (sideways) height else width,
                height = if (sideways) width else height,
                hasAudio = hasAudio,
                peaks = if (hasAudio) WaveformExtractor.fromFile(file.absolutePath, duration).peaks else emptyList(),
            )
        } finally {
            retriever.release()
        }
    }
}
