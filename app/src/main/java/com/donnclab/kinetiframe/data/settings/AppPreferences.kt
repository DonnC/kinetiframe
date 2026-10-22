package com.donnclab.kinetiframe.data.settings

import com.donnclab.kinetiframe.data.storage.DurableFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

@Serializable enum class AppTheme { SYSTEM, LIGHT, DARK }

@Serializable enum class StudioOrientation { LANDSCAPE, PORTRAIT, DEVICE }

@Serializable data class AppPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val orientation: StudioOrientation = StudioOrientation.LANDSCAPE,
    val hideSystemBars: Boolean = false,
    val hideToolsDuringPlayback: Boolean = true,
    val showMenuLabels: Boolean = true,
)

/** Small independent preferences document; never mixed with an animation's export settings. */
class AppPreferencesStore(private val file: File, private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    suspend fun load(): AppPreferences = withContext(ioDispatcher) {
        if (!file.exists()) {
            AppPreferences()
        } else {
            require(file.length() <= 16_384) { "Settings file is too large" }
            json.decodeFromString<AppPreferences>(file.readText())
        }
    }
    suspend fun save(value: AppPreferences) = withContext(ioDispatcher) {
        DurableFile.write(file) { it.write(json.encodeToString(value).toByteArray(Charsets.UTF_8)) }
    }
}
