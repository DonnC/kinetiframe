package com.donnclab.kinetiframe.preferences.model

import kotlinx.serialization.Serializable

@Serializable enum class AppTheme { SYSTEM, LIGHT, DARK }

@Serializable enum class StudioOrientation { LANDSCAPE, PORTRAIT, DEVICE }

@Serializable data class AppPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    // Follow the phone unless the animator chooses to lock the studio; never force a rotation.
    val orientation: StudioOrientation = StudioOrientation.DEVICE,
    val hideSystemBars: Boolean = false,
    val hideToolsDuringPlayback: Boolean = true,
    val showMenuLabels: Boolean = true,
)
