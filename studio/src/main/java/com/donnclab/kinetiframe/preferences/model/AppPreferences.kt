package com.donnclab.kinetiframe.preferences.model

import kotlinx.serialization.Serializable

@Serializable enum class AppTheme { SYSTEM, LIGHT, DARK }

@Serializable enum class StudioOrientation { LANDSCAPE, PORTRAIT, DEVICE }

@Serializable data class AppPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val orientation: StudioOrientation = StudioOrientation.LANDSCAPE,
    val hideSystemBars: Boolean = false,
    val hideToolsDuringPlayback: Boolean = true,
    val showMenuLabels: Boolean = true,
)
