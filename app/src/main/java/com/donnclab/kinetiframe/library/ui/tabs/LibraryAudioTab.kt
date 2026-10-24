package com.donnclab.kinetiframe.library.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.donnclab.kinetiframe.R
import com.donnclab.kinetiframe.designsystem.theme.Dimens
import com.donnclab.kinetiframe.designsystem.theme.KinetiFrameTheme
import com.donnclab.kinetiframe.library.ui.LibraryIntent
import com.donnclab.kinetiframe.library.ui.catalog.AudioTrackRow
import com.donnclab.kinetiframe.library.ui.catalog.FilterChipRow
import com.donnclab.kinetiframe.presentation.common.AudioTrackSummary
import com.donnclab.kinetiframe.preview.PreviewCatalog

private val filters = listOf(
    R.string.filter_all,
    R.string.library_filter_sound_effects,
    R.string.library_filter_music_tracks,
    R.string.library_filter_voice_clips,
)

/** Library's "Audio Track Box" tab -- the user's own sound effects, music and voice clips,
 * narrowed by [selectedFilter]. */
@Composable
fun LibraryAudioTab(tracks: List<AudioTrackSummary>, selectedFilter: Int, onIntent: (LibraryIntent) -> Unit) {
    val wantedTag = when (selectedFilter) {
        1 -> "SFX"
        2 -> "MUSIC"
        3 -> "VOICE"
        else -> null
    }
    val filtered = tracks.filter { wantedTag == null || it.tagLabel == wantedTag }
    Column {
        FilterChipRow(filters, selectedFilter, onSelect = { onIntent(LibraryIntent.SelectFilter(it)) })
        if (filtered.isEmpty()) {
            com.donnclab.kinetiframe.library.ui.LibraryEmptyState()
            return@Column
        }
        LazyColumn(
            contentPadding = PaddingValues(horizontal = Dimens.spaceXl, vertical = Dimens.spaceS),
            verticalArrangement = Arrangement.spacedBy(Dimens.spaceM),
        ) {
            items(filtered, key = {
                it.id
            }) { track -> AudioTrackRow(track, onClick = { onIntent(LibraryIntent.OpenDetails(track.details)) }) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryAudioTabPreview() {
    KinetiFrameTheme { LibraryAudioTab(PreviewCatalog.libraryAudio(), selectedFilter = 2, onIntent = {}) }
}
