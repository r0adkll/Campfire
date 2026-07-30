// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.socket.events

import app.campfire.network.RequestOrigin
import app.campfire.network.models.PodcastEpisodeDownload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class EpisodeDownloadQueued(
  val download: PodcastEpisodeDownload,
) : SocketEvent {
  override fun toString(): String = "EpisodeDownloadQueued(id=${download.id}, " +
    "title=${download.episodeDisplayTitle}, libraryItemId=${download.libraryItemId})"

  override fun applyOrigin(origin: RequestOrigin) {
    download.applyOrigin(origin)
  }

  companion object : SocketEventConfig<EpisodeDownloadQueued> {
    override val name: String = "episode_download_queued"
    override fun Json.decode(element: JsonElement): EpisodeDownloadQueued {
      return EpisodeDownloadQueued(decodeFromJsonElement(PodcastEpisodeDownload.serializer(), element))
    }
  }
}
