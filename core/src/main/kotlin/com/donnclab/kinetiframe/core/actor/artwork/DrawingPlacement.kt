// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.artwork

import com.donnclab.kinetiframe.core.geometry.Point2

data class DrawingPlacement(val origin: Point2, val extent: DrawingExtent)
data class DrawingExtent(val width: Float, val height: Float)
