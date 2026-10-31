// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Headphones
import app.campfire.common.compose.icons.rounded.MenuBook
import app.campfire.common.compose.layout.LocalSnackBarHost
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.EditableInset
import app.campfire.libraries.ui.detail.composables.MediaProgressBar
import app.campfire.libraries.ui.detail.dates.ListeningDatesEditor
import com.slack.circuit.overlay.LocalOverlayHost
import kotlin.time.Duration
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ProgressSlot(
  val isPlaying: Boolean,
  val playbackSpeed: Float,
  @get:VisibleForTesting val mediaProgress: MediaProgress,
  val libraryItem: LibraryItem,
  /** The chapter of an ebook being read, where its contents are known. */
  @get:VisibleForTesting val chapter: String? = null,
  @get:VisibleForTesting val kind: Kind = Kind.Item,
  /** Changes the listening dates of finished progress, which tapping the bar does where given. */
  private val listeningDatesEditor: ListeningDatesEditor? = null,
) : ContentSlot {

  /** Whether tapping the bar opens the progress's listening dates. */
  @get:VisibleForTesting
  val canEditDates: Boolean
    get() = listeningDatesEditor != null && kind == Kind.Item && mediaProgress.isFinished

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
    // An editable bar's touch target reaches past it, so it's inset less to keep the bar in place
    val barModifier = modifier.padding(horizontal = if (canEditDates) BarInset - EditableInset else BarInset)
    when (kind) {
      Kind.Item -> MediaProgressBar(
        isPlaying = isPlaying,
        playbackSpeed = playbackSpeed,
        progress = mediaProgress,
        totalDuration = libraryItem.media.duration,
        chapter = chapter,
        onEditDates = listeningDatesEditor?.takeIf { canEditDates }?.let { rememberEditDates(it) },
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

  @Composable
  private fun rememberEditDates(editor: ListeningDatesEditor): () -> Unit {
    val overlayHost = LocalOverlayHost.current
    val snackbarHost = LocalSnackBarHost.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf<Job?>(null) }
    return {
      // A second tap while the dates are open would only queue them up again behind the first
      if (editing?.isActive != true) {
        editing = scope.launch { editor.edit(libraryItem, null, overlayHost, snackbarHost) }
      }
    }
  }
}

private val BarInset = 20.dp
