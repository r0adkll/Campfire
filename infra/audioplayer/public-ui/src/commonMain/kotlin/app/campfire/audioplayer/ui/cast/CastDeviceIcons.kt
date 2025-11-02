package app.campfire.audioplayer.ui.cast

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BluetoothAudio
import androidx.compose.material.icons.rounded.Computer
import androidx.compose.material.icons.rounded.DeviceUnknown
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.SettingsInputHdmi
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tablet
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material.icons.rounded.VideogameAsset
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.ui.graphics.vector.ImageVector
import app.campfire.audioplayer.cast.CastDevice
import app.campfire.audioplayer.cast.CastDevice.Type.BLUETOOTH
import app.campfire.audioplayer.cast.CastDevice.Type.CAR
import app.campfire.audioplayer.cast.CastDevice.Type.COMPUTER
import app.campfire.audioplayer.cast.CastDevice.Type.GAME_CONSOLE
import app.campfire.audioplayer.cast.CastDevice.Type.HDMI
import app.campfire.audioplayer.cast.CastDevice.Type.HEADPHONES
import app.campfire.audioplayer.cast.CastDevice.Type.HEARING_AID
import app.campfire.audioplayer.cast.CastDevice.Type.SMARTPHONE
import app.campfire.audioplayer.cast.CastDevice.Type.SMARTWATCH
import app.campfire.audioplayer.cast.CastDevice.Type.SPEAKER
import app.campfire.audioplayer.cast.CastDevice.Type.TABLET
import app.campfire.audioplayer.cast.CastDevice.Type.TV
import app.campfire.audioplayer.cast.CastDevice.Type.UNKNOWN
import app.campfire.audioplayer.cast.CastDevice.Type.USB

fun CastDevice.asIcon(): ImageVector {
  if (id == CastDevice.DEFAULT_ID) return Icons.Rounded.PhoneAndroid
  return when (type) {
    UNKNOWN -> Icons.Rounded.DeviceUnknown
    TV -> Icons.Rounded.Tv
    SPEAKER -> Icons.Rounded.Speaker
    TABLET -> Icons.Rounded.Tablet
    COMPUTER -> Icons.Rounded.Computer
    GAME_CONSOLE -> Icons.Rounded.VideogameAsset
    CAR -> Icons.Rounded.DirectionsCar
    BLUETOOTH -> Icons.Rounded.BluetoothAudio
    SMARTPHONE -> Icons.Rounded.Smartphone
    SMARTWATCH -> Icons.Rounded.Watch
    HEADPHONES -> Icons.Rounded.Headphones
    USB -> Icons.Rounded.Usb
    HDMI -> Icons.Rounded.SettingsInputHdmi
    HEARING_AID -> Icons.Rounded.Hearing
  }
}
