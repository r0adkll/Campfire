// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import kotlinx.serialization.Serializable

@Serializable
data class CreateBookmarkRequest(
  val time: Int,
  val title: String,
)
