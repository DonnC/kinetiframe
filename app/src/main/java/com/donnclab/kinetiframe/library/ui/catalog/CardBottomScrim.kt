package com.donnclab.kinetiframe.library.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/**
 * The translucent-black-to-transparent gradient a grid card's title/subtitle sits on, anchored to
 * the bottom of whatever [Box] this is called inside (the card's thumbnail area). [footer] is an
 * optional extra line below the subtitle. One place to keep this treatment so every card --
 * Projects, Actors, Packs -- reads as the same design system.
 */
@Composable
fun BoxScope.CardBottomScrim(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    footer: @Composable ColumnScope.() -> Unit = {},
) {
    Box(
        modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.76f))))
            .padding(horizontal = Dimens.spaceS, vertical = Dimens.spaceM),
    ) {
        Column {
            Text(title, style = MaterialTheme.typography.labelLarge, color = Color.White, maxLines = 1)
            Text(
                subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.85f),
                maxLines = 1,
            )
            footer()
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CardBottomScrimPreview() {
    KinetiFrameTheme {
        Box(Modifier.aspectRatio(1.3f).background(Color(0xFFFF6A45))) {
            CardBottomScrim(title = "Spidey Web Swing", subtitle = "@Animator_Lagos")
        }
    }
}
