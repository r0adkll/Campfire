// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.discover.api

import app.campfire.bookinfo.api.ProviderId
import app.campfire.bookinfo.api.ProviderSeriesEntry
import app.campfire.core.model.SeriesId

/**
 * A provider-listed series book the user doesn't own, found by a library scan
 * and tagged with the series it was discovered under.
 */
data class DiscoveredBook(
  val seriesId: SeriesId,
  val seriesName: String,
  val entry: ProviderSeriesEntry,
  val providerId: ProviderId,
)
