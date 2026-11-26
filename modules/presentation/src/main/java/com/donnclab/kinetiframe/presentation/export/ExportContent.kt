// SPDX-License-Identifier: Apache-2.0
package com.donnclab.kinetiframe.presentation.export

import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import com.composables.icons.lucide.Check
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Play
import com.composables.icons.lucide.Share2
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.export.model.ExportPreset
import com.donnclab.kinetiframe.export.model.ExportProjectSummary
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.asString
import com.donnclab.kinetiframe.presentation.common.windowWidthClass

/** What the export screen's buttons do. */
data class ExportActions(
    val onChoose: (ExportPreset) -> Unit,
    val onStart: () -> Unit,
    val onWatch: () -> Unit,
    val onSave: () -> Unit,
    val onShare: () -> Unit,
    val onClose: () -> Unit,
)

/**
 * The export screen: a large preview shaped like the video it will make, the project's facts, the
 * output size as picture cards, and the step's actions (export, its progress over the preview, then
 * watch, save and share). It replaced a column of chips and one button. A tablet shows the preview
 * beside the options; a phone stacks them.
 */
@Composable
fun ExportContent(state: ExportState, actions: ExportActions, modifier: Modifier = Modifier) {
    if (windowWidthClass().isCompact) {
        Column(
            modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(Dimens.spaceXl),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceXl),
        ) {
            ExportPreview(state, actions.onWatch, Modifier.fillMaxWidth())
            ExportDetails(state, actions)
        }
    } else {
        Row(
            modifier.fillMaxSize().padding(Dimens.spaceXl),
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXlPlus),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.weight(1.2f).fillMaxSize(), contentAlignment = Alignment.Center) {
                ExportPreview(state, actions.onWatch)
            }
            Column(
                Modifier.weight(1f).widthIn(max = Dimens.readableWidth).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Dimens.spaceXl),
            ) { ExportDetails(state, actions) }
        }
    }
}

/** The output's size: the preset's, or the project's own canvas for "Same as project". */
private fun ExportState.outputSize(): StageDimensions? = preset.dimensions ?: summary?.stage

/**
 * The project's picture in a frame shaped like the video, letterboxed as the export will be, so
 * choosing Square or Vertical shows what happens to the picture. Progress shows over it while it
 * renders, and Play once it's done.
 */
@Composable
private fun ExportPreview(state: ExportState, onWatch: () -> Unit, modifier: Modifier = Modifier) {
    val size = state.outputSize() ?: StageDimensions.LandscapeHd
    Box(
        modifier.aspectRatio(size.width.toFloat() / size.height.coerceAtLeast(1))
            .clip(RoundedCornerShape(Dimens.radiusL))
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        // The project's canvas on paper, in its own shape inside the video's frame: the black
        // around it is the letterbox the export adds, and dark lines on a clear drawing still show.
        state.summary?.let { summary ->
            val stage = summary.stage
            Box(
                Modifier.aspectRatio(stage.width.toFloat() / stage.height.coerceAtLeast(1))
                    .background(EditorTokens.framePaper),
            ) {
                summary.thumbnailPath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = state.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        when (state.phase) {
            ExportPhase.Rendering -> Box(
                Modifier.fillMaxSize().background(Color.Black.copy(alpha = SCRIM)),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(
                    progress = { state.progress },
                    color = Color.White,
                    modifier = Modifier.size(Dimens.progressLarge),
                )
                Text(
                    stringResource(R.string.export_progress_percent, (state.progress * PERCENT).toInt()),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                )
            }

            is ExportPhase.Done -> FilledIconButton(
                onClick = onWatch,
                modifier = Modifier.size(Dimens.progressLarge),
            ) {
                Icon(Lucide.Play, stringResource(R.string.export_watch))
            }

            else -> Unit
        }
    }
}

/** The project's name and facts, then the step: size and Export, progress, results or an error. */
@Composable
private fun ExportDetails(state: ExportState, actions: ExportActions) {
    Text(
        state.displayName,
        style = MaterialTheme.typography.headlineSmall,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
    )
    state.summary?.let { summary -> ExportFacts(summary, state.outputSize()) }
    when (val phase = state.phase) {
        ExportPhase.ChoosingPreset -> {
            Text(stringResource(R.string.export_size_title), style = MaterialTheme.typography.titleSmall)
            Text(
                stringResource(R.string.export_choose_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ExportSizeCards(state.preset, state.summary?.stage, actions.onChoose)
            Button(onClick = actions.onStart, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.export_start))
            }
        }

        ExportPhase.Rendering -> Text(
            stringResource(R.string.export_rendering),
            style = MaterialTheme.typography.titleMedium,
        )

        is ExportPhase.Done -> {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS),
            ) {
                Icon(Lucide.Check, null, tint = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.export_ready), style = MaterialTheme.typography.titleMedium)
            }
            Button(onClick = actions.onSave, modifier = Modifier.fillMaxWidth()) {
                Icon(Lucide.Download, null, Modifier.size(Dimens.iconS))
                Text(stringResource(R.string.export_save_to_gallery), Modifier.padding(start = Dimens.spaceS))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM)) {
                OutlinedButton(onClick = actions.onShare, modifier = Modifier.weight(1f)) {
                    Icon(Lucide.Share2, null, Modifier.size(Dimens.iconS))
                    Text(stringResource(R.string.action_share), Modifier.padding(start = Dimens.spaceS))
                }
                OutlinedButton(onClick = actions.onWatch, modifier = Modifier.weight(1f)) {
                    Icon(Lucide.Play, null, Modifier.size(Dimens.iconS))
                    Text(stringResource(R.string.export_watch), Modifier.padding(start = Dimens.spaceS))
                }
            }
        }

        is ExportPhase.Failed -> {
            Text(
                stringResource(R.string.export_failed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            Text(phase.message.asString(), style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM)) {
                Button(onClick = actions.onStart, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.export_try_again))
                }
                OutlinedButton(onClick = actions.onClose, modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.action_close))
                }
            }
        }
    }
}

/** Length, frame rate and the video's size, as short facts under the name. */
@Composable
private fun ExportFacts(summary: ExportProjectSummary, output: StageDimensions?) {
    val seconds = if (summary.fps > 0) summary.totalFrames / summary.fps else 0
    val length = stringResource(
        R.string.home_card_length,
        "%d:%02d".format(seconds / SECONDS_PER_MINUTE, seconds % SECONDS_PER_MINUTE),
        pluralStringResource(R.plurals.home_card_frames, summary.totalFrames, summary.totalFrames),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS)) {
        Fact(length)
        Fact(stringResource(R.string.fps_chip, summary.fps))
        output?.let { Fact(stringResource(R.string.export_output, it.width, it.height)) }
    }
}

@Composable
private fun Fact(text: String) {
    Surface(shape = RoundedCornerShape(Dimens.radiusPill), color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = Dimens.spaceM, vertical = Dimens.spaceXs),
        )
    }
}

/** The four output sizes as cards with their shape drawn, the chosen one outlined. */
@Composable
private fun ExportSizeCards(chosen: ExportPreset, projectStage: StageDimensions?, onChoose: (ExportPreset) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS), modifier = Modifier.fillMaxWidth()) {
        ExportPreset.entries.forEach { preset ->
            val size = preset.dimensions ?: projectStage
            val selected = preset == chosen
            val colours = MaterialTheme.colorScheme
            val fill = if (selected) colours.primaryContainer else colours.surfaceVariant
            val edge = if (selected) colours.primary else Color.Transparent
            Surface(
                onClick = { onChoose(preset) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(Dimens.radiusM),
                color = fill,
                border = BorderStroke(Dimens.borderMedium, edge),
            ) {
                Column(Modifier.padding(Dimens.spaceS), horizontalAlignment = Alignment.CenterHorizontally) {
                    ShapeGlyph(size, MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        stringResource(preset.shortLabel()),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                    )
                    size?.let {
                        Text(
                            stringResource(R.string.export_dimensions, it.width, it.height),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

/** A size's shape, as the canvas cards on New Project draw it. */
@Composable
private fun ShapeGlyph(size: StageDimensions?, colour: Color) {
    Canvas(Modifier.size(Dimens.iconXl)) {
        val aspect = size?.let { it.width.toFloat() / it.height.coerceAtLeast(1) } ?: 1f
        val w = if (aspect >= 1f) this.size.width * GLYPH else this.size.height * GLYPH * aspect
        val h = if (aspect >= 1f) w / aspect else this.size.height * GLYPH
        drawRoundRect(
            colour,
            Offset((this.size.width - w) / 2, (this.size.height - h) / 2),
            Size(w, h),
            CornerRadius(this.size.minDimension * CORNER),
            style = Stroke(this.size.minDimension * LINE),
        )
    }
}

@StringRes
private fun ExportPreset.shortLabel(): Int = when (this) {
    ExportPreset.SAME_AS_PROJECT -> R.string.export_size_same
    ExportPreset.SQUARE -> R.string.export_size_square
    ExportPreset.VERTICAL -> R.string.export_size_vertical
    ExportPreset.LANDSCAPE -> R.string.export_size_landscape
}

private const val SCRIM = 0.55f
private const val PERCENT = 100
private const val SECONDS_PER_MINUTE = 60
private const val GLYPH = 0.8f
private const val CORNER = 0.08f
private const val LINE = 0.06f
