// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

/**
 * The body of `POST /api/items/:id/match`. The server searches [provider] by the book's own
 * title, author, ISBN and ASIN, and takes the first result.
 *
 * Both overrides are always sent: when neither is, the server replaces everything if its "prefer
 * matched metadata" setting is on, which the app can't see.
 */
@Serializable
data class QuickMatchRequest(
  val provider: String,
  /** Replace details the book already has, not only fill in the empty ones */
  val overrideDetails: Boolean,
  /** Replace the book's cover, not only give it one when it has none */
  val overrideCover: Boolean,
)

/**
 * What `POST /api/items/:id/match` answers: whether it changed the book, or a [warning] when the
 * source found nothing for it.
 */
@Serializable
data class QuickMatchResponse(
  val updated: Boolean = false,
  val warning: String? = null,
)
