package com.donnclab.kinetiframe.library.data

import com.donnclab.kinetiframe.data.storage.DurableFile
import com.donnclab.kinetiframe.library.model.AssetOrganization
import com.donnclab.kinetiframe.library.model.BrowserPosition
import com.donnclab.kinetiframe.library.model.LibraryOrganization
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/** Private local organisation. Collections are labels, never file owners or exported metadata.
 * Read-modify-write under a shared lock prevents independent browser instances losing updates.
 * Call on Dispatchers.IO; corruption is reported rather than silently overwriting user data. */
class LibraryOrganizationStore(private val file: File) {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    fun load(): LibraryOrganization = synchronized(lock) {
        if (!file.exists()) {
            LibraryOrganization()
        } else {
            require(file.length() <= 2_000_000) { "Library organisation is too large" }
            json.decodeFromString<LibraryOrganization>(file.readText())
        }
    }
    fun updateItem(
        id: String,
        default: AssetOrganization = AssetOrganization(),
        change: (AssetOrganization) -> AssetOrganization,
    ): LibraryOrganization = update { state ->
        require(id.isNotBlank() && id.length <= 240)
        val item = change(state.items[id] ?: default)
        require(item.collection.isNotBlank() && item.collection.length <= 60)
        require(item.displayName == null || (item.displayName.isNotBlank() && item.displayName.length <= 60))
        state.copy(items = state.items + (id to item))
    }
    fun rememberBrowser(id: String, position: BrowserPosition): LibraryOrganization =
        update { it.copy(browsers = it.browsers + (id to position)) }
    fun removeCollection(name: String): LibraryOrganization = update { state ->
        state.copy(
            items = state.items.mapValues { (_, item) ->
                if (item.collection == name) item.copy(collection = "General Essentials") else item
            },
            browsers = state.browsers.mapValues { (_, position) ->
                if (position.collection == name) position.copy(collection = null) else position
            },
        )
    }
    private fun update(change: (LibraryOrganization) -> LibraryOrganization) = synchronized(lock) {
        val next = change(load())
        val bytes = json.encodeToString(next).toByteArray(Charsets.UTF_8)
        require(bytes.size <= 2_000_000)
        DurableFile.write(file) { it.write(bytes) }
        next
    }
    companion object {
        private val lock = Any()
    }
}
