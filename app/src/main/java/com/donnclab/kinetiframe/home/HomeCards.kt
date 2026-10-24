package com.donnclab.kinetiframe.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.composables.icons.lucide.Layers
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.R
import com.donnclab.kinetiframe.core.actor.document.ActorManifest
import com.donnclab.kinetiframe.core.actor.document.LibraryItemManifest
import com.donnclab.kinetiframe.data.project.ProjectManifest
import com.donnclab.kinetiframe.designsystem.theme.HomeTokens
import com.donnclab.kinetiframe.presentation.common.ContextIconAction
import com.donnclab.kinetiframe.presentation.common.IconContextMenu
import java.io.File

/** One Home dashboard card, shared shape across all three tabs: a full-bleed thumbnail, a small
 *  translucent chip (or two) overlaid top-corner for the item's key stats, a bottom scrim carrying
 *  the title (and, for a project, its duration), and a corner overflow menu for the same
 *  rename/share/delete actions each tab already had on its own standalone screen. */
private val ScrimBrush = Brush.verticalGradient(listOf(Color.Transparent, Color(0xB8141210)))

@Composable
private fun HomeChip(text: String, icon: ImageVector? = null, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(HomeTokens.chipCorner), color = HomeTokens.chipBg) {
        Row(
            Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(it, null, tint = Color.White, modifier = Modifier.size(11.dp))
                androidx.compose.foundation.layout.Spacer(Modifier.size(3.dp))
            }
            Text(
                text,
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
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
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        Modifier
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
        HomeChip(
            "${project.fps} fps",
            icon = Icons.Filled.PlayArrow,
            modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
        )
        HomeChip(
            "${project.stage.width}×${project.stage.height}",
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(ScrimBrush)
                .padding(top = 26.dp, start = 10.dp, end = 40.dp, bottom = 8.dp),
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
        Box(Modifier.align(Alignment.BottomEnd).padding(2.dp)) {
            IconButton(onClick = {
                menuOpen = true
            }) { Icon(Icons.Default.MoreVert, "Project actions", tint = Color.White) }
            IconContextMenu(
                menuOpen,
                { menuOpen = false },
                listOf(
                    ContextIconAction(Icons.Default.Star, "Favourite", {
                        menuOpen = false
                        onFavourite()
                    }, selected = favourite),
                    ContextIconAction(Icons.Default.Folder, "Collection", {
                        menuOpen = false
                        onOrganise()
                    }),
                    ContextIconAction(Icons.Default.Edit, "Rename", {
                        menuOpen = false
                        onRename()
                    }),
                    ContextIconAction(Icons.Default.Movie, "Export video", {
                        menuOpen = false
                        onExportVideo()
                    }),
                    ContextIconAction(Icons.Default.Share, "Share", {
                        menuOpen = false
                        onShare()
                    }),
                    ContextIconAction(Icons.Default.LibraryAdd, "Library", {
                        menuOpen = false
                        onSaveToLibrary()
                    }),
                    ContextIconAction(Icons.Default.Delete, "Delete", {
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

private val PackTints = listOf(
    Color(0xFFFFB199),
    Color(0xFF9FCBEF),
    Color(0xFF8FCB8A),
    Color(0xFFB77FE0),
    Color(0xFFE8C24C),
    Color(0xFFF2879F),
)

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
                Icon(Lucide.Layers, null, tint = Color.White.copy(alpha = .85f), modifier = Modifier.size(40.dp))
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
        HomeChip("$itemCount items", icon = Lucide.Layers, modifier = Modifier.align(Alignment.TopEnd).padding(8.dp))
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(ScrimBrush)
                .padding(top = 26.dp, start = 10.dp, end = 40.dp, bottom = 8.dp),
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
        Box(Modifier.align(Alignment.BottomEnd).padding(2.dp)) {
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
                                if (actions.favourite) "Unfavourite" else "Favourite",
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
                            ContextIconAction(Icons.Default.Folder, "Organise", {
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
    onRename: () -> Unit,
    onDuplicate: () -> Unit,
    onExport: () -> Unit,
    onArchive: () -> Unit,
    favourite: Boolean = false,
    onFavourite: () -> Unit = {},
    onOrganise: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(HomeTokens.cardCorner))
            .clickable(onClick = onOpen),
    ) {
        if (thumbnailPath == null) {
            com.donnclab.kinetiframe.actors.ui.rigging.RigBlueprintThumbnail(
                com.donnclab.kinetiframe.actors.rig.model.ActorTemplate.HUMAN,
                Modifier.fillMaxSize().padding(24.dp),
            )
        } else {
            AsyncImage(
                model = "file://$thumbnailPath",
                contentDescription = "${actor.displayName} actor preview",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(top = 28.dp, bottom = 40.dp, start = 12.dp, end = 12.dp),
            )
        }
        HomeChip(
            "${actor.authoredViews().size} views",
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp),
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(ScrimBrush)
                .padding(top = 26.dp, start = 10.dp, end = 40.dp, bottom = 8.dp),
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
        Box(Modifier.align(Alignment.BottomEnd).padding(2.dp)) {
            IconButton(onClick = {
                menuOpen = true
            }) { Icon(Icons.Default.MoreVert, "Actor actions", tint = Color.White) }
            IconContextMenu(
                menuOpen,
                { menuOpen = false },
                listOf(
                    ContextIconAction(Icons.Default.Star, "Favourite", {
                        menuOpen = false
                        onFavourite()
                    }, selected = favourite),
                    ContextIconAction(Icons.Default.Folder, "Collection", {
                        menuOpen = false
                        onOrganise()
                    }),
                    ContextIconAction(Icons.Default.Edit, "Rename", {
                        menuOpen = false
                        onRename()
                    }),
                    ContextIconAction(Icons.Default.ContentCopy, "Duplicate", {
                        menuOpen = false
                        onDuplicate()
                    }),
                    ContextIconAction(Icons.Default.LibraryAdd, "Library", {
                        menuOpen = false
                        onExport()
                    }),
                    ContextIconAction(Icons.Default.Archive, "Archive", {
                        menuOpen = false
                        onArchive()
                    }),
                ),
            )
        }
    }
}
