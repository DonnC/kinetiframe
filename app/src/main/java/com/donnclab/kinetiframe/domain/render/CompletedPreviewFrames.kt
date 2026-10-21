package com.donnclab.kinetiframe.domain.render

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map

/** Finish one preview before taking the newest tick; neither cancel every pose nor queue all ticks. */
fun <T, R> Flow<T>.completedPreviewFrames(resolve: suspend (T) -> R): Flow<Pair<T, R>> =
    conflate().map { request -> request to resolve(request) }
