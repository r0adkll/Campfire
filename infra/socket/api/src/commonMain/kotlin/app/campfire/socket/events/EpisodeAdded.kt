// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.LibraryItemExpanded
import app.campfire.network.models.PodcastEpisode
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.jsonObject

data class EpisodeAdded(
  val episode: PodcastEpisode,
  val libraryItem: LibraryItemExpanded,
) : SocketEvent {
  override fun toString(): String =
    "EpisodeAdded(episodeId=${episode.id}, title=${episode.title}, libraryItemId=${episode.libraryItemId})"

  override fun applyOrigin(origin: RequestOrigin) {
    libraryItem.applyOrigin(origin)
  }

  companion object : SocketEventConfig<EpisodeAdded> {
    override val name: String = "episode_added"
    override fun Json.decode(element: JsonElement): EpisodeAdded {
      val obj = element.jsonObject
      val episode = decodeFromJsonElement(PodcastEpisode.serializer(), element)
      val libraryItem = decodeFromJsonElement(LibraryItemExpanded.serializer(), obj.getValue("libraryItem"))
      return EpisodeAdded(episode, libraryItem)
    }
  }
}
