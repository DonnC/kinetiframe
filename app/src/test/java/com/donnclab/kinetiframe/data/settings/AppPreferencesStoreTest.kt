package com.donnclab.kinetiframe.data.settings

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class AppPreferencesStoreTest {
    @TempDir lateinit var root: File

    @Test fun `missing settings use studio friendly defaults`() = runBlocking {
        val settings = AppPreferencesStore(File(root, "settings.json")).load()
        assertEquals(AppTheme.SYSTEM, settings.theme)
        assertEquals(StudioOrientation.LANDSCAPE, settings.orientation)
        assertFalse(settings.hideSystemBars)
        assertTrue(settings.showMenuLabels)
    }

    @Test fun `preferences survive a new store instance`() = runBlocking {
        val file = File(root, "settings.json")
        val chosen = AppPreferences(AppTheme.DARK, StudioOrientation.PORTRAIT, true, false, false)
        AppPreferencesStore(file).save(chosen)
        assertEquals(chosen, AppPreferencesStore(file).load())
        assertEquals(listOf("settings.json"), root.listFiles()!!.map { it.name })
    }

    @Test fun `corrupt settings are not silently overwritten`() = runBlocking {
        val file = File(root, "settings.json")
        file.writeText("broken")
        assertTrue(runCatching { AppPreferencesStore(file).load() }.isFailure)
        assertEquals("broken", file.readText())
    }
}
