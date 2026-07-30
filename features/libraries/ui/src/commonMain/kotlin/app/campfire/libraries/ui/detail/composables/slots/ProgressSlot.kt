// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.MediaProgressBar

class ProgressSlot(
  val isPlaying: Boolean,
  val playbackSpeed: Float,
  @get:VisibleForTesting val mediaProgress: MediaProgress,
  val libraryItem: LibraryItem,
) : ContentSlot {

  override val id: String = "media_progress"

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    MediaProgressBar(
      isPlaying = isPlaying,
      playbackSpeed = playbackSpeed,
      progress = mediaProgress,
      totalDuration = libraryItem.media.duration,
      modifier = modifier
        .padding(
          horizontal = 20.dp,
        ),
    )
  }
}
