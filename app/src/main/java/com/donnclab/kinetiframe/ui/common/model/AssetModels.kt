package com.donnclab.kinetiframe.ui.common.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/** What an asset actually is -- drives both which preview behavior [PreviewKind] makes sense and
 *  which spec line a card/sheet shows. */
enum class AssetKind { PROJECT, ACTOR_RIG, ACTOR_PERFORMANCE, PACK, AUDIO, BACKGROUND_IMAGE, FRAME }

/** How the Asset Details Card should render its preview area for a given [AssetKind], per the
 *  product rule: full projects get a locked static screenshot (no buffering risk), everything
 *  else plays a short automatic loop. */
enum class PreviewKind { STATIC_SCREENSHOT, LIVE_MOTION, LIVE_COLLAGE, AUDIO_PLAYER }

/** The two chips a grid card can float over its thumbnail (e.g. fps + canvas size, or a single
 *  spec like rig type). Either slot may be left null. */
data class CardChips(val topStart: String? = null, val topEnd: String? = null)

/** How a grid card paints its thumbnail. Kept as one sealed type so [AssetGridCard] stays a single
 *  reusable component instead of forking per asset kind. */
sealed interface CardThumbnail {
    data class LocalImage(val path: String) : CardThumbnail
    data class IconOnGradient(val start: Color, val end: Color, val icon: ImageVector) : CardThumbnail
    data class Collage(val start: Color, val end: Color, val icons: List<ImageVector>) : CardThumbnail
    data class SolidGradient(val start: Color, val end: Color) : CardThumbnail
}

/** Full detail set behind one asset -- what the Asset Details Card renders, regardless of which
 *  tab or source screen opened it. */
data class AssetDetails(
    val id: String,
    val kind: AssetKind,
    val previewKind: PreviewKind,
    val title: String,
    val specsLine: String,
    val thumbnail: CardThumbnail,
    val waveformSeed: Int = 0,
    val audioPath: String? = null,
)

/** One card in a Projects/Actors/Packs grid. [scrimSubtitle] is the second line under the title in
 *  the bottom scrim (a duration, an item count, a bone count -- whatever that grid's cards show). */
data class AssetSummary(
    val id: String,
    val title: String,
    val scrimSubtitle: String,
    val thumbnail: CardThumbnail,
    val chips: CardChips = CardChips(),
    val details: AssetDetails,
)

/** One row in an Audio tab's list. */
data class AudioTrackSummary(
    val id: String,
    val title: String,
    val durationLabel: String,
    val tagLabel: String,
    val waveformSeed: Int,
    val details: AssetDetails,
)

/** One stamp graphic inside a pack's contents drill-down grid. */
data class StampAsset(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val tint: Color,
    val imagePath: String? = null,
)
