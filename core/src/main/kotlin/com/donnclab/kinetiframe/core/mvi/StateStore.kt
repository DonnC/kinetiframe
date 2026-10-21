// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.mvi

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

/** Shared MVI state/effects without Android lifecycle ownership or an application service bag. */
class StateStore<S, E>(initialState: S) {
    private val mutableState = MutableStateFlow(initialState)
    private val effects = Channel<E>(Channel.BUFFERED)
    val state = mutableState.asStateFlow()
    val effect = effects.receiveAsFlow()
    fun update(reducer: (S) -> S) = mutableState.update(reducer)
    fun send(effect: E): Boolean = effects.trySend(effect).isSuccess
    fun close() = effects.close()
}
