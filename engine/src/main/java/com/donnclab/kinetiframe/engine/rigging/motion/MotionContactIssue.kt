// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.engine.rigging.motion

enum class MotionContactIssue { MISSING_IK_ROOT }
class MotionContactException(val issue: MotionContactIssue) : IllegalArgumentException(issue.name)
