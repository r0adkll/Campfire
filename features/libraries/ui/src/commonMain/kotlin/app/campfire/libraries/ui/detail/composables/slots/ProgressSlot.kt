// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.Headphones
import app.campfire.common.compose.icons.rounded.MenuBook
import app.campfire.common.compose.icons.rounded.Warning
import app.campfire.common.compose.widgets.dialog.CampfireDatePickerDialog
import app.campfire.core.extensions.asDate
import app.campfire.core.extensions.readableFormat
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.MediaProgress
import app.campfire.libraries.ui.detail.LibraryItemUiEvent
import app.campfire.libraries.ui.detail.composables.EditableInset
import app.campfire.libraries.ui.detail.composables.MediaProgressBar
import app.campfire.libraries.ui.detail.dates.ListeningDate
import app.campfire.libraries.ui.detail.dates.RecreateProgressDialog
import app.campfire.libraries.ui.detail.dates.showListeningDatesSheet
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.listening_dates_finished_picker_earliest
import campfire.features.libraries.ui.generated.resources.listening_dates_finished_picker_title
import campfire.features.libraries.ui.generated.resources.listening_dates_not_restored
import campfire.features.libraries.ui.generated.resources.listening_dates_not_restored_retry
import campfire.features.libraries.ui.generated.resources.listening_dates_started_picker_latest
import campfire.features.libraries.ui.generated.resources.listening_dates_started_picker_title
import com.slack.circuit.overlay.LocalOverlayHost
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

class ProgressSlot(
  val isPlaying: Boolean,
  val playbackSpeed: Float,
  @get:VisibleForTesting val mediaProgress: MediaProgress,
  val libraryItem: LibraryItem,
  /** The chapter of an ebook being read, where its contents are known. */
  @get:VisibleForTesting val chapter: String? = null,
  @get:VisibleForTesting val kind: Kind = Kind.Item,
  /** Whether tapping the bar opens the progress's listening dates, for finished progress. */
  @get:VisibleForTesting val canEditDates: Boolean = false,
  /** The listening date being picked, if any. */
  @get:VisibleForTesting val editingDate: ListeningDate? = null,
  /** Whether the start can be changed now, which it can't while the item plays. */
  @get:VisibleForTesting val canChangeStarted: Boolean = false,
  /** The day picked for the start, waiting on the user to agree to recreating the progress. */
  @get:VisibleForTesting val confirmingStartedOn: LocalDate? = null,
  /** Whether changing the start left the server without this progress, to try putting it back. */
  @get:VisibleForTesting val progressNotRestored: Boolean = false,
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
    Column(modifier) {
      Bar(eventSink)
      if (progressNotRestored) {
        NotRestoredRow(
          onRetry = { eventSink(LibraryItemUiEvent.RestoreProgress) },
          modifier = Modifier.padding(horizontal = BarInset),
        )
      }
    }

    if (!canEditDates) return
    when (editingDate) {
      ListeningDate.Finished -> FinishedDatePicker(eventSink)
      ListeningDate.Started -> StartedDatePicker(eventSink)
      null -> Unit
    }
    confirmingStartedOn?.let { startedOn ->
      RecreateProgressDialog(
        from = mediaProgress.startedAt.takeIf { it > 0L }?.asDate(),
        to = startedOn,
        onConfirm = { eventSink(LibraryItemUiEvent.ConfirmStartedDate) },
        onDismissRequest = { eventSink(LibraryItemUiEvent.DismissListeningDate) },
      )
    }
  }

  @Composable
  private fun Bar(eventSink: (LibraryItemUiEvent) -> Unit) {
    // An editable bar's touch target reaches past it, so it's inset less to keep the bar in place
    val barModifier = Modifier.padding(horizontal = if (canEditDates) BarInset - EditableInset else BarInset)
    when (kind) {
      Kind.Item -> MediaProgressBar(
        isPlaying = isPlaying,
        playbackSpeed = playbackSpeed,
        progress = mediaProgress,
        totalDuration = libraryItem.media.duration,
        chapter = chapter,
        onEditDates = if (canEditDates) rememberEditDates(eventSink) else null,
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
  private fun rememberEditDates(eventSink: (LibraryItemUiEvent) -> Unit): () -> Unit {
    val overlayHost = LocalOverlayHost.current
    val scope = rememberCoroutineScope()
    return {
      scope.launch {
        overlayHost.showListeningDatesSheet(libraryItem.media.metadata.title, mediaProgress, canChangeStarted)
          ?.let { eventSink(LibraryItemUiEvent.EditListeningDate(it)) }
      }
    }
  }

  @Composable
  private fun StartedDatePicker(eventSink: (LibraryItemUiEvent) -> Unit) {
    val today = Clock.System.now().toEpochMilliseconds().asDate()
    val finishedOn = mediaProgress.finishedAt?.asDate()?.takeIf { it <= today }
    CampfireDatePickerDialog(
      title = stringResource(Res.string.listening_dates_started_picker_title),
      initialDate = mediaProgress.startedAt.takeIf { it > 0L }?.asDate() ?: finishedOn ?: today,
      latest = finishedOn ?: today,
      supportingText = finishedOn?.let {
        stringResource(Res.string.listening_dates_started_picker_latest, it.readableFormat)
      },
      onConfirm = { eventSink(LibraryItemUiEvent.ChangeListeningDate(ListeningDate.Started, it)) },
      onDismissRequest = { eventSink(LibraryItemUiEvent.DismissListeningDate) },
    )
  }

  @Composable
  private fun FinishedDatePicker(eventSink: (LibraryItemUiEvent) -> Unit) {
    val today = Clock.System.now().toEpochMilliseconds().asDate()
    val startedOn = mediaProgress.startedAt.takeIf { it > 0L }?.asDate()?.takeIf { it <= today }
    CampfireDatePickerDialog(
      title = stringResource(Res.string.listening_dates_finished_picker_title),
      initialDate = mediaProgress.finishedAt?.asDate() ?: today,
      earliest = startedOn,
      latest = today,
      supportingText = startedOn?.let {
        stringResource(Res.string.listening_dates_finished_picker_earliest, it.readableFormat)
      },
      onConfirm = { eventSink(LibraryItemUiEvent.ChangeListeningDate(ListeningDate.Finished, it)) },
      onDismissRequest = { eventSink(LibraryItemUiEvent.DismissListeningDate) },
    )
  }
}

@Composable
private fun NotRestoredRow(
  onRetry: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    modifier = modifier
      .fillMaxWidth()
      .padding(top = 8.dp)
      .testTag("progress_not_restored"),
  ) {
    Icon(
      CampfireIcons.Rounded.Warning,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.error,
      modifier = Modifier.size(18.dp),
    )
    Text(
      text = stringResource(Res.string.listening_dates_not_restored),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.error,
      modifier = Modifier.weight(1f),
    )
    TextButton(onClick = onRetry) {
      Text(stringResource(Res.string.listening_dates_not_restored_retry))
    }
  }
}

private val BarInset = 20.dp
