package com.donnclab.kinetiframe.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.presentation.R

/** The app bar for Home's "Add" flows (New Project, New Pack, New Actor): back and the task's title. */
@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun AddFlowTopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier) {
    TopAppBar(
        title = { Text(title) },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
        },
        modifier = modifier,
    )
}

@Preview
@Composable
private fun AddFlowTopBarPreview() = AddFlowTopBar(title = "New Project", onBack = {})
