package com.donnclab.kinetiframe.presentation.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.core.actor.document.LibraryScope
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.R

/**
 * The Project-vs-Personal save choice -- shown right before an export actually runs, so every
 * "Add to Library" action gets the same choice, with no separate "Vault" UI concept.
 * [itemLabel] names the specific thing being saved (e.g. "\"Voice-over\"", or
 * "this background image") so the dialog's body reads naturally either way.
 */
@Composable
fun LibraryScopeDialog(itemLabel: String, onDismiss: () -> Unit, onChoose: (LibraryScope) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.library_save_title)) },
        text = {
            Column(Modifier.padding(top = Dimens.spaceXs)) {
                Text(
                    stringResource(R.string.library_save_message, itemLabel),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onChoose(LibraryScope.PERSONAL)
            }) { Text(stringResource(R.string.library_scope_personal)) }
        },
        dismissButton = {
            TextButton(onClick = {
                onChoose(LibraryScope.PROJECT)
            }) { Text(stringResource(R.string.library_scope_project)) }
        },
    )
}
