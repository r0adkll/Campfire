// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.ui.cast

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
import app.campfire.common.compose.icons.CampfireIcons
import app.campfire.common.compose.icons.rounded.BluetoothAudio
import app.campfire.common.compose.icons.rounded.Computer
import app.campfire.common.compose.icons.rounded.DeviceUnknown
import app.campfire.common.compose.icons.rounded.DirectionsCar
import app.campfire.common.compose.icons.rounded.Headphones
import app.campfire.common.compose.icons.rounded.Hearing
import app.campfire.common.compose.icons.rounded.PhoneAndroid
import app.campfire.common.compose.icons.rounded.SettingsInputHdmi
import app.campfire.common.compose.icons.rounded.Smartphone
import app.campfire.common.compose.icons.rounded.Speaker
import app.campfire.common.compose.icons.rounded.Tablet
import app.campfire.common.compose.icons.rounded.Tv
import app.campfire.common.compose.icons.rounded.Usb
import app.campfire.common.compose.icons.rounded.VideogameAsset
import app.campfire.common.compose.icons.rounded.Watch

fun CastDevice.asIcon(): ImageVector {
  if (id == CastDevice.DEFAULT_ID) return CampfireIcons.Rounded.PhoneAndroid
  return when (type) {
    UNKNOWN -> CampfireIcons.Rounded.DeviceUnknown
    TV -> CampfireIcons.Rounded.Tv
    SPEAKER -> CampfireIcons.Rounded.Speaker
    TABLET -> CampfireIcons.Rounded.Tablet
    COMPUTER -> CampfireIcons.Rounded.Computer
    GAME_CONSOLE -> CampfireIcons.Rounded.VideogameAsset
    CAR -> CampfireIcons.Rounded.DirectionsCar
    BLUETOOTH -> CampfireIcons.Rounded.BluetoothAudio
    SMARTPHONE -> CampfireIcons.Rounded.Smartphone
    SMARTWATCH -> CampfireIcons.Rounded.Watch
    HEADPHONES -> CampfireIcons.Rounded.Headphones
    USB -> CampfireIcons.Rounded.Usb
    HDMI -> CampfireIcons.Rounded.SettingsInputHdmi
    HEARING_AID -> CampfireIcons.Rounded.Hearing
  }
}
