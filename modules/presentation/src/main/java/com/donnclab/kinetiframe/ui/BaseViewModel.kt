package com.donnclab.kinetiframe.ui

import androidx.lifecycle.ViewModel
import com.donnclab.kinetiframe.core.mvi.StateStore

/**
 * Generic base class to enforce MVI structure.
 */
abstract class BaseViewModel<S : ViewState, I : ViewIntent, E : ViewEffect>(initialState: S) : ViewModel() {

    private val store = StateStore<S, E>(initialState)
    val state = store.state
    val effect = store.effect

    protected val currentState: S
        get() = state.value

    abstract fun handleIntent(intent: I)

    protected fun updateState(reducer: (S) -> S) {
        store.update(reducer)
    }

    protected fun sendEffect(effect: E) {
        store.send(effect)
    }
    override fun onCleared() {
        store.close()
        super.onCleared()
    }
}
