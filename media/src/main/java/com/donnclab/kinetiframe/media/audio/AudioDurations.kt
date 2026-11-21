// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.media.audio

import android.media.MediaMetadataRetriever

/** Reads how long an audio file plays. */
object AudioDurations {
    /** The duration of the audio file at [path] in microseconds, or 0 when it has none. */
    fun readMicros(path: String): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(path)
            val millis = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            millis * 1000L
        } finally {
            retriever.release()
        }
    }
}
