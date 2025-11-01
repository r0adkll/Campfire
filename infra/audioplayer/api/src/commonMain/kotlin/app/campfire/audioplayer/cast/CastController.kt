package app.campfire.audioplayer.cast

import kotlinx.coroutines.flow.StateFlow

interface CastController {

  val state: StateFlow<CastState>
  val availableDevices: StateFlow<List<CastDevice>>
}

enum class CastState {
  Connected,
  Connecting,
  NotConnected,
  NoDevicesAvailable,
  Unavailable,
}

data class CastDevice(
  val id: String,
  val name: String,
  val description: String?,
  val iconUri: String?,
) {

  companion object {
    const val DEFAULT_ID = "DEFAULT_ROUTE"
  }
}
