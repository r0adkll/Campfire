// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

/**
 * The body of `PATCH /api/items/:id/cover`: the path on the server of an image to use as the
 * item's cover, such as one of the item's own files.
 */
@Serializable
data class SetCoverRequest(
  val cover: String,
)
