package com.donnclab.kinetiframe.presentation.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/**
 * The app bar every root screen shares. With no [middle], it shows the logo and the KinetiFrame
 * wordmark. A screen with its own job in the bar, such as Library's search box, passes [middle]:
 * the bar then shows the logo only, then that content, so the screen never needs a second header.
 * [actions] sit at the end.
 */
@Composable
fun AppBrandBar(middle: (@Composable RowScope.() -> Unit)? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceSPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSPlus),
    ) {
        BrandBadge(Modifier.size(Dimens.brandBadge))
        if (middle != null) {
            Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, content = middle)
        } else {
            BrandWordmark(Modifier.size(Dimens.brandWordmarkWidth, Dimens.brandWordmarkHeight))
            Row(Modifier.weight(1f)) {}
        }
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBrandBarPreview() {
    KinetiFrameTheme { AppBrandBar() }
}
