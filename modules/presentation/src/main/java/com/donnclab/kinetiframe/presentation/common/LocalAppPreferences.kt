package com.donnclab.kinetiframe.presentation.common

import androidx.compose.runtime.staticCompositionLocalOf
import com.donnclab.kinetiframe.preferences.model.AppPreferences

/** The user's current preferences. MainActivity provides them; screens read them where needed. */
val LocalAppPreferences = staticCompositionLocalOf { AppPreferences() }
