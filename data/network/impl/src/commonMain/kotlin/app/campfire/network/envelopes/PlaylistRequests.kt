// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.envelopes

import app.campfire.core.model.LibraryId
import app.campfire.network.models.PlaylistItem
import kotlinx.serialization.Serializable

@Serializable
class NewPlaylistRequest(
  val libraryId: LibraryId,
  val name: String,
  val description: String? = null,
  val items: List<PlaylistItem.Minified> = emptyList(),
)

@Serializable
class UpdatePlaylistRequest(
  val name: String,
  val description: String? = null,
  val items: List<PlaylistItem.Minified> = emptyList(),
)
