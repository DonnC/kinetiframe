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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.Activity
import com.composables.icons.lucide.Drama
import com.composables.icons.lucide.Footprints
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.MessageCircle
import com.donnclab.kinetiframe.actors.rig.presets.ActorMotionPreset
import com.donnclab.kinetiframe.designsystem.theme.ActorStudioTokens
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
        fun of(preset: ActorMotionPreset): PresetCategory {
            val key = preset.id.lowercase()
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
    preset: ActorMotionPreset,
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
    val category = remember(preset.id) { PresetCategory.of(preset) }
    Card(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(96.dp),
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
            1.dp,
            if (selected) ActorStudioTokens.accent else ActorStudioTokens.panelDivider,
        ),
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Column(
                Modifier.weight(1f).fillMaxWidth()
                    .background(ActorStudioTokens.panelSurface, RoundedCornerShape(4.dp)),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                androidx.compose.foundation.layout.Box(
                    Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    if (vector != null) {
                        Icon(
                            vector!!,
                            preset.name,
                            tint = ActorStudioTokens.onPanel,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                        )
                    } else if (thumbnail != null) {
                        Image(
                            bitmap = thumbnail.asImageBitmap(),
                            contentDescription = preset.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize().padding(4.dp),
                        )
                    } else {
                        Icon(
                            category.icon(),
                            null,
                            tint = if (selected) ActorStudioTokens.accent else ActorStudioTokens.onPanelMuted,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                }
            }
            Text(
                preset.name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = ActorStudioTokens.onPanel,
                maxLines = 1,
            )
            Text(
                preset.authoredView.title,
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
        preset = ActorMotionPreset(
            id = "wave-hand",
            name = "Wave",
            description = "",
            durationSeconds = 1f,
            requiredBones = listOf("arm"),
            channels = emptyList(),
        ),
        selected = true,
        enabled = true,
        onClick = {},
    )
}
