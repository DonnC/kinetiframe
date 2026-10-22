package com.donnclab.kinetiframe.studio.ui.timeline

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

/** Non-interactive playhead: gestures pass through to the thumbnails beneath it. */
@Composable
internal fun TimelineCenterPointer(modifier: Modifier = Modifier) {
    Spacer(
        modifier.size(10.dp, 7.dp).semantics { contentDescription = "Center frame playhead" }
            .drawWithCache {
                val arrow = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2, size.height)
                    close()
                }
                onDrawBehind { drawPath(arrow, Color(0xFF5796F5)) }
            },
    )
}

@Preview(showBackground = true)
@Composable
private fun CenterPointerPreview() {
    TimelineCenterPointer()
}
