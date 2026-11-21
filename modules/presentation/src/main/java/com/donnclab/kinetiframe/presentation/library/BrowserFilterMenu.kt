package com.donnclab.kinetiframe.presentation.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.LibraryTokens

@Composable
internal fun BrowserFilterMenu(
    label: String,
    icon: ImageVector,
    options: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(Dimens.radiusS),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.height(LibraryTokens.controlHeight).padding(horizontal = Dimens.spaceS),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.spaceXsPlus),
            ) {
                Icon(icon, null, Modifier.size(Dimens.iconS))
                Text(
                    label,
                    Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(Icons.Default.ExpandMore, null, Modifier.size(Dimens.iconS))
            }
        }
        DropdownMenu(expanded, {
            expanded = false
        }, modifier = Modifier.heightIn(max = LibraryTokens.filterMenuMaxHeight)) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = { Text(option, style = MaterialTheme.typography.labelSmall) },
                    onClick = {
                        expanded = false
                        onSelect(index)
                    },
                    trailingIcon = {
                        if (index ==
                            selectedIndex
                        ) {
                            Icon(Icons.Default.Check, null, Modifier.size(Dimens.iconS))
                        }
                    },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrowserFilterPreview() {
    MaterialTheme {
        BrowserFilterMenu(
            "All categories",
            Icons.Default.Category,
            listOf("All categories", "Characters"),
            0,
            onSelect = {},
        )
    }
}
