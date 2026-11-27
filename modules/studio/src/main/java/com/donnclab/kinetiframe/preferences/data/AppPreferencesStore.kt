package com.donnclab.kinetiframe.preferences.data

import com.donnclab.kinetiframe.data.storage.DurableFile
import com.donnclab.kinetiframe.preferences.model.AppPreferences
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

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
