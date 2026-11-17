// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.contracts

/**
 * A failure the person can fix, such as a duplicate name or a file that isn't a project. Screens
 * word [problem] from their own string resources, and [detail] fills in a name or a number. The
 * message is for logs.
 */
class ActionableException(val problem: ActionableProblem, val detail: String? = null) :
    IllegalArgumentException(detail?.let { "${problem.name}: $it" } ?: problem.name)

enum class ActionableProblem {
    PROJECT_NAME_BLANK,

    /** [ActionableException.detail] is the name. */
    PROJECT_NAME_TAKEN,
    ACTOR_NAME_BLANK,
    FILE_UNREADABLE,
    NOT_A_PROJECT_ARCHIVE,
    PROJECT_ALREADY_ON_DEVICE,
    NO_BACKGROUND_IMAGE,
    PACK_EMPTY,

    /** [ActionableException.detail] is the first unnamed frame's number. */
    PACK_FRAME_UNNAMED,
}
