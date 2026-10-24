// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.actor.rigging

/** Authored 2D drawings, not camera rotation or interchangeable joint angles. */
enum class ActorView(val title: String) {
    ORIGINAL("Original"),
    FRONT("Front"),
    PROFILE("Profile"),
    THREE_QUARTER("Three quarter"),
    BACK("Back"),
}
