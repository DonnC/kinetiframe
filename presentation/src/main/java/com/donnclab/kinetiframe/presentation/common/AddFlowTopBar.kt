package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Shared header for the Home "Add" flows (New Project, New Pack, New Actor): a back button and
 *  a title, matching each other exactly since these screens are presented back-to-back off the
 *  same "+" action. */
@Composable
fun AddFlowTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth()) {
        AppBrandBar()
        Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier.fillMaxWidth().padding(8.dp)) {
            IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
            Text(title, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Preview
@Composable
private fun AddFlowTopBarPreview() = AddFlowTopBar(title = "New Project", onBack = {})
