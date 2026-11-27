// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

data class DrawingComposition(
    val enabled: Boolean = true,
    val overlayParts: Set<String> = emptySet(),
    val underlayParts: Set<String> = emptySet(),
)
