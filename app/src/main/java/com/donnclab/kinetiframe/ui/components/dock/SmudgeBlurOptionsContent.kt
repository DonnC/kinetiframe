package com.donnclab.kinetiframe.ui.components.dock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.donnclab.kinetiframe.engine.model.DrawingTool
import com.donnclab.kinetiframe.ui.StudioIntent
import com.donnclab.kinetiframe.ui.StudioState
import com.donnclab.kinetiframe.ui.ToolConfig
import com.donnclab.kinetiframe.ui.components.StandardSlider

/** Size is the only user-facing control; transfer/softening are tuned in ManipulationDefaults. */
@Composable
fun SmudgeBlurOptionsContent(state: StudioState, tool: DrawingTool, onIntent: (StudioIntent) -> Unit) {
    val config = state.tool.configs[tool] ?: ToolConfig()
    val toolName = if (tool == DrawingTool.SMUDGE) "Smudge" else "Blur"

    Column(modifier = Modifier.padding(12.dp).fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(toolName, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        StandardSlider(
            label = "Size",
            value = config.size,
            onValueChange = { onIntent(StudioIntent.Draw.SetBrushSize(it)) },
            range = 1f..100f,
        )
    }
}

@androidx.compose.ui.tooling.preview.Preview
@Composable
private fun SmudgeOptionsPreview() {
    SmudgeBlurOptionsContent(StudioState(), DrawingTool.SMUDGE, {})
}
