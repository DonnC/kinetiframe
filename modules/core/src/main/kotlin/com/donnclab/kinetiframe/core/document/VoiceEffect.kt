// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.core.document

import kotlinx.serialization.Serializable

/** The voice treatment a project applies to an audio clip. */
@Serializable
enum class VoiceEffect { ORIGINAL, WARM, RADIO, ROBOT }
