package com.donnclab.kinetiframe.presentation.library.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** Small "SFX" / "MUSIC" / "VOICE" pill shown next to an audio row's duration. */
@Composable
fun AudioTag(label: String, modifier: Modifier = Modifier) {
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .clip(RoundedCornerShape(Dimens.radiusS))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
            .padding(horizontal = Dimens.spaceS, vertical = 1.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun AudioTagPreview() {
    KinetiFrameTheme { AudioTag("SFX") }
}
