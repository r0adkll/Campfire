// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.dialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.LocalWindowSizeClass
import app.campfire.common.compose.layout.isHeightCompact
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.date_picker_dialog_confirm
import campfire.common.compose.generated.resources.date_picker_dialog_dismiss
import kotlin.time.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

/**
 * Material's date picker in a dialog, for picking a calendar day between [earliest] and [latest].
 *
 * The picker works in days rather than instants, so it takes and gives [LocalDate]s and leaves the
 * time of day to the caller. A window too short for the calendar opens on typing the date instead.
 *
 * It's laid out the way Material's `DatePickerDialog` is, but without that dialog's height cap,
 * which is sized to the picker alone and squeezes out anything under it.
 *
 * @param supportingText a line under the picker, such as why some days can't be picked
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CampfireDatePickerDialog(
  title: String,
  initialDate: LocalDate,
  onConfirm: (LocalDate) -> Unit,
  onDismissRequest: () -> Unit,
  modifier: Modifier = Modifier,
  earliest: LocalDate? = null,
  latest: LocalDate? = null,
  supportingText: String? = null,
) {
  val selectableDates = remember(earliest, latest) { DateRange(earliest, latest) }
  val isHeightCompact = LocalWindowSizeClass.current.isHeightCompact
  val state = rememberDatePickerState(
    initialSelectedDateMillis = initialDate.toPickerMillis(),
    yearRange = (earliest?.year ?: MIN_YEAR)..(latest?.year ?: MAX_YEAR),
    initialDisplayMode = if (isHeightCompact) DisplayMode.Input else DisplayMode.Picker,
    selectableDates = selectableDates,
  )

  BasicAlertDialog(
    onDismissRequest = onDismissRequest,
    modifier = modifier,
  ) {
    Surface(
      shape = DatePickerDefaults.shape,
      color = DatePickerDefaults.colors().containerColor,
      tonalElevation = DatePickerDefaults.TonalElevation,
      modifier = Modifier.requiredWidth(ContainerWidth),
    ) {
      Column {
        DatePicker(
          state = state,
          title = {
            Text(
              text = title,
              modifier = Modifier.padding(TitlePadding),
            )
          },
        )

        if (supportingText != null) {
          Text(
            text = supportingText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .padding(horizontal = 24.dp)
              .testTag("date_picker_supporting_text"),
          )
        }

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
          modifier = Modifier
            .fillMaxWidth()
            .padding(ButtonsPadding),
        ) {
          TextButton(onClick = onDismissRequest) {
            Text(stringResource(Res.string.date_picker_dialog_dismiss))
          }
          TextButton(
            enabled = state.selectedDateMillis != null,
            onClick = {
              state.selectedDateMillis?.let { onConfirm(it.toPickerDate()) }
            },
            modifier = Modifier.testTag("date_picker_confirm"),
          ) {
            Text(stringResource(Res.string.date_picker_dialog_confirm))
          }
        }
      }
    }
  }
}

/** The days from [earliest] to [latest], either end open when it's null. */
@OptIn(ExperimentalMaterial3Api::class)
private class DateRange(
  private val earliest: LocalDate?,
  private val latest: LocalDate?,
) : SelectableDates {

  override fun isSelectableDate(utcTimeMillis: Long): Boolean {
    val date = utcTimeMillis.toPickerDate()
    return (earliest == null || date >= earliest) && (latest == null || date <= latest)
  }

  override fun isSelectableYear(year: Int): Boolean {
    return (earliest == null || year >= earliest.year) && (latest == null || year <= latest.year)
  }
}

// The picker names a day by its midnight in UTC, whatever zone the device is in
private fun LocalDate.toPickerMillis(): Long = atStartOfDayIn(TimeZone.UTC).toEpochMilliseconds()

private fun Long.toPickerDate(): LocalDate = Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.UTC).date

// The insets and width Material gives its own date picker dialog
private val TitlePadding = PaddingValues(start = 24.dp, end = 12.dp, top = 16.dp)
private val ButtonsPadding = PaddingValues(top = 8.dp, bottom = 8.dp, end = 6.dp)
private val ContainerWidth = 360.dp

private const val MIN_YEAR = 1900
private const val MAX_YEAR = 2100
