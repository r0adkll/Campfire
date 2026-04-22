package app.campfire.network.envelopes

import app.campfire.network.models.DeviceInfo
import app.campfire.network.models.PlaybackSession
import kotlinx.serialization.Serializable

@Serializable
class SyncSessionRequest(
  val deviceInfo: DeviceInfo,
  val sessions: List<PlaybackSession>,
)
