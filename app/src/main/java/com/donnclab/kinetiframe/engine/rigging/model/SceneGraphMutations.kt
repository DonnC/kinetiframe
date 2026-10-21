package com.donnclab.kinetiframe.engine.rigging.model

import com.donnclab.kinetiframe.engine.rigging.math.RigGraphEdits

/** Copies only the graph map and the targeted node; untouched nodes are shared. */
fun updateBoneRotation(graph: SceneGraph, targetBoneId: String, newRotationDegrees: Float): SceneGraph =
    RigGraphEdits.rotate(graph, targetBoneId, newRotationDegrees)
