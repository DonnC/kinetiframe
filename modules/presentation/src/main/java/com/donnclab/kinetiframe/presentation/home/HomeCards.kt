package com.donnclab.kinetiframe.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.donnclab.kinetiframe.designsystem.theme.LibraryTokens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.MediaBadge
import com.donnclab.kinetiframe.presentation.common.MediaCard
import com.donnclab.kinetiframe.presentation.common.MediaCardMenu
import com.donnclab.kinetiframe.presentation.common.projectLength
import java.io.File

/*
 * Home's project, actor and pack cards. All three use the shared [MediaCard]: the picture on top
 * with short facts as badges, and the name, a detail line and the ⋮ menu on a solid caption below.
 */

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
    dragModifier: Modifier = Modifier,
) {
    MediaCard(
        title = project.displayName,
        subtitle = projectLength(project.totalFrames, project.fps),
        onClick = onOpen,
        modifier = modifier,
        contentModifier = dragModifier,
        media = {
            // The whole canvas on light paper, as in Library: a portrait drawing cropped to the card
            // could show nothing, and clear paper on a dark card hides dark lines.
            Box(Modifier.fillMaxSize().background(LibraryTokens.stampWell)) {
                project.thumbnailPath?.let { path ->
                    AsyncImage(
                        model = path,
                        contentDescription = project.displayName,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
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

/**
 * A pack's first drawing on light paper, as in Library. Pack drawings are dark lines on clear
 * paper, so they need a light page in every theme or black line art disappears in dark mode.
 */
@Composable
private fun PackCover(previewFiles: List<File>) {
    val cover = previewFiles.firstOrNull { it.isFile }
    Box(Modifier.fillMaxSize().background(LibraryTokens.stampWell), contentAlignment = Alignment.Center) {
        if (cover == null) {
            Icon(
                Lucide.Layers,
                null,
                tint = LibraryTokens.stampPlaceholder,
                modifier = Modifier.size(HomeTokens.placeholderIcon),
            )
        } else {
            AsyncImage(
                model = cover,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(Dimens.spaceM),
            )
        }
    }
}

/** One saved-pack card: its first drawing on light paper, with the item count under the name. */
@Composable
fun HomePackCard(pack: LibraryItemManifest, itemCount: Int, previewFiles: List<File>, actions: PackCardActions) {
    MediaCard(
        title = pack.displayName,
        subtitle = pluralStringResource(R.plurals.home_chip_items, itemCount, itemCount),
        onClick = actions.onOpen,
        media = { PackCover(previewFiles) },
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
