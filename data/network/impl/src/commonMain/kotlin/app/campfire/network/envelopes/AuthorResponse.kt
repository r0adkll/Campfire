// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import app.campfire.network.models.Author
import kotlinx.serialization.Serializable

@Serializable
data class AuthorResponse(
  val results: List<Author>,
  val total: Int,
  val limit: Int,
  val page: Int,
)
