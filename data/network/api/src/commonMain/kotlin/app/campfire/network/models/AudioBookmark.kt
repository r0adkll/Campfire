// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import kotlinx.serialization.Serializable

@Serializable
data class AudioBookmark(
  val libraryItemId: String,
  val title: String,
  val time: Float,
  val createdAt: Long,
)
