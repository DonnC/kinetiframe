package com.donnclab.kinetiframe.data

import android.content.res.AssetManager
import com.donnclab.kinetiframe.engine.rigging.presets.ActorMotionPreset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

/** View-specific motion recipes and their paired vector icons are discovered without registration. */
class ActorPresetRepository(private val assets: AssetManager) {
    suspend fun load(): List<ActorMotionPreset> = withContext(Dispatchers.IO) {
        val json = Json { ignoreUnknownKeys = false }
        val presets = assets.jsonFiles(DIRECTORY).sorted().map { path ->
            assets.open(path).bufferedReader().use { reader ->
                val recipe = json.decodeFromString<ActorMotionPreset>(reader.readText()).validate()
                val preview = ActorPresetAssets.previewPath(
                    path,
                    assets.list(path.substringBeforeLast('/')).orEmpty().toList(),
                )
                recipe.copy(previewAssetPath = preview)
            }
        }
        require(presets.map { it.id }.distinct().size == presets.size) { "Duplicate actor preset ID" }
        presets
    }

    /** Assets may be grouped by rig family; folders are not a second preset format. */
    private fun AssetManager.jsonFiles(path: String): List<String> = list(path).orEmpty().flatMap { name ->
        val child = "$path/$name"
        val children = list(child).orEmpty()
        when {
            children.isNotEmpty() -> jsonFiles(child)
            name.endsWith(".json", ignoreCase = true) -> listOf(child)
            else -> emptyList()
        }
    }

    companion object {
        const val DIRECTORY = "actor/presets/views"
    }
}
