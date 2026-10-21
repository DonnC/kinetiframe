package com.donnclab.kinetiframe.ui.components.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.ClipboardPlus
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Music
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.SquarePlus
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.ui.common.model.AssetDetails
import com.donnclab.kinetiframe.ui.common.model.AssetKind
import com.donnclab.kinetiframe.ui.common.model.CardThumbnail
import com.donnclab.kinetiframe.ui.common.model.PreviewKind
import com.donnclab.kinetiframe.ui.common.strings.FeatureStrings

/** How [AssetDetailsSheet]'s action button behaves. [openLabel] replaces "Add to project" with an
 *  open action for items that are documents in their own right (projects and actors). */
data class AssetDetailsContext(val hasOpenProject: Boolean = true, val openLabel: String? = null)

/** Library item details: an audio player or a static preview image, plus one primary action. */
@Composable
fun AssetDetailsSheet(
    details: AssetDetails,
    context: AssetDetailsContext,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.verticalScroll(rememberScrollState()).padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceM),
        verticalArrangement = Arrangement.spacedBy(Dimens.spaceL),
    ) {
        if (details.kind == AssetKind.AUDIO) {
            details.audioPath?.let { com.donnclab.kinetiframe.ui.audio.AudioPreview(it) }
                ?: Text("Audio file is unavailable", color = MaterialTheme.colorScheme.error)
        } else {
            AssetPreviewArea(details)
        }
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.spaceXs)) {
            Text(details.title, style = MaterialTheme.typography.titleLarge)
            Text(
                details.specsLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (context.openLabel != null) {
            PrimaryActionButton(
                ActionButtonContent(context.openLabel, Lucide.ClipboardPlus),
                ActionButtonState.ENABLED,
                onClick = onAction,
            )
        } else if (context.hasOpenProject) {
            PrimaryActionButton(
                ActionButtonContent(FeatureStrings.Library.ADD_TO_PROJECT, Lucide.SquarePlus),
                ActionButtonState.ENABLED,
                onClick = onAction,
            )
        } else {
            Text(
                FeatureStrings.Library.NO_PROJECT_HELPER,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AssetPreviewArea(details: AssetDetails) {
    Box(Modifier.fillMaxWidth().aspectRatio(1.7f)) {
        AssetThumbnailBox(thumbnail = details.thumbnail, modifier = Modifier.aspectRatio(1.7f))
        // Motion assets remain static until an actual motion preview renderer is connected.
        val badge = previewBadgeFor(PreviewKind.STATIC_SCREENSHOT)
        PreviewBadge(badge.first, badge.second, modifier = Modifier.align(Alignment.TopStart).padding(Dimens.spaceS))
    }
}

private fun previewBadgeFor(kind: PreviewKind) = when (kind) {
    PreviewKind.STATIC_SCREENSHOT -> FeatureStrings.AssetDetails.STATIC_PREVIEW to Lucide.Square

    PreviewKind.LIVE_MOTION, PreviewKind.LIVE_COLLAGE ->
        FeatureStrings.AssetDetails.LIVE_PREVIEW_LOOPS to
            Lucide.ClipboardPlus

    PreviewKind.AUDIO_PLAYER -> FeatureStrings.AssetDetails.AUDIO_PREVIEW to Lucide.Music
}

@Preview(showBackground = true)
@Composable
private fun AssetDetailsSheetPreview() {
    val sample = AssetDetails(
        id = "preview",
        kind = AssetKind.ACTOR_PERFORMANCE,
        previewKind = PreviewKind.LIVE_MOTION,
        title = "Walk Cycle Take 3",
        specsLine = "Humanoid  ·  12 FPS  ·  1080×1920",
        thumbnail = CardThumbnail.SolidGradient(Color(0xFFFFD9CC), Color(0xFFFF6A45)),
    )
    KinetiFrameTheme { AssetDetailsSheet(sample, AssetDetailsContext(), onAction = {}) }
}
