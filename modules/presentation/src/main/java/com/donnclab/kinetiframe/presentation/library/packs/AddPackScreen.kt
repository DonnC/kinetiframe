package com.donnclab.kinetiframe.presentation.library.packs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.presentation.R
import com.donnclab.kinetiframe.presentation.common.AddFlowBottomBar
import com.donnclab.kinetiframe.presentation.common.AddFlowTopBar
import com.donnclab.kinetiframe.presentation.common.CompactTextField
import com.donnclab.kinetiframe.presentation.studio.dialogs.SelectionDot

/**
 * Home's "New Pack" flow: a name plus a square canvas size -- collected together up front, unlike
 * [PackCreatorRoute]'s older size-first-then-name-at-save-time flow, so a pack creator sees and
 * commits to both choices before drawing ever starts. Presented full-screen via
 * [com.donnclab.kinetiframe.presentation.common.FullScreenDialog], matching the New Project / New Actor
 * screens.
 */
@Composable
fun AddPackScreen(state: AddPackFormState, callbacks: AddPackCallbacks, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = { AddFlowTopBar(title = stringResource(R.string.pack_new_title), onBack = callbacks.onBack) },
        bottomBar = {
            AddFlowBottomBar(
                buttonLabel = stringResource(R.string.pack_create),
                enabled = state.name.isNotBlank(),
                onClick = callbacks.onCreate,
                subtitle = stringResource(R.string.pack_size_square, state.dimension),
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(
                padding,
            ).verticalScroll(rememberScrollState()).padding(horizontal = Dimens.spaceXl),
        ) {
            CompactTextField(
                value = state.name,
                onValueChange = callbacks.onNameChange,
                label = { Text(stringResource(R.string.pack_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = Dimens.spaceXl),
            )
            Text(
                stringResource(R.string.pack_canvas_size),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = Dimens.spaceXlPlus, bottom = Dimens.spaceS),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.spaceM), modifier = Modifier.fillMaxWidth()) {
                PACK_SIZES.forEach { (size, tag) ->
                    PackSizeCard(
                        size = size,
                        tag = stringResource(tag),
                        selected = size == state.dimension,
                        onClick = { callbacks.onDimensionChange(size) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            PackSizePreview(state.dimension, Modifier.padding(top = Dimens.spaceXl))
            Spacer(Modifier.height(Dimens.spaceXlPlus))
        }
    }
}

/**
 * How big each pack item is drawn, against the largest size: the chosen square sits inside a
 * dashed 1024 outline, with a sample sticker, so Small, Medium and Large mean something.
 */
@Composable
private fun PackSizePreview(dimension: Int, modifier: Modifier = Modifier) {
    val largest = PACK_SIZES.maxOf { it.first }.toFloat()
    val outline = MaterialTheme.colorScheme.outline
    val fill = MaterialTheme.colorScheme.primaryContainer
    val ink = MaterialTheme.colorScheme.primary
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        androidx.compose.foundation.Canvas(Modifier.fillMaxWidth(PREVIEW_WIDTH).aspectRatio(1f)) {
            val dash = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(DASH, DASH))
            drawRect(outline, style = androidx.compose.ui.graphics.drawscope.Stroke(STROKE, pathEffect = dash))
            val side = size.width * (dimension / largest)
            val topLeft = androidx.compose.ui.geometry.Offset((size.width - side) / 2, (size.height - side) / 2)
            drawRect(fill, topLeft, androidx.compose.ui.geometry.Size(side, side))
            // A sample sticker: a round face, so the scale reads as "how big a drawing is".
            val centre = androidx.compose.ui.geometry.Offset(size.width / 2, size.height / 2)
            drawCircle(ink, side * FACE, centre, style = androidx.compose.ui.graphics.drawscope.Stroke(STROKE))
            drawCircle(ink, side * EYE, centre + androidx.compose.ui.geometry.Offset(-side * EYE_GAP, -side * EYE_GAP))
            drawCircle(ink, side * EYE, centre + androidx.compose.ui.geometry.Offset(side * EYE_GAP, -side * EYE_GAP))
        }
        Text(
            stringResource(R.string.pack_size_preview, dimension),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = Dimens.spaceS),
        )
    }
}

private const val PREVIEW_WIDTH = 0.6f
private const val DASH = 12f
private const val STROKE = 3f
private const val FACE = 0.3f
private const val EYE = 0.035f
private const val EYE_GAP = 0.1f

/** The three square sizes New Pack offers, each paired with its S/M/L shorthand. */
private val PACK_SIZES =
    listOf(512 to R.string.pack_size_small, 768 to R.string.pack_size_medium, 1024 to R.string.pack_size_large)

/** One selectable square-size option: its S/M/L shorthand, its exact pixel size, and a
 *  [SelectionDot]. */
@Composable
private fun PackSizeCard(
    size: Int,
    tag: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(Dimens.radiusL),
        modifier = modifier.aspectRatio(1f),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            Dimens.borderMedium,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(Modifier.fillMaxSize().padding(Dimens.spaceM), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { SelectionDot(selected) }
            Spacer(Modifier.height(Dimens.spaceM))
            Text(tag, style = MaterialTheme.typography.headlineSmall)
            Text(
                stringResource(R.string.pack_size_square, size),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Preview
@Composable
private fun AddPackScreenPreview() = AddPackScreen(
    state = AddPackFormState(name = "My Pack"),
    callbacks = AddPackCallbacks(onNameChange = {}, onDimensionChange = {}, onBack = {}, onCreate = {}),
)
