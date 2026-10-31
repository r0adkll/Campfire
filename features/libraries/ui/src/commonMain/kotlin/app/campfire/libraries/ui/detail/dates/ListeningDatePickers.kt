// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.dates

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.campfire.common.compose.widgets.dialog.CampfireDatePickerDialog
import app.campfire.core.extensions.asDate
import app.campfire.core.extensions.readableFormat
import app.campfire.core.model.MediaProgress
import campfire.features.libraries.ui.generated.resources.Res
import campfire.features.libraries.ui.generated.resources.listening_dates_finished_picker_earliest
import campfire.features.libraries.ui.generated.resources.listening_dates_finished_picker_title
import campfire.features.libraries.ui.generated.resources.listening_dates_started_picker_latest
import campfire.features.libraries.ui.generated.resources.listening_dates_started_picker_title
import com.slack.circuit.overlay.Overlay
import com.slack.circuit.overlay.OverlayHost
import com.slack.circuit.overlay.OverlayNavigator
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource

/** Asks for the day [progress] finished on, or null when the picker is put away without one. */
internal suspend fun OverlayHost.showFinishedDatePicker(
  progress: MediaProgress,
  today: LocalDate,
): LocalDate? {
  return (show(FinishedDatePickerOverlay(progress, today)) as? DatePicked.Day)?.day
}

/** Asks for the day [progress] started on, or null when the picker is put away without one. */
internal suspend fun OverlayHost.showStartedDatePicker(
  progress: MediaProgress,
  today: LocalDate,
): LocalDate? {
  return (show(StartedDatePickerOverlay(progress, today)) as? DatePicked.Day)?.day
}

internal sealed interface DatePicked {
  data class Day(val day: LocalDate) : DatePicked
  data object Dismissed : DatePicked
}

internal class FinishedDatePickerOverlay(
  private val progress: MediaProgress,
  private val today: LocalDate,
) : Overlay<DatePicked> {

  @Composable
  override fun Content(navigator: OverlayNavigator<DatePicked>) {
    FinishedDatePicker(
      progress = progress,
      today = today,
      onConfirm = { navigator.finish(DatePicked.Day(it)) },
      onDismissRequest = { navigator.finish(DatePicked.Dismissed) },
    )
  }
}

/** Picks the day [progress] finished on, no earlier than it started and no later than [today]. */
@Composable
internal fun FinishedDatePicker(
  progress: MediaProgress,
  today: LocalDate,
  onConfirm: (LocalDate) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val startedOn = progress.startedAt.takeIf { it > 0L }?.asDate()?.takeIf { it <= today }
  CampfireDatePickerDialog(
    title = stringResource(Res.string.listening_dates_finished_picker_title),
    initialDate = progress.finishedAt?.asDate() ?: today,
    earliest = startedOn,
    latest = today,
    supportingText = startedOn?.let {
      stringResource(Res.string.listening_dates_finished_picker_earliest, it.readableFormat)
    },
    onConfirm = onConfirm,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  )
}

internal class StartedDatePickerOverlay(
  private val progress: MediaProgress,
  private val today: LocalDate,
) : Overlay<DatePicked> {

  @Composable
  override fun Content(navigator: OverlayNavigator<DatePicked>) {
    StartedDatePicker(
      progress = progress,
      today = today,
      onConfirm = { navigator.finish(DatePicked.Day(it)) },
      onDismissRequest = { navigator.finish(DatePicked.Dismissed) },
    )
  }
}

/** Picks the day [progress] started on, no later than it finished or [today]. */
@Composable
internal fun StartedDatePicker(
  progress: MediaProgress,
  today: LocalDate,
  onConfirm: (LocalDate) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val finishedOn = progress.finishedAt?.asDate()?.takeIf { it <= today }
  CampfireDatePickerDialog(
    title = stringResource(Res.string.listening_dates_started_picker_title),
    initialDate = progress.startedAt.takeIf { it > 0L }?.asDate() ?: finishedOn ?: today,
    latest = finishedOn ?: today,
    supportingText = finishedOn?.let {
      stringResource(Res.string.listening_dates_started_picker_latest, it.readableFormat)
    },
    onConfirm = onConfirm,
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  )
}
