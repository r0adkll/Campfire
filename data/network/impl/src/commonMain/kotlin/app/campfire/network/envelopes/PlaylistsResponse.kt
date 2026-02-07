package app.campfire.network.envelopes

import app.campfire.network.models.PlaylistExpanded
import kotlinx.serialization.Serializable

@Serializable
class PlaylistsResponse(
  val results: List<PlaylistExpanded>,
) : Envelope() {

  override fun applyPostage() {
    results.forEach { playlist ->
      playlist.applyOrigin(origin)
    }
  }
}
