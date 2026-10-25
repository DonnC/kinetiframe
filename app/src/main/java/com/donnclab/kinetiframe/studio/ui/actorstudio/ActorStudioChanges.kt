package com.donnclab.kinetiframe.studio.ui.actorstudio

import com.donnclab.kinetiframe.studio.StudioState

/** Only authored content is dirty; selection, scrubbing and viewport changes are not edits. */
fun hasActorStudioChanges(initial: StudioState?, current: StudioState): Boolean {
    val definition = initial?.project?.puppetDefinitions?.values?.singleOrNull() ?: return false
    val original = initial.project.puppetInstances.values.firstOrNull { it.definitionId == definition.id }
    val edited = current.project.puppetInstances.values.firstOrNull { it.definitionId == definition.id }
    return current.project.puppetDefinitions[definition.id]?.sceneGraph != definition.sceneGraph ||
        edited?.poseKeys != original?.poseKeys || edited?.artworkKeys != original?.artworkKeys ||
        edited?.motionPreset != original?.motionPreset ||
        edited?.rotationCurves != original?.rotationCurves ||
        edited?.poseInterpolation != original?.poseInterpolation ||
        edited?.rootMotionCurves != original?.rootMotionCurves ||
        current.actor.performanceDurationMicros != initial.actor.performanceDurationMicros
}
