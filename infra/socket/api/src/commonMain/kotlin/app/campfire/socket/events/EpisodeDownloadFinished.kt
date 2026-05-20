package app.campfire.socket.events

import app.campfire.network.models.PodcastEpisodeDownload
import app.campfire.socket.handlers.SocketEventHandler
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

data class EpisodeDownloadFinished(
  val download: PodcastEpisodeDownload,
) : SocketEvent {
  override fun toString(): String =
    "EpisodeDownloadFinished(id=${download.id}, title=${download.episodeDisplayTitle}, failed=${download.failed})"

  companion object : SocketEventHandler<EpisodeDownloadFinished> {
    override val name: String = "episode_download_finished"
    override fun Json.decode(element: JsonElement): EpisodeDownloadFinished {
      return EpisodeDownloadFinished(decodeFromJsonElement(PodcastEpisodeDownload.serializer(), element))
    }
  }
}
