// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.edit

import com.donnclab.kinetiframe.core.actor.rigging.BoneNode
import com.donnclab.kinetiframe.core.actor.rigging.SceneGraph
import com.donnclab.kinetiframe.core.actor.rigging.Transform

internal object RigEditFixtures {
    val graph = SceneGraph(
        listOf("root", "other"),
        linkedMapOf(
            "root" to BoneNode("root", name = "Root", length = 100f),
            "upper" to BoneNode("upper", "root", "Upper", 80f).withRest(localTransform = Transform(0f, 20f)),
            "lower" to
                BoneNode(
                    "lower",
                    "upper",
                    "Lower",
                    60f,
                ).withRest(localTransform = Transform(80f), ikChainRootId = "upper"),
            "hand" to BoneNode("hand", "lower", "Hand", 15f).withRest(localTransform = Transform(60f)),
            "other" to BoneNode("other", name = "Other", length = 10f).withRest(localTransform = Transform(300f, 50f)),
        ),
    )
}
