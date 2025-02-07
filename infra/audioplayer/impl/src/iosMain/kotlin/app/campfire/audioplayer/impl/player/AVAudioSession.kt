package app.campfire.audioplayer.impl.player

import platform.AVFAudio.AVAudioSessionInterruptionOptionKey
import platform.AVFAudio.AVAudioSessionInterruptionOptionShouldResume
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionPortDescription
import platform.AVFAudio.AVAudioSessionPortHeadphones
import platform.AVFAudio.AVAudioSessionRouteChangePreviousRouteKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonCategoryChange
import platform.AVFAudio.AVAudioSessionRouteChangeReasonKey
import platform.AVFAudio.AVAudioSessionRouteChangeReasonNewDeviceAvailable
import platform.AVFAudio.AVAudioSessionRouteChangeReasonNoSuitableRouteForCategory
import platform.AVFAudio.AVAudioSessionRouteChangeReasonOldDeviceUnavailable
import platform.AVFAudio.AVAudioSessionRouteChangeReasonOverride
import platform.AVFAudio.AVAudioSessionRouteChangeReasonRouteConfigurationChange
import platform.AVFAudio.AVAudioSessionRouteChangeReasonUnknown
import platform.AVFAudio.AVAudioSessionRouteChangeReasonWakeFromSleep
import platform.AVFAudio.AVAudioSessionRouteDescription
import platform.Foundation.NSNotification

fun AVAudioSessionRouteDescription.hasHeadphones(): Boolean {
  return outputs
    .filterIsInstance<AVAudioSessionPortDescription>()
    .any { it.portType == AVAudioSessionPortHeadphones }
}

fun NSNotification.getPreviousRoute(): AVAudioSessionRouteDescription? {
  return userInfo?.get(AVAudioSessionRouteChangePreviousRouteKey) as? AVAudioSessionRouteDescription
}

enum class InterruptionType(private val nsValue: ULong?) {
  Unknown(null),
  Began(AVAudioSessionInterruptionTypeBegan),
  Ended(AVAudioSessionInterruptionTypeEnded),
  ;

  companion object {
    fun fromNotification(notification: NSNotification): InterruptionType {
      val value = notification.userInfo?.get(AVAudioSessionInterruptionTypeKey)
      return when (value) {
        Began.nsValue -> Began
        Ended.nsValue -> Ended
        else -> Unknown
      }
    }
  }
}

enum class InterruptionOptions(private val nsValue: ULong?) {
  Unknown(null),
  ShouldResume(AVAudioSessionInterruptionOptionShouldResume),
  ;

  companion object {
    fun fromNotification(notification: NSNotification): InterruptionOptions {
      val value = notification.userInfo?.get(AVAudioSessionInterruptionOptionKey)
      return when (value) {
        ShouldResume.nsValue -> ShouldResume
        else -> Unknown
      }
    }
  }
}

enum class RouteChangeReason(val nsValue: ULong) {
  Unknown(AVAudioSessionRouteChangeReasonUnknown),
  NewDeviceAvailable(AVAudioSessionRouteChangeReasonNewDeviceAvailable),
  OldDeviceUnavailable(AVAudioSessionRouteChangeReasonOldDeviceUnavailable),
  CategoryChange(AVAudioSessionRouteChangeReasonCategoryChange),
  Override(AVAudioSessionRouteChangeReasonOverride),
  WakeFromSleep(AVAudioSessionRouteChangeReasonWakeFromSleep),
  NoSuitableRouteForCategory(AVAudioSessionRouteChangeReasonNoSuitableRouteForCategory),
  RouteConfigurationChange(AVAudioSessionRouteChangeReasonRouteConfigurationChange),
  ;

  companion object {
    fun fromNotification(notification: NSNotification): RouteChangeReason {
      val value = notification.userInfo?.get(AVAudioSessionRouteChangeReasonKey)
      return when (value) {
        OldDeviceUnavailable.nsValue -> OldDeviceUnavailable
        CategoryChange.nsValue -> CategoryChange
        Override.nsValue -> Override
        WakeFromSleep.nsValue -> WakeFromSleep
        NoSuitableRouteForCategory.nsValue -> NoSuitableRouteForCategory
        RouteConfigurationChange.nsValue -> RouteConfigurationChange
        else -> Unknown
      }
    }
  }
}
