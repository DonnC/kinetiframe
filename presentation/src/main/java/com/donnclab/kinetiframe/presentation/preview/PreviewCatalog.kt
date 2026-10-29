package com.donnclab.kinetiframe.presentation.preview

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.composables.icons.lucide.Bone
import com.composables.icons.lucide.Circle
import com.composables.icons.lucide.Drama
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Shapes
import com.composables.icons.lucide.Square
import com.composables.icons.lucide.Triangle
import com.donnclab.kinetiframe.presentation.common.AssetDetails
import com.donnclab.kinetiframe.presentation.common.AssetKind
import com.donnclab.kinetiframe.presentation.common.AssetSummary
import com.donnclab.kinetiframe.presentation.common.AudioTrackSummary
import com.donnclab.kinetiframe.presentation.common.CardChips
import com.donnclab.kinetiframe.presentation.common.CardThumbnail
import com.donnclab.kinetiframe.presentation.common.PreviewKind
import com.donnclab.kinetiframe.presentation.common.StampAsset

/**
 * Sample Library content for Compose previews only. The running app reads real items through
 * [com.donnclab.kinetiframe.library.ui.LocalLibraryCatalog].
 */
object PreviewCatalog {

    private fun pastel(hex1: Long, hex2: Long) = Color(hex1) to Color(hex2)

    fun libraryProjects(): List<AssetSummary> {
        val (b1, b2) = pastel(0xFFCDE8FF, 0xFF8FC4F2)
        val (b3, b4) = pastel(0xFFD8F5D0, 0xFFA6E39A)
        val (b5, b6) = pastel(0xFFFFE0C2, 0xFFFFB889)
        val (b7, b8) = pastel(0xFFE6DCF7, 0xFFC6AEEA)
        return listOf(
            libraryProjectSummary("lp1", "Rooftop Chase", "24 fps", "1080x1920", "2m 14s", b1 to b2),
            libraryProjectSummary("lp2", "Meadow Loop", "12 fps", "1920x1080", "0m 38s", b3 to b4),
            libraryProjectSummary("lp3", "Untitled Sketch", "24 fps", "1080x1080", "0m 52s", b5 to b6),
            libraryProjectSummary("lp4", "Intro Titles", "30 fps", "1080x1920", "0m 12s", b7 to b8),
        )
    }

    fun libraryActors(): List<AssetSummary> {
        val (r1, r2) = pastel(0xFFFFD9CC, 0xFFFF6A45)
        val (r3, r4) = pastel(0xFFE6DCF7, 0xFFB79AE0)
        val (r5, r6) = pastel(0xFFD8F5D0, 0xFF8FCB80)
        val (r7, r8) = pastel(0xFFFFE8CF, 0xFFFFB482)
        return listOf(
            libraryActorSummary("la1", "My Ninja Rig", "Rig  ·  14 bones", r1 to r2, Lucide.Bone, isRig = true),
            libraryActorSummary("la2", "Bird Character", "Rig  ·  9 bones", r3 to r4, Lucide.Bone, isRig = true),
            libraryActorSummary("la3", "Shopkeeper NPC", "Rig  ·  16 bones", r5 to r6, Lucide.Bone, isRig = true),
            libraryActorSummary("la4", "Hero Base Rig", "Rig  ·  18 bones", r7 to r8, Lucide.Bone, isRig = true),
            libraryActorSummary(
                "la5",
                "Walk Cycle Take 3",
                "Performance  ·  0:04",
                r3 to r4,
                Lucide.Drama,
                isRig = false,
            ),
            libraryActorSummary(
                "la6",
                "Combo Performance",
                "Performance  ·  0:06",
                r5 to r6,
                Lucide.Drama,
                isRig = false,
            ),
        )
    }

    fun libraryPacks(): List<AssetSummary> = listOf(
        packSummary("lk1", "My Mouth Set", 6, faceCollage()),
        packSummary("lk2", "Custom FX", 4, fxCollage()),
        packSummary("lk3", "Hand Poses", 9, shapeCollage()),
        packSummary("lk4", "BG Textures", 5, bgCollage()),
    )

    fun libraryAudio(): List<AudioTrackSummary> = listOf(
        audioSummary("lu1", "Voice Line -- Intro", "0:08", "VOICE", 5),
        audioSummary("lu2", "My VO Take 1", "0:12", "VOICE", 6),
        audioSummary("lu3", "Recorded SFX -- Door", "0:01", "SFX", 7),
        audioSummary("lu4", "Background Hum", "0:45", "MUSIC", 8),
    )

    /** Twelve stamp graphics for previewing a pack's drill-down grid. */
    fun packContents(): List<StampAsset> {
        val names = listOf(
            "Smile", "Frown", "Shock", "Wink", "Surprise", "Neutral",
            "Wince", "Gasp", "Sleepy", "Laugh", "Angry", "Calm",
        )
        val tints = listOf(Color(0xFFFF6A45), Color(0xFFE14E28), Color(0xFFFF8A63), Color(0xFFD9401E))
        return names.mapIndexed { index, name ->
            StampAsset(
                "stamp_$index",
                name,
                Lucide.Circle,
                tints[
                    index %
                        tints.size,
                ],
            )
        }
    }

    // -- private builders, kept small so the public functions above read as plain data --

    private fun libraryActorSummary(
        id: String,
        title: String,
        specs: String,
        colors: Pair<Color, Color>,
        icon: ImageVector,
        isRig: Boolean,
    ) = AssetSummary(
        id = id,
        title = title,
        scrimSubtitle = specs,
        thumbnail = CardThumbnail.IconOnGradient(colors.first, colors.second, icon),
        chips = CardChips(topEnd = if (isRig) "Human" else "Performance"),
        details = AssetDetails(
            id,
            if (isRig) AssetKind.ACTOR_RIG else AssetKind.ACTOR_PERFORMANCE,
            PreviewKind.LIVE_MOTION,
            title,
            specs,
            CardThumbnail.IconOnGradient(colors.first, colors.second, icon),
        ),
    )

    private fun libraryProjectSummary(
        id: String,
        title: String,
        fps: String,
        canvasSize: String,
        duration: String,
        colors: Pair<Color, Color>,
    ) = AssetSummary(
        id = id,
        title = title,
        scrimSubtitle = duration,
        thumbnail = CardThumbnail.SolidGradient(colors.first, colors.second),
        chips = CardChips(topStart = fps, topEnd = canvasSize),
        details = AssetDetails(
            id,
            AssetKind.PROJECT,
            PreviewKind.STATIC_SCREENSHOT,
            title,
            "$fps  ·  $canvasSize",
            CardThumbnail.SolidGradient(colors.first, colors.second),
        ),
    )

    private fun packSummary(id: String, title: String, itemCount: Int, collage: CardThumbnail.Collage) = AssetSummary(
        id = id,
        title = title,
        scrimSubtitle = "$itemCount Items",
        thumbnail = collage,
        details = AssetDetails(
            id,
            AssetKind.PACK,
            PreviewKind.LIVE_COLLAGE,
            title,
            "$itemCount Items in this pack",
            collage,
        ),
    )

    private fun audioSummary(id: String, title: String, duration: String, tag: String, seed: Int) = AudioTrackSummary(
        id = id,
        title = title,
        durationLabel = duration,
        tagLabel = tag,
        waveformSeed = seed,
        details = AssetDetails(
            id,
            AssetKind.AUDIO,
            PreviewKind.AUDIO_PLAYER,
            title,
            "$tag  ·  $duration",
            CardThumbnail.SolidGradient(Color(0xFF2A2622), Color(0xFF4A423B)),
            waveformSeed = seed,
        ),
    )

    private fun faceCollage() = CardThumbnail.Collage(
        Color(0xFFFFE1D6),
        Color(0xFFFFC9B3),
        listOf(Lucide.Circle, Lucide.Circle, Lucide.Circle, Lucide.Circle),
    )
    private fun fxCollage() = CardThumbnail.Collage(
        Color(0xFFFFD9CC),
        Color(0xFFFF6A45),
        listOf(Lucide.Shapes, Lucide.Shapes, Lucide.Shapes, Lucide.Shapes),
    )
    private fun shapeCollage() = CardThumbnail.Collage(
        Color(0xFFFFE8CF),
        Color(0xFFFFB482),
        listOf(Lucide.Square, Lucide.Circle, Lucide.Triangle, Lucide.Square),
    )
    private fun bgCollage() = CardThumbnail.Collage(
        Color(0xFFF3E8DC),
        Color(0xFFE8C9A8),
        listOf(Lucide.Layers, Lucide.Layers, Lucide.Layers, Lucide.Layers),
    )
}
