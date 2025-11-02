package app.campfire.audioplayer.cast

import kotlinx.coroutines.flow.StateFlow

interface CastController {

  val state: StateFlow<CastState>
  val availableDevices: StateFlow<List<CastDevice>>

  fun connect(device: CastDevice)
}

enum class CastState {
  Connected,
  Connecting,
  NotConnected,
  NoDevicesAvailable,
  Unavailable,
}

abstract class CastDevice(
  val id: String,
  val name: String,
  val description: String?,
  val iconUri: String?,
  val type: Type,
  val isSelected: Boolean,
) {

  override fun toString(): String {
    return "CastDevice(" +
      "id='$id', " +
      "name='$name', " +
      "description=$description, " +
      "iconUri=$iconUri, " +
      "type=$type, " +
      "isSelected=$isSelected" +
      ")"
  }

  enum class Type {
    UNKNOWN,
    TV,
    SPEAKER,
    TABLET,
    COMPUTER,
    GAME_CONSOLE,
    CAR,
    BLUETOOTH,
    SMARTPHONE,
    SMARTWATCH,
    HEADPHONES,
    USB,
    HDMI,
    HEARING_AID,
  }

  companion object {
    const val DEFAULT_ID = "DEFAULT_ROUTE"
  }
}
