package com.donnclab.kinetiframe.actors.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.actors.rig.model.ActorTemplate
import com.donnclab.kinetiframe.common.ui.AddFlowBottomBar
import com.donnclab.kinetiframe.common.ui.AddFlowTopBar
import com.donnclab.kinetiframe.common.ui.CompactTextField

/**
 * Home's "New Actor" flow: a name plus a starting skeleton template. This is the only place a
 * template is chosen; an actor's template is fixed when it is created.
 *
 * Presented full-screen via [com.donnclab.kinetiframe.common.ui.FullScreenDialog] rather than
 * an [androidx.compose.material3.AlertDialog], matching the New Project / New Pack screens.
 */
@Composable
fun AddActorScreen(
    onBack: () -> Unit,
    onCreate: (name: String, template: ActorTemplate) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by remember { mutableStateOf("") }
    var template by remember { mutableStateOf(ActorTemplate.HUMAN) }

    Scaffold(
        modifier = modifier,
        topBar = { AddFlowTopBar(title = "New Actor", onBack = onBack) },
        bottomBar = {
            AddFlowBottomBar(buttonLabel = "Create Actor", enabled = name.isNotBlank(), onClick = {
                onCreate(name, template)
            })
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            CompactTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Actor name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
            )
            Text(
                "Starting structure",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp, bottom = 8.dp),
            )
            TemplateOptionRow(
                template = ActorTemplate.HUMAN,
                description = "Paired limbs for figures. Choose the drawing view in Actor Studio.",
                selected = template == ActorTemplate.HUMAN,
                onClick = { template = ActorTemplate.HUMAN },
            )
            Spacer(Modifier.height(10.dp))
            TemplateOptionRow(
                template = ActorTemplate.ANIMAL,
                description = "A horizontal body with four linked supports and a tail.",
                selected = template == ActorTemplate.ANIMAL,
                onClick = { template = ActorTemplate.ANIMAL },
            )
            Spacer(Modifier.height(10.dp))
            TemplateOptionRow(
                template = ActorTemplate.CUSTOM,
                description = "One root. Add branches, joints and endpoints yourself.",
                selected = template == ActorTemplate.CUSTOM,
                onClick = { template = ActorTemplate.CUSTOM },
            )
            Spacer(Modifier.height(10.dp))
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview
@Composable
private fun AddActorScreenPreview() = AddActorScreen(onBack = {}, onCreate = { _, _ -> })
