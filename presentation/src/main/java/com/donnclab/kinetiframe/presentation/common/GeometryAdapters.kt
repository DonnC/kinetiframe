// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.common

import androidx.compose.ui.geometry.Offset
import com.donnclab.kinetiframe.core.geometry.Point2

/** Rig and studio logic work in platform-neutral [Point2]; screens convert at the boundary. */
fun Offset.toPoint2(): Point2 = Point2(x, y)

fun Point2.toOffset(): Offset = Offset(x, y)
