// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.actors

import androidx.annotation.StringRes
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.rigging.ActorView
import com.donnclab.kinetiframe.presentation.R

@StringRes
internal fun ActorView.labelRes(): Int = when (this) {
    ActorView.ORIGINAL -> R.string.actor_view_original
    ActorView.FRONT -> R.string.actor_view_front
    ActorView.PROFILE -> R.string.actor_view_profile
    ActorView.THREE_QUARTER -> R.string.actor_view_three_quarter
    ActorView.BACK -> R.string.actor_view_back
}

@StringRes
internal fun ActorTemplate.labelRes(): Int = when (this) {
    ActorTemplate.HUMAN -> R.string.actor_template_two_legs
    ActorTemplate.ANIMAL -> R.string.actor_template_four_supports
    ActorTemplate.CUSTOM -> R.string.actor_template_empty_root
}

/** The template a saved actor was built from, or null for an ID this version doesn't know. */
@StringRes
internal fun actorTemplateLabelRes(templateId: String): Int? = when (templateId) {
    ActorManifest.HUMAN_TEMPLATE_ID -> R.string.actor_template_two_legs
    ActorManifest.ANIMAL_TEMPLATE_ID -> R.string.actor_template_four_supports
    ActorManifest.CUSTOM_TEMPLATE_ID -> R.string.actor_template_empty_root
    else -> null
}
