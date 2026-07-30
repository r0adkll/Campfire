// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.network.models

import app.campfire.network.envelopes.Envelope
import kotlinx.serialization.Serializable

typealias PlaylistExpanded = Playlist<PlaylistItem.Expanded>

@Serializable
data class Playlist<ItemType : PlaylistItem>(
  val id: String,
  val name: String,
  val description: String?,
  val lastUpdate: Long,
  val createdAt: Long,
  val items: List<ItemType>,
) : Envelope() {
  override fun applyPostage() {
    items.forEach { it.applyOrigin(origin) }
  }
}

@Serializable
abstract class PlaylistItem : NetworkModel() {
  abstract val libraryItemId: String
  abstract val episodeId: String?

  @Serializable
  data class Minified(
    override val libraryItemId: String,
    override val episodeId: String? = null,
  ) : PlaylistItem()

  @Serializable
  data class Expanded(
    override val libraryItemId: String,
    override val episodeId: String? = null,
    val libraryItem: LibraryItemExpanded,
    // The server returns the resolved podcast episode at the playlist-item level (not
    // nested inside libraryItem.media). For book entries this is null; for podcast
    // entries the libraryItem.media is the *minified* podcast (no episodes array) and
    // the playable unit lives here.
    val episode: PodcastEpisode? = null,
  ) : PlaylistItem()
}
