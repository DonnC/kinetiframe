package com.donnclab.kinetiframe.engine.rigging.math

import com.donnclab.kinetiframe.engine.rigging.edit.PoseEditException
import com.donnclab.kinetiframe.engine.rigging.motion.MotionContactException

internal inline fun <T> withLegacyRigDiagnostics(operation: () -> T): T = try {
    operation()
} catch (failure: RigidGeometryException) {
    throw IllegalStateException(CYCLE_FAILURE)
} catch (failure: TwoBoneException) {
    throw IllegalArgumentException(REQUIREMENT_FAILURE)
} catch (failure: PoseEditException) {
    throw IllegalArgumentException(REQUIREMENT_FAILURE)
} catch (failure: MotionContactException) {
    throw IllegalArgumentException(MISSING_VALUE_FAILURE)
}

private const val CYCLE_FAILURE = "Cyclic skeleton"
private const val REQUIREMENT_FAILURE = "Failed requirement."
private const val MISSING_VALUE_FAILURE = "Required value was null."
