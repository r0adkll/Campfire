// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

import kotlinx.serialization.Serializable

/**
 * A series as one provider knows it: canonical entries in reading order,
 * including announced-but-unreleased books. Serializable so implementations
 * can persist it in their local cache.
 */
@Serializable
data class ProviderSeries(
  val providerSeriesId: String,
  val name: String,
  val isCompleted: Boolean?,
  val entries: List<ProviderSeriesEntry>,
)

/**
 * One canonical book in a provider's series listing.
 *
 * @param position reading-order position; fractional positions are companion
 * works (e.g. 0.5 novellas)
 * @param isReleased false for announced/placeholder entries with future or
 * unknown release dates
 */
@Serializable
data class ProviderSeriesEntry(
  val providerBookId: String,
  val position: Double,
  val title: String,
  val releaseDate: String?,
  val isReleased: Boolean,
  val providerUrl: String?,
  val coverUrl: String?,
  val isbns: List<String> = emptyList(),
  val asins: List<String> = emptyList(),
)
