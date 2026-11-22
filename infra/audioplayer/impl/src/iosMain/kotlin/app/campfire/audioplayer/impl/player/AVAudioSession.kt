// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.player

import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionPortAirPlay
import platform.AVFAudio.AVAudioSessionPortBluetoothA2DP
import platform.AVFAudio.AVAudioSessionPortBluetoothHFP
import platform.AVFAudio.AVAudioSessionPortBluetoothLE
import platform.AVFAudio.AVAudioSessionPortCarAudio
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.AVAudioSessionPortHeadphones
import platform.AVFAudio.AVAudioSessionPortUSBAudio
import platform.AVFAudio.AVAudioSessionRouteChangePreviousRouteKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonOldDeviceUnavailable
import platform.AVFAudio.AVAudioSessionRouteDescription
import platform.Foundation.NSNotification
import platform.Foundation.NSNumber

/**
 * Outputs whose disappearance should pause playback rather than move it onto the speaker:
 * headphones and headsets, AirPlay, CarPlay and USB audio.
 */
private val privateOutputPorts = setOf(
  AVAudioSessionPortHeadphones,
  AVAudioSessionPortBluetoothA2DP,
  AVAudioSessionPortBluetoothLE,
  AVAudioSessionPortBluetoothHFP,
  AVAudioSessionPortAirPlay,
  AVAudioSessionPortCarAudio,
  AVAudioSessionPortUSBAudio,
)

fun AVAudioSessionRouteDescription.hasPrivateOutput(): Boolean {
  return outputs
    .filterIsInstance<AVAudioSessionPortDescription>()
    .any { it.portType in privateOutputPorts }
}

enum class InterruptionType {
  Began,
  Ended,
  Unknown,
  ;

  companion object {
    fun fromNotification(notification: NSNotification): InterruptionType {
      return when (notification.userInfoULong(AVAudioSessionInterruptionTypeKey)) {
        AVAudioSessionInterruptionTypeBegan -> Began
        AVAudioSessionInterruptionTypeEnded -> Ended
        else -> Unknown
      }
    }
  }
}

/** Whether an ended interruption says the session may resume (`AVAudioSessionInterruptionOptionShouldResume`). */
fun NSNotification.interruptionShouldResume(): Boolean {
  val options = userInfoULong(AVAudioSessionInterruptionOptionKey) ?: return false
  return options and AVAudioSessionInterruptionOptionShouldResume != 0uL
}

/** True when a route change notification reports that the previous output device went away. */
fun NSNotification.isOldDeviceUnavailable(): Boolean {
  return userInfoULong(AVAudioSessionRouteChangeReasonKey) == AVAudioSessionRouteChangeReasonOldDeviceUnavailable
}

fun NSNotification.getPreviousRoute(): AVAudioSessionRouteDescription? {
  return userInfo?.get(AVAudioSessionRouteChangePreviousRouteKey) as? AVAudioSessionRouteDescription
}

/**
 * Reads an `NSUInteger` userInfo value. It arrives as an [NSNumber] (or a bridged Kotlin number),
 * never as a [ULong], so comparing the raw value against the enum constants never matches.
 */
private fun NSNotification.userInfoULong(key: Any?): ULong? {
  return when (val value = userInfo?.get(key)) {
    is NSNumber -> value.unsignedLongValue
    is Number -> value.toLong().toULong()
    else -> null
  }
}
