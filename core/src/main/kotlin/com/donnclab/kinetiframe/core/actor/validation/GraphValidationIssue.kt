// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.validation

enum class GraphValidationIssue {
    PART_COUNT,
    ROOTS,
    PART_IDENTITY,
    VARIANT_COUNT,
    VARIANT_IDENTITY,
    ARTWORK_ADDRESS,
    ARTWORK_ALIGNMENT,
    ATTACHMENT_PAIR,
    SKIN_GRID,
    SKIN_BONES,
    SKIN_BIND_MATRIX,
    SINGULAR_BIND,
    SKIN_WEIGHTS,
    PART_LENGTH,
    PART_TRANSFORM,
    UNKNOWN_PARENT,
    IK_ROOT,
    HIERARCHY_CYCLE,
    GRID_BUDGET,
    DRAWING_SOURCES,
    DRAWING_ADDRESS,
    DRAWING_PLACEMENT,
    DRAWING_OVERLAYS,
    DRAWING_UNDERLAYS,
    DRAWING_CORRECTIONS,
    DRAWING_GRID,
    DRAWING_BONES,
    DRAWING_BIND_MATRIX,
    DRAWING_WEIGHTS,
    SURFACE_DESCENDANTS,
    SURFACE_OVERLAP,
}

class GraphValidationException(val issue: GraphValidationIssue, cause: Throwable? = null) :
    IllegalArgumentException(issue.name, cause)

internal fun requireGraph(condition: Boolean, issue: GraphValidationIssue) {
    if (!condition) throw GraphValidationException(issue)
}
