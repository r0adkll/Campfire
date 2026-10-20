// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail.composables

import androidx.compose.runtime.Composable
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.series_book_position
import campfire.features.series.ui.generated.resources.series_sequence_position
import org.jetbrains.compose.resources.stringResource

private val BOOK_NUMBER_REGEX = "^\\d+(?:\\.\\d+)?$".toRegex()

/**
 * "Book 1.5 · 2021" — a series book's reading-order position and release year, with whichever
 * part is unknown left out. Null when neither is known. [position] is displayed as given, so
 * pass the formatted sequence (e.g. [app.campfire.core.model.SeriesSequence.formattedSequence]).
 *
 * Only a plain book number in a [bookNumbered] series reads as "Book 1.5"; dates
 * (`2026.10.10`) and free text (`prequel`) read as "#2026.10.10" instead. Decide [bookNumbered]
 * once per series (see [app.campfire.core.model.isDatedSeries]) so its books share one style.
 */
@Composable
internal fun seriesBookLabel(
  position: String?,
  year: String?,
  bookNumbered: Boolean = true,
): String? {
  val positionLabel = position
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.let {
      if (bookNumbered && BOOK_NUMBER_REGEX.matches(it)) {
        stringResource(Res.string.series_book_position, it)
      } else {
        stringResource(Res.string.series_sequence_position, it)
      }
    }
  return listOfNotNull(positionLabel, year?.takeIf { it.isNotBlank() })
    .joinToString(" · ")
    .ifEmpty { null }
}
