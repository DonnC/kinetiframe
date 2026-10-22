package com.donnclab.kinetiframe.library.ui.catalog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.composables.icons.lucide.ClipboardPlus
import com.composables.icons.lucide.Lucide
import com.donnclab.kinetiframe.common.strings.FeatureStrings
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.DisabledContent
import com.donnclab.kinetiframe.designsystem.theme.DisabledSurface
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** Label + icon a [PrimaryActionButton] shows -- bundled together so the button itself stays a
 * 4-parameter function even though it needs both pieces of content. */
data class ActionButtonContent(val label: String, val icon: ImageVector)

/** Whether a [PrimaryActionButton] can be pressed. */
enum class ActionButtonState { ENABLED, DISABLED }

/** Full-width pill action button used at the bottom of the Asset Details Card and the pack
 * contents "Add pack to Library" bar, so its states look the same everywhere. */
@Composable
fun PrimaryActionButton(
    content: ActionButtonContent,
    state: ActionButtonState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when (state) {
        ActionButtonState.ENABLED -> MaterialTheme.colorScheme.primary
        ActionButtonState.DISABLED -> DisabledSurface
    }
    val onBackground = when (state) {
        ActionButtonState.ENABLED -> MaterialTheme.colorScheme.onPrimary
        ActionButtonState.DISABLED -> DisabledContent
    }
    Surface(
        onClick = onClick,
        enabled = state != ActionButtonState.DISABLED,
        modifier = modifier.fillMaxWidth().height(Dimens.actionButtonHeight),
        shape = RoundedCornerShape(Dimens.radiusL),
        color = background,
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(content.icon, contentDescription = null, tint = onBackground, modifier = Modifier.size(Dimens.iconM))
            Spacer(Modifier.width(Dimens.spaceS))
            Text(content.label, color = onBackground, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryActionButtonPreview() {
    KinetiFrameTheme {
        PrimaryActionButton(
            content = ActionButtonContent(FeatureStrings.PackContents.ADD_PACK_TO_LIBRARY, Lucide.ClipboardPlus),
            state = ActionButtonState.ENABLED,
            onClick = {},
        )
    }
}
