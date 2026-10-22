package com.donnclab.kinetiframe.actors.rig.model

/** Authored 2D drawings, not camera rotation or interchangeable joint angles. */
enum class ActorView(val title: String) {
    ORIGINAL("Original"),
    FRONT("Front"),
    PROFILE("Profile"),
    THREE_QUARTER("Three quarter"),
    BACK("Back"),
}
