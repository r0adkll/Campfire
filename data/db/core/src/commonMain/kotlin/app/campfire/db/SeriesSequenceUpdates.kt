// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.campfire.data.Media
import app.campfire.data.MediaQueries
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull

/**
 * Bring an existing row's primary series up to date from a freshly mapped [media] row, which
 * `insertOrIgnore` leaves untouched: the sortable sequence columns, and that series' entry in
 * the full series list, which carries the sequence exactly as the server stores it.
 */
suspend fun MediaQueries.updatePrimarySeriesSequence(media: Media) {
  val id = media.metadata_series_id ?: return
  val name = media.metadata_series_name ?: return
  // A sequence with no number (e.g. "prequel") parses to null; its entry below still carries it.
  media.metadata_series_sequence?.let { sequence ->
    updateSeriesSequence(id, name, sequence, media.libraryItemId)
  }

  val entry = media.metadata_series?.find { it.id == id } ?: return
  val existing = selectSeriesList(media.libraryItemId).awaitAsOneOrNull()?.metadata_series.orEmpty()
  updateSeriesList(existing.filterNot { it.id == id } + entry, media.libraryItemId)
}
