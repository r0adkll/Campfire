// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.payloads

import kotlinx.serialization.Serializable

@Serializable
data class SeriesRemovedPayload(
  val id: String,
  val libraryId: String,
)
