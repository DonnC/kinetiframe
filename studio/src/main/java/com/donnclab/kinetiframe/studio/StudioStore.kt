package com.donnclab.kinetiframe.studio

import kotlinx.coroutines.CoroutineScope
import java.io.File

/** The studio ViewModel's state, updates and effects, as seen by its session and intent handlers. */
class StudioStore(
    val scope: CoroutineScope,
    private val read: () -> StudioState,
    private val write: ((StudioState) -> StudioState) -> Unit,
    private val emit: (StudioEffect) -> Unit,
) {
    val state: StudioState get() = read()

    fun update(reducer: (StudioState) -> StudioState) = write(reducer)

    fun send(effect: StudioEffect) = emit(effect)
}

/**
 * Where a studio keeps its files. [files] holds durable project assets such as the background
 * image, because the OS may clear [cache] at any time. [diagnostics] is kept apart so crash reports
 * never end up in exports.
 */
data class StudioDirectories(val cache: File, val files: File, val diagnostics: File)
