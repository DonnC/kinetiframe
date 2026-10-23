package com.donnclab.kinetiframe.settings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @TempDir lateinit var root: File
    private val dispatcher = StandardTestDispatcher()

    @BeforeEach fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterEach fun teardown() {
        Dispatchers.resetMain()
    }

    @Test fun `rapid choices preserve all preferences across reopening`() = runTest(dispatcher) {
        val file = File(root, "settings.json")
        val model = SettingsViewModel(AppPreferencesStore(file, dispatcher))
        model.state.first { !it.loading }
        model.handleIntent(SettingsIntent.Theme(AppTheme.DARK))
        model.handleIntent(SettingsIntent.SystemBars(true))
        model.handleIntent(SettingsIntent.Orientation(StudioOrientation.DEVICE))
        model.state.first { it.preferences.orientation == StudioOrientation.DEVICE }
        val saved = AppPreferencesStore(file, dispatcher).load()
        assertEquals(AppTheme.DARK, saved.theme)
        assertTrue(saved.hideSystemBars)
        assertEquals(StudioOrientation.DEVICE, saved.orientation)
    }
}
