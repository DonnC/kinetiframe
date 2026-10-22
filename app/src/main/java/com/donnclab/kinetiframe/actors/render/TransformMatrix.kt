package com.donnclab.kinetiframe.actors.render

import android.graphics.Matrix
import com.donnclab.kinetiframe.actors.rig.model.Transform

/** Builds the affine matrix used for scene placement without retaining Android UI state. */
fun Transform.toMatrix(): Matrix = Matrix().apply {
    postScale(scaleX, scaleY)
    postRotate(rotationDegrees)
    postTranslate(x, y)
}
