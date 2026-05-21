package app.campfire.socket.events

import app.campfire.network.models.PodcastEpisodeDownload
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class EpisodeDownloadStarted(
  val download: PodcastEpisodeDownload,
) : SocketEvent {
  override fun toString(): String = "EpisodeDownloadStarted(id=${download.id}, " +
    "title=${download.episodeDisplayTitle}, libraryItemId=${download.libraryItemId})"

  companion object : SocketEventConfig<EpisodeDownloadStarted> {
    override val name: String = "episode_download_started"
    override fun Json.decode(element: JsonElement): EpisodeDownloadStarted {
      return EpisodeDownloadStarted(decodeFromJsonElement(PodcastEpisodeDownload.serializer(), element))
    }
  }
}
