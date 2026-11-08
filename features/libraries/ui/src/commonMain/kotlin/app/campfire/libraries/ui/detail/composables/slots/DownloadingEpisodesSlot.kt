// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.theme.CampfireTheme
import app.campfire.common.compose.widgets.ServerDownloadsBanner
import app.campfire.libraries.ui.detail.LibraryItemUiEvent

/**
 * Header banner on the podcast detail screen showing how many episodes are currently in the
 * server's download queue for this podcast. Tapping opens the server's download queue.
 *
 * Only rendered when [count] > 0 (PodcastPresenter omits the slot otherwise).
 */
class DownloadingEpisodesSlot(
  private val count: Int,
) : ContentSlot {

  override val id: String = "downloading_episodes"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    ServerDownloadsBanner(
      count = count,
      onClick = { eventSink(LibraryItemUiEvent.OpenServerDownloadQueue) },
      modifier = modifier.padding(horizontal = 16.dp),
    )
  }
}

@Preview
@Composable
private fun DownloadingEpisodesSlotPreview_Single() = CampfireTheme {
  DownloadingEpisodesSlot(count = 1).Content(Modifier) {}
}

@Preview
@Composable
private fun DownloadingEpisodesSlotPreview_Multiple() = CampfireTheme {
  DownloadingEpisodesSlot(count = 4).Content(Modifier) {}
}
