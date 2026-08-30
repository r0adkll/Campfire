// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

import app.campfire.core.model.LibraryItem

/**
 * One row of a series as shown to the user: a book they own, a released book
 * missing from their library, or an announced-but-unreleased book. [key] is
 * unique within a series listing and stable for lazy-list keying.
 */
sealed interface SeriesEntry {
  val key: String

  data class Owned(val item: LibraryItem) : SeriesEntry {
    override val key: String get() = item.id
  }

  data class Missing(val entry: ProviderSeriesEntry, val providerId: ProviderId) : SeriesEntry {
    override val key: String get() = "missing:${providerId.key}:${entry.providerBookId}"
  }

  data class Upcoming(val entry: ProviderSeriesEntry, val providerId: ProviderId) : SeriesEntry {
    override val key: String get() = "upcoming:${providerId.key}:${entry.providerBookId}"
  }
}

/**
 * Presentation-ready series listing. [providerId]/[providerName] are null when
 * no provider could serve the series — [entries] then contains only [SeriesEntry.Owned]
 * rows. Provider attribution must be shown when provider-sourced rows render.
 */
data class SeriesInfoState(
  val providerId: ProviderId?,
  val providerName: String?,
  val isCompleted: Boolean?,
  val entries: List<SeriesEntry>,
)
