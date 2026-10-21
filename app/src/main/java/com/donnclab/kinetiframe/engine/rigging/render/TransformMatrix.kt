package com.donnclab.kinetiframe.engine.rigging.render

import android.graphics.Matrix
import com.donnclab.kinetiframe.engine.rigging.model.Transform

/** Builds the affine matrix used for scene placement without retaining Android UI state. */
fun Transform.toMatrix(): Matrix = Matrix().apply {
    postScale(scaleX, scaleY)
    postRotate(rotationDegrees)
    postTranslate(x, y)
}
