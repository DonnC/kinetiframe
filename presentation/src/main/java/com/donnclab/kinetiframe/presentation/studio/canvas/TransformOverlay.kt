package com.donnclab.kinetiframe.presentation.studio.canvas

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.EditorTokens
import com.donnclab.kinetiframe.presentation.R

@Composable
fun TransformationOverlay(
    zoom: Float,
    offset: Offset,
    rotation: Float,
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(
                    top = EditorTokens.transformPanelTop,
                    end = Dimens.spaceL,
                    bottom = Dimens.spaceL,
                    start = Dimens.spaceL,
                )
                .statusBarsPadding(),
        ) {
            Surface(
                color = Color.Black.copy(alpha = 0.85f),
                shape = RoundedCornerShape(Dimens.radiusS),
            ) {
                Text(
                    text = stringResource(
                        R.string.canvas_transform_summary,
                        (zoom * 100).toInt(),
                        offset.x.toInt(),
                        offset.y.toInt(),
                        rotation.toInt(),
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = Dimens.spaceM, vertical = Dimens.spaceS),
                    style = MaterialTheme.typography.labelMedium,
                )
            }
        }
    }
}
