package app.campfire.network.envelopes

import app.campfire.network.models.PlaybackSession
import kotlinx.serialization.Serializable

@Serializable
class SyncSessionRequest(
  val sessions: List<PlaybackSession>,
)
