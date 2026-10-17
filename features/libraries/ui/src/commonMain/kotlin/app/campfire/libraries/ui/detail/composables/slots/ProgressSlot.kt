// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Headphones
import app.campfire.common.compose.icons.rounded.MenuBook
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.MediaProgressBar
import kotlin.time.Duration

class ProgressSlot(
  val isPlaying: Boolean,
  val playbackSpeed: Float,
  @get:VisibleForTesting val mediaProgress: MediaProgress,
  val libraryItem: LibraryItem,
  /** The chapter of an ebook being read, where its contents are known. */
  @get:VisibleForTesting val chapter: String? = null,
  @get:VisibleForTesting val kind: Kind = Kind.Item,
) : ContentSlot {

  /**
   * Which progress the bar shows. An item with both audio and an ebook shows a bar for each, as
   * neither position says anything about the other.
   */
  enum class Kind {
    /** How far through the item, for an item that's only one or the other. */
    Item,

    /** How far through the item's audio. */
    Listening,

    /** How far through the item's ebook. */
    Reading,
  }

  override val id: String = when (kind) {
    Kind.Reading -> "reading_progress"
    else -> "media_progress"
  }

  @Composable
  override fun Content(modifier: Modifier, eventSink: (LibraryItemUiEvent) -> Unit) {
    val barModifier = modifier.padding(horizontal = 20.dp)
    when (kind) {
      Kind.Item -> MediaProgressBar(
        isPlaying = isPlaying,
        playbackSpeed = playbackSpeed,
        progress = mediaProgress,
        totalDuration = libraryItem.media.duration,
        chapter = chapter,
        modifier = barModifier,
      )

      Kind.Listening -> MediaProgressBar(
        isPlaying = isPlaying,
        playbackSpeed = playbackSpeed,
        progress = mediaProgress.copy(ebookProgress = null),
        totalDuration = libraryItem.media.duration,
        leadingIcon = CampfireIcons.Rounded.Headphones,
        modifier = barModifier,
      )

      // With no length, the bar reads as an ebook's: its reading place and when it was last read
      Kind.Reading -> MediaProgressBar(
        isPlaying = false,
        playbackSpeed = 1f,
        progress = mediaProgress.copy(duration = 0f, currentTime = 0f, progress = 0f),
        totalDuration = Duration.ZERO,
        chapter = chapter,
        leadingIcon = CampfireIcons.Rounded.MenuBook,
        modifier = barModifier,
      )
    }
  }
}
