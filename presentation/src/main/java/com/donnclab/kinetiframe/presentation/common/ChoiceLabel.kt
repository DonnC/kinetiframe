package com.donnclab.kinetiframe.presentation.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.presentation.R

/** A choice button's [label], marked with a bullet when it is the [selected] one. */
@Composable
@ReadOnlyComposable
fun choiceLabel(label: String, selected: Boolean): String =
    if (selected) stringResource(R.string.choice_selected, label) else label
