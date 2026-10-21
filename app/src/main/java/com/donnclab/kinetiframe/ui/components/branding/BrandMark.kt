package com.donnclab.kinetiframe.ui.components.branding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.R
import com.donnclab.kinetiframe.designsystem.theme.BrandIvory
import com.donnclab.kinetiframe.designsystem.theme.BrandTeal
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme

/** Untinted multi-colour mark: Material Icon tint would erase the rig and motion accents. */
@Composable
fun BrandMark(modifier: Modifier = Modifier, onTeal: Boolean = false, contentDescription: String? = null) {
    Image(
        painterResource(if (onTeal) R.drawable.brand_approved_icon else R.drawable.brand_approved_mark),
        contentDescription,
        modifier,
    )
}

/** Original approved lettering, not a replacement system-font approximation. */
@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    // Ivory backing preserves the approved teal lettering in both appearance modes.
    Surface(modifier, color = BrandIvory, shape = RoundedCornerShape(4.dp)) {
        Image(
            painterResource(R.drawable.brand_approved_wordmark),
            contentDescription = null,
            modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp),
        )
    }
}

@Composable
fun BrandBadge(modifier: Modifier = Modifier) {
    Surface(modifier, shape = RoundedCornerShape(10.dp), color = BrandTeal) {
        Box(contentAlignment = Alignment.Center) {
            BrandMark(Modifier.fillMaxSize(), onTeal = true, contentDescription = stringResource(R.string.app_name))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun BrandMarkPreview() {
    KinetiFrameTheme {
        Surface {
            Row(Modifier.padding(16.dp)) {
                BrandMark(Modifier.size(96.dp))
                BrandBadge(Modifier.size(48.dp))
                BrandWordmark(Modifier.size(160.dp, 28.dp))
            }
        }
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BrandMarkDarkPreview() {
    KinetiFrameTheme(darkTheme = true) { Surface { BrandMark(Modifier.size(96.dp)) } }
}
