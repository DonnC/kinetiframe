package com.donnclab.kinetiframe.presentation.actors.rigging

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Drama
import com.composables.icons.lucide.Footprints
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.donnclab.kinetiframe.actors.rig.presets.ActorPreset
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipe
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeChannels
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeDocument
import com.donnclab.kinetiframe.core.actor.presets.PresetRecipeMetadata
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.actors.labelRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Coarse grouping inferred from a preset's stable ID until the asset contract carries a real category field. */
private enum class PresetCategory(val label: String) {
    LOCOMOTION("Locomotion"),
    CONVERSATIONAL("Conversational"),
    EMOTIONAL("Emotional"),
    OTHER("Motion"),
    ;

    companion object {
        fun of(preset: ActorPreset): PresetCategory {
            val key = preset.recipe.id.lowercase()
            return when {
                listOf("walk", "run", "jump", "sneak", "skip", "leap").any(key::contains) -> LOCOMOTION
                listOf("talk", "nod", "shake", "wave").any(key::contains) -> CONVERSATIONAL
                listOf("cheer", "sigh", "surprise", "suprise", "emotional").any(key::contains) -> EMOTIONAL
                else -> OTHER
            }
        }
    }
}

private fun PresetCategory.icon() = when (this) {
    PresetCategory.LOCOMOTION -> Lucide.Footprints
    PresetCategory.CONVERSATIONAL -> Lucide.MessageCircle
    PresetCategory.EMOTIONAL -> Lucide.Drama
    PresetCategory.OTHER -> Lucide.Activity
}

/** Legacy raster previews remain readable; the new bundled gestures use compact vector icons. */
@Composable
private fun rememberPresetThumbnail(path: String?): Bitmap? {
    val context = LocalContext.current
    val state = produceState<Bitmap?>(initialValue = null, path) {
        value = path?.takeUnless { it.endsWith(".svg") }?.let {
            withContext(Dispatchers.IO) {
                runCatching { context.assets.open(it).use(BitmapFactory::decodeStream) }.getOrNull()
            }
        }
    }
    return state.value
}

/** One preset in the Preset Library: real preview artwork when its asset pack ships one, a
 * category icon otherwise. Contributor-added presets follow the same image+JSON pairing rule
 * ([com.donnclab.kinetiframe.actors.data.ActorPresetRepository]), so a new pack needs no code change here. */
@Composable
fun ActorPresetCard(
    preset: ActorPreset,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val thumbnail = rememberPresetThumbnail(preset.previewAssetPath)
    val context = LocalContext.current
    val vector by produceState<androidx.compose.ui.graphics.vector.ImageVector?>(null, preset.previewAssetPath) {
        value = preset.previewAssetPath?.takeIf { it.endsWith(".svg") }?.let { path ->
            withContext(Dispatchers.IO) {
                runCatching {
                    context.assets.open(path).bufferedReader().use { PresetVectorIcon.decode(it.readText()) }
                }.getOrNull()
            }
        }
    }
    val category = remember(preset.recipe.id) { PresetCategory.of(preset) }
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(ActorStudioTokens.presetCard),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                ActorStudioTokens.accent.copy(
                    alpha = 0.16f,
                )
            } else {
                ActorStudioTokens.panelSurfaceVariant
            },
        ),
        border = androidx.compose.foundation.BorderStroke(
            Dimens.borderThin,
            if (selected) ActorStudioTokens.accent else ActorStudioTokens.panelDivider,
        ),
    ) {
        Column(Modifier.fillMaxSize().padding(Dimens.spaceS), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .background(ActorStudioTokens.panelSurface, RoundedCornerShape(Dimens.radiusXxs)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.foundation.layout.Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (vector != null) {
                        Icon(
                            vector!!,
                            preset.recipe.name,
                            tint = ActorStudioTokens.onPanel,
                            modifier = Modifier.fillMaxSize().padding(Dimens.spaceXs),
                        )
                    } else if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = preset.recipe.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(Dimens.spaceXs),
                        )
                    } else {
                        Icon(
                            category.icon(),
                            null,
                            tint = if (selected) ActorStudioTokens.accent else ActorStudioTokens.onPanelMuted,
                            modifier = Modifier.size(ActorStudioTokens.presetIconButton),
                        )
                    }
                }
            }
            Text(
                preset.recipe.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ActorStudioTokens.onPanel,
                maxLines = 1,
            )
            Text(
                stringResource(preset.recipe.authoredView.labelRes()),
                style = MaterialTheme.typography.labelSmall,
                color = ActorStudioTokens.onPanelMuted,
                maxLines = 1,
            )
        }
    }
}

@Preview
@Composable
private fun ActorPresetCardPreview() {
    ActorPresetCard(
        preset = ActorPreset(
            PresetRecipeDocument(
                PresetRecipe(
                    PresetRecipeMetadata(1, "wave-hand", "Wave", 1f),
                    PresetRecipeChannels(listOf("arm"), emptyList()),
                ),
                description = "",
            ),
        ),
        selected = true,
        enabled = true,
        onClick = {},
    )
}
