// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.bookinfo.api

import kotlinx.serialization.Serializable

/**
 * Aggregate community signal for a single book as reported by one provider.
 * Serializable so implementations can persist it in their local cache.
 *
 * @param providerBookId the provider's own id for the matched book, used for caching
 * @param providerUrl web page for the book on the provider, for attribution link-outs
 * @param ratingsDistribution star value (1..5) to count of ratings at that value
 */
@Serializable
data class BookCommunityInfo(
  val providerBookId: String,
  val providerUrl: String?,
  val rating: Double?,
  val ratingsCount: Int?,
  val ratingsDistribution: Map<Int, Int>?,
  val reviewsCount: Int?,
  val releaseDate: String?,
  val coverUrl: String?,
)

/**
 * A single written community review.
 *
 * @param author the reviewer's display name (or username when no name is set)
 * @param title the reviewer's own headline for the review, when the provider has one
 * @param avatarUrl the reviewer's profile image, when the provider exposes one
 * @param badge provider-issued profile badge text (e.g. Hardcover's "Supporter" flair)
 */
@Serializable
data class BookReview(
  val author: String?,
  val rating: Double?,
  val text: String,
  val hasSpoilers: Boolean,
  val avatarUrl: String? = null,
  val badge: String? = null,
  val title: String? = null,
)
