package app.campfire.socket.events

import app.campfire.network.models.PodcastEpisodeDownload
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class EpisodeDownloadQueued(
  val download: PodcastEpisodeDownload,
) : SocketEvent {
  override fun toString(): String = "EpisodeDownloadQueued(id=${download.id}, " +
    "title=${download.episodeDisplayTitle}, libraryItemId=${download.libraryItemId})"

  companion object : SocketEventHandler<EpisodeDownloadQueued> {
    override val name: String = "episode_download_queued"
    override fun Json.decode(element: JsonElement): EpisodeDownloadQueued {
      return EpisodeDownloadQueued(decodeFromJsonElement(PodcastEpisodeDownload.serializer(), element))
    }
  }
}
