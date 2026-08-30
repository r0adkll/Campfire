// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

/**
 * What a [BookInfoProvider] can actually serve. Providers overlap only partially
 * (e.g. only some have written review text), so consumers select providers per
 * capability rather than assuming a uniform feature set.
 */
data class ProviderCapabilities(
  val hasReviewText: Boolean = false,
  val hasAggregateRating: Boolean = false,
  val hasSeriesOrdering: Boolean = false,
  val hasUpcomingReleases: Boolean = false,
  val hasSupplementalMetadata: Boolean = false,
  val requiresAccountLink: Boolean = false,
)
