// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail.composables

import androidx.compose.runtime.Composable
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.series_book_position
import org.jetbrains.compose.resources.stringResource

/**
 * "Book 1.5 · 2021" — a series book's reading-order position and release year, with whichever
 * part is unknown left out. Null when neither is known. [position] is displayed as given, so
 * pass the formatted sequence (e.g. [app.campfire.core.model.SeriesSequence.formattedSequence]).
 */
@Composable
internal fun seriesBookLabel(
  position: String?,
  year: String?,
): String? {
  val positionLabel = position
    ?.takeIf { it.isNotBlank() }
    ?.let { stringResource(Res.string.series_book_position, it) }
  return listOfNotNull(positionLabel, year?.takeIf { it.isNotBlank() })
    .joinToString(" · ")
    .ifEmpty { null }
}
