package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.media.audio.Waveform
import com.donnclab.kinetiframe.presentation.media.audio.AudioClipWaveform
import kotlin.math.roundToInt

internal val FRAME_CELL_WIDTH = 40.dp
internal val TRACK_GUTTER = 96.dp
internal val TRACK_HEIGHT = 48.dp

/** Local previews only. A cancelled gesture never writes the project; release writes once. */
@Composable
internal fun TimelineAssetClip(
    id: String,
    label: String,
    start: Int,
    duration: Int,
    selected: Boolean,
    locked: Boolean = false,
    peaks: List<Float> = emptyList(),
    muted: Boolean = false,
    track: Int = 0,
    trackCount: Int = 1,
    onSelect: () -> Unit,
    onMove: (Int, Int) -> Unit,
    onTrim: (Int, Int) -> Unit,
    allowLeftTrim: Boolean = true,
    allowTrim: Boolean = true,
    minStart: Int = start,
    maxEnd: Int = start + duration,
    offsetFrames: Float = 0f,
    visibleFrames: Float = 100f,
    onDragTarget: (Int?) -> Unit = {},
    waveformStart: Int = start,
    waveformDuration: Int = duration,
    contextMenu: (@Composable () -> Unit)? = null,
) {
    val cellPx = with(LocalDensity.current) { FRAME_CELL_WIDTH.toPx() }
    val rowPx = with(LocalDensity.current) { TRACK_HEIGHT.toPx() }
    var previewStart by remember(id, start, duration) { mutableIntStateOf(start) }
    var previewDuration by remember(id, start, duration) { mutableIntStateOf(duration) }
    var targetTrack by remember(id, track) { mutableIntStateOf(track) }
    var dragging by remember { mutableStateOf(false) }
    var trimming by remember { mutableStateOf(false) }
    val move by rememberUpdatedState(onMove)
    val trim by rememberUpdatedState(onTrim)
    val select by rememberUpdatedState(onSelect)
    val dragTarget by rememberUpdatedState(onDragTarget)
    val visibleStart = maxOf(previewStart.toFloat(), offsetFrames)
    val visibleEnd = minOf((previewStart + previewDuration).toFloat(), offsetFrames + visibleFrames)
    val sourceShift = if (dragging) start - previewStart else 0
    val waveform = remember(peaks, visibleStart, visibleEnd, sourceShift, waveformStart, waveformDuration) {
        val from = (((visibleStart + sourceShift - waveformStart) / waveformDuration) * peaks.size).toInt().coerceIn(
            0,
            peaks.size,
        )
        val to = (((visibleEnd + sourceShift - waveformStart) / waveformDuration) * peaks.size).toInt().coerceIn(
            from,
            peaks.size,
        )
        Waveform(peaks.subList(from, to))
    }
    fun restore() {
        previewStart = start
        previewDuration = duration
        targetTrack = track
        dragging = false
        trimming =
            false
        dragTarget(null)
    }
    if (visibleEnd <= visibleStart && !dragging) return

    Box(
        Modifier.offset(x = FRAME_CELL_WIDTH * (visibleStart - offsetFrames))
            .width(
                FRAME_CELL_WIDTH * (visibleEnd - visibleStart).coerceAtLeast(1f),
            ).height(TRACK_HEIGHT).padding(vertical = 3.dp)
            .border(
                if (selected) 2.dp else 1.dp,
                MaterialTheme.colorScheme.primary.copy(alpha = if (selected) 1f else .4f),
                RoundedCornerShape(6.dp),
            )
            .background(
                MaterialTheme.colorScheme.primary.copy(alpha = if (selected) .22f else .1f),
                RoundedCornerShape(6.dp),
            ),
    ) {
        Box(
            Modifier.fillMaxSize()
                .semantics {
                    contentDescription =
                        "$label, frame ${previewStart + 1}, $previewDuration frames${if (locked) ", locked" else ""}"
                }
                .clickable { select() }
                .pointerInput(id, start, duration, track, trackCount, locked, cellPx) {
                    if (!locked) {
                        var delta = ClipDragMath(cellPx)
                        var dy = 0f
                        detectDragGesturesAfterLongPress(
                            onDragStart = {
                                delta = ClipDragMath(cellPx)
                                dy = 0f
                                dragging = true
                                dragTarget(track)
                            },
                            onDragCancel = { restore() },
                            onDragEnd = {
                                move(previewStart, targetTrack)
                                restore()
                            },
                        ) { change, amount ->
                            change.consume()
                            previewStart = (start + delta.add(amount.x)).coerceAtLeast(0)
                            dy += amount.y
                            targetTrack = (track + (dy / rowPx).roundToInt()).coerceIn(0, trackCount - 1)
                            dragTarget(targetTrack)
                        }
                    }
                },
        ) {
            if (peaks.isNotEmpty()) {
                AudioClipWaveform(
                    waveform,
                    MaterialTheme.colorScheme.primary.copy(alpha = if (muted) .18f else .45f),
                )
            }
            Text(
                if (dragging ||
                    trimming
                ) {
                    "F${previewStart + 1} · ${if (dragging && trackCount > 1) "A${targetTrack + 1}" else "$previewDuration f"}"
                } else {
                    label
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.align(Alignment.CenterStart).padding(horizontal = if (selected) 20.dp else 6.dp),
            )
        }
        // Popup menus anchor to the visible clip, not a distant panel header.
        if (selected && !dragging && !trimming) contextMenu?.invoke()
        if (selected && !locked && allowTrim) {
            if (allowLeftTrim &&
                previewStart >= offsetFrames
            ) {
                AssetTrimHandle(
                    Modifier.align(Alignment.CenterStart),
                    "Trim start of $label",
                    cellPx,
                    onDelta = { frames ->
                        trimming = true
                        // Lane trims shorten the selected source window. Move uses the body instead.
                        val cut = frames.coerceIn(minStart - start, duration - 1)
                        previewStart = start + cut
                        previewDuration = duration - cut
                    },
                    onEnd = {
                        trim(previewStart, previewDuration)
                        restore()
                    },
                    onCancel = { restore() },
                )
            }
            if (previewStart + previewDuration <=
                offsetFrames + visibleFrames
            ) {
                AssetTrimHandle(
                    Modifier.align(Alignment.CenterEnd),
                    "Trim end of $label",
                    cellPx,
                    onDelta = { frames ->
                        trimming = true
                        previewDuration = (duration + frames).coerceIn(1, maxEnd - start)
                    },
                    onEnd = {
                        trim(previewStart, previewDuration)
                        restore()
                    },
                    onCancel = { restore() },
                )
            }
        }
    }
}

@Composable
private fun AssetTrimHandle(
    modifier: Modifier,
    description: String,
    cellPx: Float,
    onDelta: (Int) -> Unit,
    onEnd: () -> Unit,
    onCancel: () -> Unit,
) {
    val delta by rememberUpdatedState(onDelta)
    val end by rememberUpdatedState(onEnd)
    val cancel by rememberUpdatedState(onCancel)
    Box(
        modifier.width(12.dp).fillMaxHeight().semantics { contentDescription = description }
            .background(MaterialTheme.colorScheme.primary.copy(alpha = .3f))
            .pointerInput(cellPx) {
                var movement = ClipDragMath(cellPx)
                detectHorizontalDragGestures(
                    onDragStart = { movement = ClipDragMath(cellPx) },
                    onDragEnd = { end() },
                    onDragCancel = { cancel() },
                ) { change, px ->
                    change.consume()
                    delta(movement.add(px))
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text("Ⅱ", style = MaterialTheme.typography.labelSmall)
    }
}

@Preview(widthDp = 320, heightDp = 48, showBackground = true)
@Composable
private fun AssetClipPreview() {
    MaterialTheme {
        TimelineAssetClip("preview", "Dialogue", 0, 8, true, onSelect = {}, onMove = { _, _ -> }, onTrim = { _, _ -> })
    }
}
