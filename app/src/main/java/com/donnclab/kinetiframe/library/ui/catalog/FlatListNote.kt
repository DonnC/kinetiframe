package com.donnclab.kinetiframe.library.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.common.strings.FeatureStrings
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** The small explainer banner shown atop the Library Packs tab, saying why
 * there is no secondary filter row here (packs are a deliberately flat list). */
@Composable
fun FlatListNote(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .padding(horizontal = Dimens.spaceXl, vertical = Dimens.spaceS)
            .clip(RoundedCornerShape(Dimens.radiusM))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            .padding(Dimens.spaceM),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceS),
    ) {
        Icon(Icons.Filled.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Preview(showBackground = true)
@Composable
private fun FlatListNotePreview() {
    KinetiFrameTheme { FlatListNote(FeatureStrings.Library.PACKS_FLAT_NOTE) }
}
