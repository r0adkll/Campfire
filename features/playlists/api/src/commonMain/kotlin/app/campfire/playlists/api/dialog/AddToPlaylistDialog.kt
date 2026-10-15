// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.playlists.api.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.core.model.LibraryItemId
import app.campfire.core.model.PlaylistId
import app.campfire.core.model.PodcastEpisode

sealed interface PlaylistDialogResult {
  data object None : PlaylistDialogResult
  data class Existing(val playlistId: PlaylistId) : PlaylistDialogResult
  data class New(val playlistId: PlaylistId) : PlaylistDialogResult
}

interface AddToPlaylistDialog {

  // No default values: since Kotlin 2.4.20, calling an interface composable's default from another
  // module fails to link on Kotlin/Native (CMP-9392)
  @Composable
  fun Content(
    libraryItemId: LibraryItemId,
    itemTitle: String,
    onDismiss: (PlaylistDialogResult) -> Unit,
    modifier: Modifier,
    episode: PodcastEpisode?,
  )

  companion object NoOp : AddToPlaylistDialog {
    @Composable
    override fun Content(
      libraryItemId: LibraryItemId,
      itemTitle: String,
      onDismiss: (PlaylistDialogResult) -> Unit,
      modifier: Modifier,
      episode: PodcastEpisode?,
    ) {
    }
  }
}
