package com.donnclab.kinetiframe.studio.camera

import android.content.Context
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
        }.getOrDefault(emptyList()).take(MAX_RECIPES)
    }
    suspend fun save(recipe: CameraRecipe): Boolean = withContext(Dispatchers.IO) {
        val existing = load().filterNot { it.name == recipe.name }
        if (existing.size >= MAX_RECIPES) return@withContext false
        val recipes = existing + recipe
        preferences.edit().putString("recipes", json.encodeToString(recipes)).commit()
    }
    suspend fun remove(name: String): Boolean = withContext(Dispatchers.IO) {
        preferences.edit().putString("recipes", json.encodeToString(load().filterNot { it.name == name })).commit()
    }

    companion object {
        /** The most presets an animator can save. */
        const val MAX_RECIPES = 64
    }
}
