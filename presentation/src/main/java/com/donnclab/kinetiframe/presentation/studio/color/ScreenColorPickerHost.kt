package com.donnclab.kinetiframe.presentation.studio.color

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

fun interface ScreenColorRequest {
    fun begin(onSelected: (Color) -> Unit)
}
val LocalScreenColorRequest = staticCompositionLocalOf<ScreenColorRequest?> { null }

/** Above the entire app's content, not trapped inside a small popup or restricted to a raster layer. */
@Composable
fun ScreenColorPickerHost(content: @Composable () -> Unit) {
    val model: ScreenColorPickerViewModel = viewModel()
    val state by model.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = remember(context) { context.activity() }
    val configuration = LocalConfiguration.current
    var selectedCallback by remember { mutableStateOf<((Color) -> Unit)?>(null) }
    val currentCallback by rememberUpdatedState(selectedCallback)
    var origin by remember { mutableStateOf(Offset.Zero) }
    LaunchedEffect(model) {
        model.effect.collect { effect ->
            when (effect) {
                is ScreenColorEffect.Selected -> {
                    currentCallback?.invoke(Color(effect.argb))
                    selectedCallback = null
                }
            }
        }
    }
    LifecycleStartEffect(model) {
        onStopOrDispose {
            model.handleIntent(ScreenColorIntent.Cancel)
            selectedCallback =
                null
        }
    }
    LaunchedEffect(configuration.screenWidthDp, configuration.screenHeightDp) {
        // An old snapshot cannot accurately sample a resized or rotated app window.
        model.handleIntent(ScreenColorIntent.Cancel)
        selectedCallback = null
    }
    BackHandler(state.active || state.capturing || state.error != null) {
        model.handleIntent(ScreenColorIntent.Cancel)
        selectedCallback = null
    }
    val request = remember(activity, model) {
        activity?.let { host ->
            ScreenColorRequest { callback ->
                selectedCallback = callback
                model.handleIntent(ScreenColorIntent.Begin(WindowColorSource(host.window)))
            }
        }
    }
    CompositionLocalProvider(LocalScreenColorRequest provides request) {
        Box(Modifier.fillMaxSize()) {
            content()
            // A native popup is above tool popups too; a canvas-only Box would sit underneath them.
            if (state.active || state.capturing) {
                Popup(
                    alignment = Alignment.TopStart,
                    onDismissRequest = {
                        model.handleIntent(ScreenColorIntent.Cancel)
                        selectedCallback = null
                    },
                    properties = PopupProperties(focusable = true, clippingEnabled = false),
                ) {
                    val popupView = LocalView.current
                    Box(
                        Modifier.fillMaxSize().onGloballyPositioned { coordinates ->
                            val popupLocation = IntArray(2)
                            val appLocation = IntArray(2)
                            popupView.getLocationOnScreen(popupLocation)
                            activity?.window?.decorView?.getLocationOnScreen(appLocation)
                            origin = coordinates.positionInWindow() + Offset(
                                (popupLocation[0] - appLocation[0]).toFloat(),
                                (popupLocation[1] - appLocation[1]).toFloat(),
                            )
                        },
                    ) {
                        if (state.active) {
                            ScreenColorOverlay(state, origin) { intent ->
                                if (intent == ScreenColorIntent.Cancel) selectedCallback = null
                                model.handleIntent(intent)
                            }
                        } else {
                            Box(
                                Modifier.fillMaxSize().pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) awaitPointerEvent().changes.forEach { it.consume() }
                                    }
                                },
                            )
                        }
                    }
                }
            }
        }
        state.error?.let { message ->
            val cancel = {
                model.handleIntent(ScreenColorIntent.Cancel)
                selectedCallback = null
            }
            AlertDialog(
                onDismissRequest = cancel,
                text = { Text(message) },
                confirmButton = { TextButton(onClick = cancel) { Text("Close") } },
            )
        }
    }
}
private fun Context.activity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.takeUnless { it === this }?.activity()
    else -> null
}
