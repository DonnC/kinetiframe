package com.donnclab.kinetiframe.project.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Card
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.core.document.StageDimensions
import com.donnclab.kinetiframe.project.data.ProjectManifest
import com.donnclab.kinetiframe.project.data.ProjectRepository

/** One dashboard card. Shows the project's real frame-0 thumbnail once one has been generated
 *  (see [ProjectRepository.thumbnailFile] and EditorScreen's
 *  onSaveProjectClick) -- a plain colored surface is the fallback for a project that predates
 *  thumbnails or hasn't been saved since. */
@Composable
fun ProjectCard(
    project: ProjectManifest,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit = {},
    onSaveToLibrary: () -> Unit = {},
    onExportVideo: () -> Unit = {},
) {
    var menuOpen by remember { mutableStateOf(false) }
    Card(onClick = onOpen, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(8.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(project.stage.width.toFloat() / project.stage.height.toFloat())) {
                if (project.thumbnailPath != null) {
                    AsyncImage(
                        model = project.thumbnailPath,
                        contentDescription = project.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxWidth().aspectRatio(
                            project.stage.width.toFloat() / project.stage.height.toFloat(),
                        ),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                    ) {}
                }
                Box(Modifier.align(Alignment.TopEnd)) {
                    IconButton(onClick = { menuOpen = true }) { Icon(Icons.Default.MoreVert, "Project actions") }
                    DropdownMenu(menuOpen, { menuOpen = false }) {
                        DropdownMenuItem(text = { Text("Rename") }, onClick = {
                            menuOpen = false
                            onRename()
                        })
                        DropdownMenuItem(text = { Text("Export Video") }, onClick = {
                            menuOpen = false
                            onExportVideo()
                        })
                        DropdownMenuItem(text = { Text("Share (.kframe)") }, onClick = {
                            menuOpen = false
                            onShare()
                        })
                        DropdownMenuItem(text = { Text("Save to Library") }, onClick = {
                            menuOpen = false
                            onSaveToLibrary()
                        })
                        DropdownMenuItem(text = { Text("Delete") }, onClick = {
                            menuOpen = false
                            onDelete()
                        })
                    }
                }
            }
            Text(project.displayName, style = MaterialTheme.typography.titleSmall, maxLines = 1)
            Text(
                "${project.stage.width}×${project.stage.height} · ${project.fps} fps",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Preview(widthDp = 180)
@Composable
private fun ProjectCardPreview() = ProjectCard(
    ProjectManifest(
        projectId = "project-1",
        displayName = "My Cartoon",
        stage = StageDimensions.LandscapeHd,
        fps = 12,
        createdAtEpochMs = 1,
        updatedAtEpochMs = 1,
    ),
    {},
    {},
    {},
)
