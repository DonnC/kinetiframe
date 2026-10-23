package com.donnclab.kinetiframe.studio.camera

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], manifest = Config.NONE)
class CameraRecipeStoreTest {
    @Test fun namedPresetsSurviveNewStoreInstancesAndReplaceByName() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        context.getSharedPreferences("camera-recipes", 0).edit().clear().commit()
        val recipe = CameraRecipes.builtIn(12).first()
        assertTrue(CameraRecipeStore(context).save(recipe))
        assertEquals(listOf(recipe), CameraRecipeStore(context).load())
        val changed = recipe.copy(shot = recipe.shot.copy(waitFrames = 2))
        assertTrue(CameraRecipeStore(context).save(changed))
        assertEquals(listOf(changed), CameraRecipeStore(context).load())
        assertTrue(CameraRecipeStore(context).remove(recipe.name))
        assertTrue(CameraRecipeStore(context).load().isEmpty())
    }
}
