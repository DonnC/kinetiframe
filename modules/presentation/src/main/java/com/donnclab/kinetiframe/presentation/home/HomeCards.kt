package com.donnclab.kinetiframe.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryAdd
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
import com.donnclab.kinetiframe.presentation.common.IconContextMenu
import java.io.File

/** One Home dashboard card, shared shape across all three tabs: a full-bleed thumbnail, a small
 *  translucent chip (or two) overlaid top-corner for the item's key stats, a bottom scrim carrying
 *  the title (and, for a project, its duration), and a corner overflow menu for the same
 *  rename/share/delete actions each tab already had on its own standalone screen. */
private val ScrimBrush = Brush.verticalGradient(listOf(Color.Transparent, HomeTokens.cardScrim))

@Composable
private fun HomeChip(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    Surface(modifier = modifier, shape = RoundedCornerShape(HomeTokens.chipCorner), color = HomeTokens.chipBg) {
        Row(
            Modifier.padding(horizontal = HomeTokens.badgePaddingHorizontal, vertical = Dimens.spaceXs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(it, null, tint = Color.White, modifier = Modifier.size(Dimens.iconXs))
                androidx.compose.foundation.layout.Spacer(Modifier.size(Dimens.spaceXxsPlus))
            }
            Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

private fun formatDuration(totalFrames: Int, fps: Int): String {
    if (fps <= 0 || totalFrames <= 0) return "0:00"
    val totalSeconds = totalFrames / fps
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}

/** A project card: the thumbnail fills the card, with fps top-left, canvas size top-right and the
 *  duration beside the title in the bottom scrim. */
@Composable
fun HomeProjectCard(
    project: ProjectManifest,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit = {},
    onSaveToLibrary: () -> Unit = {},
    onExportVideo: () -> Unit = {},
    favourite: Boolean = false,
    onFavourite: () -> Unit = {},
    onOrganise: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(HomeTokens.cardCorner))
            .clickable(onClick = onOpen),
    ) {
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
        // One row, so long text shrinks both badges instead of stacking one over the other.
        Row(
            Modifier.align(Alignment.TopStart).fillMaxWidth().padding(Dimens.spaceS),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            HomeChip(
                stringResource(R.string.home_chip_fps, project.fps),
                icon = Icons.Filled.PlayArrow,
                modifier = Modifier.weight(1f, fill = false),
            )
            HomeChip(
                stringResource(R.string.size_dimensions, project.stage.width, project.stage.height),
                modifier = Modifier.weight(1f, fill = false),
            )
        }
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(ScrimBrush)
                .padding(
                    top = HomeTokens.cardTitleTop,
                    start = Dimens.spaceSPlus,
                    end = HomeTokens.cardMenuReserve,
                    bottom = Dimens.spaceS,
                ),
        ) {
            Text(
                project.displayName,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                formatDuration(project.totalFrames, project.fps),
                color = Color.White.copy(alpha = .85f),
                style = MaterialTheme.typography.labelSmall,
            )
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(Dimens.spaceXxs)) {
            IconButton(onClick = {
                menuOpen = true
            }) { Icon(Icons.Default.MoreVert, stringResource(R.string.project_actions), tint = Color.White) }
            IconContextMenu(
                menuOpen,
                { menuOpen = false },
                listOf(
                    ContextIconAction(Icons.Default.Star, stringResource(R.string.action_favourite), {
                        menuOpen = false
                        onFavourite()
                    }, selected = favourite),
                    ContextIconAction(Icons.Default.Folder, stringResource(R.string.action_collection), {
                        menuOpen = false
                        onOrganise()
                    }),
                    ContextIconAction(Icons.Default.Edit, stringResource(R.string.action_rename), {
                        menuOpen = false
                        onRename()
                    }),
                    ContextIconAction(Icons.Default.Movie, stringResource(R.string.action_export_video), {
                        menuOpen = false
                        onExportVideo()
                    }),
                    ContextIconAction(Icons.Default.Share, stringResource(R.string.action_share), {
                        menuOpen = false
                        onShare()
                    }),
                    ContextIconAction(Icons.Default.LibraryAdd, stringResource(R.string.action_add_to_library), {
                        menuOpen = false
                        onSaveToLibrary()
                    }),
                    ContextIconAction(Icons.Default.Delete, stringResource(R.string.action_delete), {
                        menuOpen = false
                        onDelete()
                    }),
                ),
            )
        }
    }
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

/** One saved-pack card: a 2x2 collage of the pack's first four drawings (or a tinted tile with a
 *  stack glyph when no preview is readable), an item-count chip and a Share/Delete overflow menu. */
@Composable
fun HomePackCard(pack: LibraryItemManifest, itemCount: Int, previewFiles: List<File>, actions: PackCardActions) {
    var menuOpen by remember { mutableStateOf(false) }
    val tint = remember(pack.libraryItemId) { pastelFor(pack.libraryItemId) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(HomeTokens.cardCorner))
            .background(tint)
            .clickable(onClick = actions.onOpen),
    ) {
        if (previewFiles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    Lucide.Layers,
                    null,
                    tint = Color.White.copy(alpha = .85f),
                    modifier = Modifier.size(HomeTokens.placeholderIcon),
                )
            }
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
        HomeChip(
            pluralStringResource(R.plurals.home_chip_items, itemCount, itemCount),
            icon = Lucide.Layers,
            modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.spaceS),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(ScrimBrush)
                .padding(
                    top = HomeTokens.cardTitleTop,
                    start = Dimens.spaceSPlus,
                    end = HomeTokens.cardMenuReserve,
                    bottom = Dimens.spaceS,
                ),
        ) {
            Text(
                pack.displayName,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(Dimens.spaceXxs)) {
            IconButton(onClick = {
                menuOpen = true
            }) { Icon(Icons.Default.MoreVert, stringResource(R.string.pack_actions), tint = Color.White) }
            IconContextMenu(
                menuOpen,
                { menuOpen = false },
                buildList {
                    actions.onFavourite?.let { action ->
                        add(
                            ContextIconAction(
                                Icons.Default.Star,
                                stringResource(
                                    if (actions.favourite) R.string.action_unfavourite else R.string.action_favourite,
                                ),
                                {
                                    menuOpen = false
                                    action()
                                },
                                selected = actions.favourite,
                            ),
                        )
                    }
                    actions.onOrganise?.let { action ->
                        add(
                            ContextIconAction(Icons.Default.Folder, stringResource(R.string.action_organise), {
                                menuOpen =
                                    false
                                action()
                            }),
                        )
                    }
                    add(
                        ContextIconAction(Icons.Default.Edit, stringResource(R.string.pack_edit), {
                            menuOpen = false
                            actions.onEdit()
                        }),
                    )
                    add(
                        ContextIconAction(Icons.Default.Share, stringResource(R.string.pack_share), {
                            menuOpen = false
                            actions.onShare()
                        }),
                    )
                    add(
                        ContextIconAction(Icons.Default.Delete, stringResource(R.string.pack_delete), {
                            menuOpen = false
                            actions.onDelete()
                        }),
                    )
                },
            )
        }
    }
}

/** An actor card in the same thumbnail-first style as project cards, with a template-name chip
 *  in place of the fps and canvas-size chips a project card shows. */
@Composable
fun HomeActorCard(
    actor: ActorManifest,
    thumbnailPath: String?,
    onOpen: () -> Unit,
    actions: ActorCardActions,
    favourite: Boolean = false,
) {
    val (onRename, onDuplicate, onExport, onArchive, onFavourite, onOrganise) = actions
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(HomeTokens.cardCorner))
            .clickable(onClick = onOpen),
    ) {
        if (thumbnailPath == null) {
            com.donnclab.kinetiframe.presentation.actors.rigging.RigBlueprintThumbnail(
                com.donnclab.kinetiframe.actors.rig.model.ActorTemplate.HUMAN,
                Modifier.fillMaxSize().padding(Dimens.spaceXlPlus),
            )
        } else {
            AsyncImage(
                model = "file://$thumbnailPath",
                contentDescription = stringResource(R.string.actor_preview_description, actor.displayName),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(
                    top = Dimens.spaceXxl,
                    bottom = HomeTokens.cardFooterReserve,
                    start = Dimens.spaceM,
                    end = Dimens.spaceM,
                ),
            )
        }
        HomeChip(
            actor.authoredViews().size.let { pluralStringResource(R.plurals.home_chip_views, it, it) },
            modifier = Modifier.align(Alignment.TopEnd).padding(Dimens.spaceS),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(ScrimBrush)
                .padding(
                    top = HomeTokens.cardTitleTop,
                    start = Dimens.spaceSPlus,
                    end = HomeTokens.cardMenuReserve,
                    bottom = Dimens.spaceS,
                ),
        ) {
            Text(
                actor.displayName,
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Box(Modifier.align(Alignment.BottomEnd).padding(Dimens.spaceXxs)) {
            IconButton(onClick = {
                menuOpen = true
            }) { Icon(Icons.Default.MoreVert, stringResource(R.string.actor_actions), tint = Color.White) }
            IconContextMenu(
                menuOpen,
                { menuOpen = false },
                listOf(
                    ContextIconAction(Icons.Default.Star, stringResource(R.string.action_favourite), {
                        menuOpen = false
                        onFavourite()
                    }, selected = favourite),
                    ContextIconAction(Icons.Default.Folder, stringResource(R.string.action_collection), {
                        menuOpen = false
                        onOrganise()
                    }),
                    ContextIconAction(Icons.Default.Edit, stringResource(R.string.action_rename), {
                        menuOpen = false
                        onRename()
                    }),
                    ContextIconAction(Icons.Default.ContentCopy, stringResource(R.string.action_duplicate), {
                        menuOpen = false
                        onDuplicate()
                    }),
                    ContextIconAction(Icons.Default.LibraryAdd, stringResource(R.string.action_add_to_library), {
                        menuOpen = false
                        onExport()
                    }),
                    ContextIconAction(Icons.Default.Archive, stringResource(R.string.action_archive), {
                        menuOpen = false
                        onArchive()
                    }),
                ),
            )
        }
    }
}
