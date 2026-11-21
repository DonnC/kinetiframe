// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.validation

import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph

object ActorGraphValidation {
    /**
     * Throws [GraphValidationException] naming the first [GraphValidationIssue] in [graph]. Screens
     * word the issue from their own string resources.
     */
    fun validate(graph: SceneGraph) {
        SceneGraphValidation.requireValid(graph)
    }
}
