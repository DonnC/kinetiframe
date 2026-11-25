package com.donnclab.kinetiframe.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import coil.compose.AsyncImage
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.core.document.ProjectManifest
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.HomeTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.MediaBadge
import com.donnclab.kinetiframe.presentation.common.MediaCard
import com.donnclab.kinetiframe.presentation.common.MediaCardMenu
import java.io.File

/*
 * Home's project, actor and pack cards. All three use the shared [MediaCard]: the picture on top
 * with short facts as badges, and the name, a detail line and the ⋮ menu on a solid caption below.
 */

private fun formatDuration(totalFrames: Int, fps: Int): String {
    if (fps <= 0 || totalFrames <= 0) return "0:00"
    val totalSeconds = totalFrames / fps
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** A project card: fps and canvas size on the picture, the duration under the name. */
@Composable
fun HomeProjectCard(
    project: ProjectManifest,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    onShare: () -> Unit = {},
    onSaveToLibrary: () -> Unit = {},
    onExportVideo: () -> Unit = {},
    favourite: Boolean = false,
    onFavourite: () -> Unit = {},
    onOrganise: () -> Unit = {},
) {
    MediaCard(
        title = project.displayName,
        subtitle = formatDuration(project.totalFrames, project.fps),
        onClick = onOpen,
        modifier = modifier,
        media = {
            if (project.thumbnailPath != null) {
                AsyncImage(
                    model = project.thumbnailPath,
                    contentDescription = project.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant))
            }
        },
        badges = {
            // One row, so long text shrinks both badges instead of stacking one over the other.
            MediaBadge(
                stringResource(R.string.home_chip_fps, project.fps),
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.weight(1f, fill = false),
            )
            MediaBadge(
                stringResource(R.string.size_dimensions, project.stage.width, project.stage.height),
                modifier = Modifier.weight(1f, fill = false),
            )
        },
        menu = {
            MediaCardMenu(
                stringResource(R.string.project_actions),
                listOf(
                    ContextIconAction(
                        Icons.Default.Star,
                        stringResource(R.string.action_favourite),
                        onFavourite,
                        selected = favourite,
                    ),
                    ContextIconAction(Icons.Default.Folder, stringResource(R.string.action_collection), onOrganise),
                    ContextIconAction(Icons.Default.Edit, stringResource(R.string.action_rename), onRename),
                    ContextIconAction(Icons.Default.Movie, stringResource(R.string.action_export_video), onExportVideo),
                    ContextIconAction(Icons.Default.Share, stringResource(R.string.action_share), onShare),
                    ContextIconAction(
                        Icons.Default.LibraryAdd,
                        stringResource(R.string.action_add_to_library),
                        onSaveToLibrary,
                    ),
                    ContextIconAction(Icons.Default.Delete, stringResource(R.string.action_delete), onDelete),
                ),
            )
        },
    )
}

@Composable
private fun PackPreviewCell(file: File?, modifier: Modifier = Modifier) {
    Box(modifier.background(Color.White.copy(alpha = .12f))) {
        if (file != null && file.isFile) {
            AsyncImage(
                model = file,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val PackTints = HomeTokens.placeholderTints

private fun pastelFor(seed: String): Color {
    val index = (seed.hashCode().let { if (it == Int.MIN_VALUE) 0 else kotlin.math.abs(it) }) % PackTints.size
    return PackTints[index]
}

/** The actions a saved-pack card offers, grouped so [HomePackCard] doesn't take a lambda per
 *  menu row. */
data class PackCardActions(
    val onOpen: () -> Unit,
    val onEdit: () -> Unit,
    val onShare: () -> Unit,
    val onDelete: () -> Unit,
    val onFavourite: (() -> Unit)? = null,
    val onOrganise: (() -> Unit)? = null,
    val favourite: Boolean = false,
)

/** A 2x2 collage of a pack's first four drawings, or a tinted tile with a stack glyph when none is readable. */
@Composable
private fun PackCollage(packId: String, previewFiles: List<File>) {
    val tint = remember(packId) { pastelFor(packId) }
    Box(Modifier.fillMaxSize().background(tint), contentAlignment = Alignment.Center) {
        if (previewFiles.isEmpty()) {
            Icon(
                Lucide.Layers,
                null,
                tint = Color.White.copy(alpha = .85f),
                modifier = Modifier.size(HomeTokens.placeholderIcon),
            )
        } else {
            Column(Modifier.fillMaxSize()) {
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    PackPreviewCell(previewFiles.getOrNull(0), Modifier.weight(1f).fillMaxHeight())
                    PackPreviewCell(previewFiles.getOrNull(1), Modifier.weight(1f).fillMaxHeight())
                }
                Row(Modifier.weight(1f).fillMaxWidth()) {
                    PackPreviewCell(previewFiles.getOrNull(2), Modifier.weight(1f).fillMaxHeight())
                    PackPreviewCell(previewFiles.getOrNull(3), Modifier.weight(1f).fillMaxHeight())
                }
            }
        }
    }
}

/** One saved-pack card: a collage of its drawings, with the item count under the name. */
@Composable
fun HomePackCard(pack: LibraryItemManifest, itemCount: Int, previewFiles: List<File>, actions: PackCardActions) {
    MediaCard(
        title = pack.displayName,
        subtitle = pluralStringResource(R.plurals.home_chip_items, itemCount, itemCount),
        onClick = actions.onOpen,
        media = { PackCollage(pack.libraryItemId, previewFiles) },
        menu = { MediaCardMenu(stringResource(R.string.pack_actions), packMenuActions(actions)) },
    )
}

@Composable
private fun packMenuActions(actions: PackCardActions): List<ContextIconAction> {
    val favouriteLabel = stringResource(
        if (actions.favourite) R.string.action_unfavourite else R.string.action_favourite,
    )
    val organiseLabel = stringResource(R.string.action_organise)
    val edit = stringResource(R.string.pack_edit)
    val share = stringResource(R.string.pack_share)
    val delete = stringResource(R.string.pack_delete)
    return buildList {
        actions.onFavourite?.let {
            add(ContextIconAction(Icons.Default.Star, favouriteLabel, it, selected = actions.favourite))
        }
        actions.onOrganise?.let { add(ContextIconAction(Icons.Default.Folder, organiseLabel, it)) }
        add(ContextIconAction(Icons.Default.Edit, edit, actions.onEdit))
        add(ContextIconAction(Icons.Default.Share, share, actions.onShare))
        add(ContextIconAction(Icons.Default.Delete, delete, actions.onDelete))
    }
}

/** An actor card: the actor drawn whole on the picture, with how many angles it has under the name. */
@Composable
fun HomeActorCard(
    actor: ActorManifest,
    thumbnailPath: String?,
    onOpen: () -> Unit,
    actions: ActorCardActions,
    favourite: Boolean = false,
) {
    val (onRename, onDuplicate, onExport, onArchive, onFavourite, onOrganise) = actions
    val angles = actor.authoredViews().size
    MediaCard(
        title = actor.displayName,
        subtitle = pluralStringResource(R.plurals.home_chip_views, angles, angles),
        onClick = onOpen,
        media = {
            Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)) {
                if (thumbnailPath == null) {
                    com.donnclab.kinetiframe.presentation.actors.rigging.RigBlueprintThumbnail(
                        com.donnclab.kinetiframe.actors.rig.model.ActorTemplate.HUMAN,
                        Modifier.fillMaxSize().padding(Dimens.spaceM),
                    )
                } else {
                    AsyncImage(
                        model = "file://$thumbnailPath",
                        contentDescription = stringResource(R.string.actor_preview_description, actor.displayName),
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize().padding(Dimens.spaceS),
                    )
                }
            }
        },
        menu = {
            MediaCardMenu(
                stringResource(R.string.actor_actions),
                listOf(
                    ContextIconAction(
                        Icons.Default.Star,
                        stringResource(R.string.action_favourite),
                        onFavourite,
                        selected = favourite,
                    ),
                    ContextIconAction(Icons.Default.Folder, stringResource(R.string.action_collection), onOrganise),
                    ContextIconAction(Icons.Default.Edit, stringResource(R.string.action_rename), onRename),
                    ContextIconAction(
                        Icons.Default.ContentCopy,
                        stringResource(R.string.action_duplicate),
                        onDuplicate,
                    ),
                    ContextIconAction(
                        Icons.Default.LibraryAdd,
                        stringResource(R.string.action_add_to_library),
                        onExport,
                    ),
                    ContextIconAction(Icons.Default.Archive, stringResource(R.string.action_archive), onArchive),
                ),
            )
        },
    )
}
