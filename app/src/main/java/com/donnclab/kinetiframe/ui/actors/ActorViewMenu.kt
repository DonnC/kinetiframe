package com.donnclab.kinetiframe.ui.actors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.engine.rigging.model.ActorView

/** Views are explicit drawings. No turn/rotate affordance implies synthesized unseen artwork. */
@Composable
fun ActorViewMenu(
    current: ActorView,
    views: Set<ActorView>,
    pinned: Boolean,
    canCreate: Boolean,
    onSelect: (ActorView) -> Unit,
    onCreate: (ActorView) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var adding by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = {
            expanded = true
            adding = false
        }, modifier = Modifier.size(40.dp)) {
            Icon(Icons.Default.Flip, "Actor views: ${current.title}", modifier = Modifier.size(18.dp))
        }
        DropdownMenu(expanded, { expanded = false }) {
            if (adding) {
                DropdownMenuItem(text = { Text("New authored view") }, enabled = false, onClick = {})
                ActorView.entries.filter { it != ActorView.ORIGINAL && it !in views }.forEach { view ->
                    DropdownMenuItem(
                        text = { Text(view.title) },
                        leadingIcon = { Icon(Icons.Default.Add, null) },
                        onClick = {
                            expanded = false
                            onCreate(view)
                        },
                    )
                }
            } else {
                views.forEach { view ->
                    DropdownMenuItem(
                        text = { Text(view.title) },
                        enabled = !pinned,
                        trailingIcon = { if (view == current) Icon(Icons.Default.Check, null) },
                        onClick = {
                            expanded = false
                            if (view != current) onSelect(view)
                        },
                    )
                }
                if (canCreate && ActorView.entries.any { it != ActorView.ORIGINAL && it !in views }) {
                    HorizontalDivider()
                    DropdownMenuItem(text = {
                        Text("Add view")
                    }, leadingIcon = { Icon(Icons.Default.Add, null) }, onClick = {
                        adding =
                            true
                    })
                }
            }
        }
    }
}

@Preview
@Composable
private fun ActorViewMenuPreview() {
    MaterialTheme { ActorViewMenu(ActorView.FRONT, setOf(ActorView.FRONT, ActorView.PROFILE), false, true, {}, {}) }
}
