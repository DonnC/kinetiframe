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

/** Consistent browse/create branding; tools remain contextual to the destination. */
@Composable
fun AppBrandBar(actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier.fillMaxWidth().padding(horizontal = Dimens.spaceL, vertical = Dimens.spaceSPlus),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.spaceSPlus),
        ) {
            BrandBadge(Modifier.size(Dimens.brandBadge))
            BrandWordmark(Modifier.size(Dimens.brandWordmarkWidth, Dimens.brandWordmarkHeight))
        }
        Row(content = actions)
    }
}

@Preview(showBackground = true)
@Composable
private fun AppBrandBarPreview() {
    KinetiFrameTheme { AppBrandBar() }
}
