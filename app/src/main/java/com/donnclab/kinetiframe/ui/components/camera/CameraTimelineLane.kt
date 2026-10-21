package com.donnclab.kinetiframe.ui.components.camera

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.Box
import com.composables.icons.lucide.Camera
import com.composables.icons.lucide.List
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Trash2
import com.donnclab.kinetiframe.engine.camera.CameraShot
import com.donnclab.kinetiframe.ui.TimelineIntent
import com.donnclab.kinetiframe.ui.components.common.ContextIconAction
import com.donnclab.kinetiframe.ui.components.common.IconContextMenu
import com.donnclab.kinetiframe.ui.components.timeline.TRACK_HEIGHT
import com.donnclab.kinetiframe.ui.components.timeline.TimelineAssetClip

@Composable
internal fun CameraTimelineLane(
    shots: List<CameraShot>,
    totalFrames: Int,
    offset: Float,
    visible: Float,
    onIntent: (TimelineIntent) -> Unit,
) {
    var selected by remember { mutableStateOf<String?>(null) }
    var menu by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().height(TRACK_HEIGHT)) {
        shots.forEach { shot ->
            key(shot.id) {
                TimelineAssetClip(
                    shot.id, shot.action.label(), shot.startFrame, shot.endFrame - shot.startFrame,
                    selected == shot.id, onSelect = {
                        selected = shot.id
                        menu = true
                    },
                    onMove = { start, _ ->
                        val duration = shot.endFrame - shot.startFrame
                        val from = start.coerceIn(0, (totalFrames - duration).coerceAtLeast(0))
                        onIntent(TimelineIntent.Camera.Put(shot.copy(startFrame = from, endFrame = from + duration)))
                    }, onTrim = { start, duration ->
                        onIntent(
                            TimelineIntent.Camera.Put(
                                shot.copy(startFrame = start, endFrame = minOf(totalFrames, start + duration)),
                            ),
                        )
                    },
                    minStart = 0, maxEnd = totalFrames, offsetFrames = offset, visibleFrames = visible,
                    contextMenu = {
                        IconContextMenu(
                            menu,
                            { menu = false },
                            listOf(
                                ContextIconAction(Lucide.Play, "Preview from start", {
                                    menu = false
                                    onIntent(TimelineIntent.Playback.SetCurrentFrame(shot.startFrame))
                                }),
                                ContextIconAction(Lucide.Trash2, "Remove camera effect", {
                                    menu = false
                                    onIntent(TimelineIntent.Camera.Remove(shot.id))
                                }),
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Preview(widthDp = 400, heightDp = 48)
@Composable
private fun CameraTimelinePreview() {
    com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme {
        CameraTimelineLane(
            listOf(CameraShot("preview", com.donnclab.kinetiframe.engine.camera.CameraAction.PAN, 0, 10)),
            24,
            0f,
            10f,
            {},
        )
    }
}
