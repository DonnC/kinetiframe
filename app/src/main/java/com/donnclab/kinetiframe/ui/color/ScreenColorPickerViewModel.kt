package com.donnclab.kinetiframe.ui.color

import androidx.lifecycle.viewModelScope
import com.donnclab.kinetiframe.ui.BaseViewModel
import com.donnclab.kinetiframe.ui.ViewEffect
import com.donnclab.kinetiframe.ui.ViewIntent
import com.donnclab.kinetiframe.ui.ViewState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

data class ScreenColorState(
    val capturing: Boolean = false,
    val active: Boolean = false,
    val color: Int? = null,
    val error: String? = null,
) : ViewState
sealed interface ScreenColorIntent : ViewIntent {
    data class Begin(val source: ScreenColorSource) : ScreenColorIntent
    data class Point(val x: Float, val y: Float) : ScreenColorIntent
    data object Confirm : ScreenColorIntent
    data object Cancel : ScreenColorIntent
}
sealed interface ScreenColorEffect : ViewEffect {
    data class Selected(val argb: Int) : ScreenColorEffect
}

class ScreenColorPickerViewModel :
    BaseViewModel<ScreenColorState, ScreenColorIntent, ScreenColorEffect>(ScreenColorState()) {
    private var capture: Job? = null
    private var snapshot: ScreenColorSnapshot? = null
    override fun handleIntent(intent: ScreenColorIntent) {
        when (intent) {
            is ScreenColorIntent.Begin -> {
                stop()
                updateState { ScreenColorState(capturing = true) }
                capture = viewModelScope.launch {
                    try {
                        snapshot = intent.source.capture()
                        updateState { ScreenColorState(active = true) }
                    } catch (
                        cancelled: CancellationException,
                    ) {
                        throw cancelled
                    } catch (
                        _: OutOfMemoryError,
                    ) {
                        fail("Not enough memory to capture the screen.")
                    } catch (
                        _: Exception,
                    ) {
                        fail("Could not sample this screen. Close the picker and try again.")
                    }
                }
            }

            is ScreenColorIntent.Point -> if (currentState.active) {
                snapshot?.let { image ->
                    if (intent.x.isFinite() && intent.y.isFinite()) {
                        updateState {
                            it.copy(color = sampleScreenColor(image, intent.x, intent.y))
                        }
                    }
                }
            }

            ScreenColorIntent.Confirm -> currentState.color?.let {
                stop()
                sendEffect(ScreenColorEffect.Selected(it))
            }

            ScreenColorIntent.Cancel -> stop()
        }
    }
    private fun fail(message: String) {
        snapshot?.close()
        snapshot = null
        updateState { ScreenColorState(error = message) }
    }
    private fun stop() {
        capture?.cancel()
        capture = null
        snapshot?.close()
        snapshot = null
        updateState { ScreenColorState() }
    }
    override fun onCleared() {
        stop()
        super.onCleared()
    }
}
