package com.donnclab.kinetiframe.studio.ui.timeline

import com.donnclab.kinetiframe.actors.domain.PerformanceChoices
import com.donnclab.kinetiframe.studio.media.VoiceStyles

/** What the timeline's clip actions need beyond the editor's state: performances and voice styles. */
data class TimelineServices(val performances: PerformanceChoices, val voiceStyles: VoiceStyles)
