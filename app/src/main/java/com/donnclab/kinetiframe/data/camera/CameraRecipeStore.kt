package com.donnclab.kinetiframe.data.camera

import android.content.Context
import com.donnclab.kinetiframe.engine.camera.CameraRecipe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Small bounded user library, separate from project state and decoded media. */
class CameraRecipeStore(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences("camera-recipes", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    suspend fun load(): List<CameraRecipe> = withContext(Dispatchers.IO) {
        runCatching {
            json.decodeFromString<List<CameraRecipe>>(preferences.getString("recipes", "[]") ?: "[]")
        }.getOrDefault(emptyList()).take(64)
    }
    suspend fun save(recipe: CameraRecipe): Boolean = withContext(Dispatchers.IO) {
        val existing = load().filterNot { it.name == recipe.name }
        if (existing.size >= 64) return@withContext false
        val recipes = existing + recipe
        preferences.edit().putString("recipes", json.encodeToString(recipes)).commit()
    }
    suspend fun remove(name: String): Boolean = withContext(Dispatchers.IO) {
        preferences.edit().putString("recipes", json.encodeToString(load().filterNot { it.name == name })).commit()
    }
}
