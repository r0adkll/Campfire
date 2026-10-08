// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.series.ui.detail.composables

import androidx.compose.runtime.Composable
import app.campfire.core.model.SeriesSequence
import app.campfire.core.model.formatSeriesSequence
import campfire.features.series.ui.generated.resources.Res
import campfire.features.series.ui.generated.resources.series_book_position
import org.jetbrains.compose.resources.stringResource

/**
 * "Book 1.5 · 2021" — a series book's reading-order position and release year, with whichever
 * part is unknown left out. Null when neither is known.
 */
@Composable
internal fun seriesBookLabel(
  position: Double?,
  year: String?,
): String? {
  val positionLabel = position
    ?.takeIf { it != SeriesSequence.UNKNOWN_SEQUENCE }
    ?.let { stringResource(Res.string.series_book_position, formatSeriesSequence(it)) }
  return listOfNotNull(positionLabel, year?.takeIf { it.isNotBlank() })
    .joinToString(" · ")
    .ifEmpty { null }
}
