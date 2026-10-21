package com.donnclab.kinetiframe.ui.actors

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.donnclab.kinetiframe.engine.rigging.model.ActorTemplate
import com.donnclab.kinetiframe.ui.components.rigging.RigBlueprintThumbnail

/** One selectable skeleton-template row for [AddActorScreen]: a reference-art thumbnail (blank
 *  for [ActorTemplate.CUSTOM], which has none), its name and description, and a trailing radio. */
@Composable
fun TemplateOptionRow(
    template: ActorTemplate,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        modifier = modifier,
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.5.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(14.dp)) {
            Box(
                Modifier.size(
                    52.dp,
                ).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
            ) {
                RigBlueprintThumbnail(template, Modifier.fillMaxSize().padding(5.dp))
            }
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(template.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

@Preview
@Composable
private fun TemplateOptionRowPreview() = TemplateOptionRow(
    template = ActorTemplate.HUMAN,
    description = "Biped rig with arms, legs & spine.",
    selected = true,
    onClick = {},
)
