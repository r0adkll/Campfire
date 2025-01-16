package app.campfire.audioplayer.impl.player

import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItemStatusFailed
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerItemStatusUnknown
import platform.AVFoundation.AVPlayerStatusFailed
import platform.AVFoundation.AVPlayerStatusReadyToPlay
import platform.AVFoundation.AVPlayerStatusUnknown
import platform.AVFoundation.AVPlayerTimeControlStatusPaused
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate
import platform.AVFoundation.AVPlayerWaitingDuringInterstitialEventReason
import platform.AVFoundation.AVPlayerWaitingForCoordinatedPlaybackReason
import platform.AVFoundation.AVPlayerWaitingToMinimizeStallsReason
import platform.AVFoundation.AVPlayerWaitingWhileEvaluatingBufferingRateReason
import platform.AVFoundation.AVPlayerWaitingWithNoItemToPlayReason
import platform.AVFoundation.currentItem
import platform.AVFoundation.play
import platform.AVFoundation.timeControlStatus

internal val AVPlayer.isReadyToPlay: Boolean
  get() = status == AVPlayerStatusReadyToPlay

internal val AVPlayer.isCurrentItemReadyToPlay: Boolean
  get() = currentItem?.status == AVPlayerItemStatusReadyToPlay

internal val AVPlayer.isPaused: Boolean
  get() = timeControlStatus == AVPlayerTimeControlStatusPaused

internal fun AVPlayer.playIfReady() {
  if (isReadyToPlay && isCurrentItemReadyToPlay) play()
}

internal fun avPlayerStatusString(status: Long): String = when (status) {
  AVPlayerStatusReadyToPlay -> "AVPlayerStatusReadyToPlay"
  AVPlayerStatusFailed -> "AVPlayerStatusFailed"
  AVPlayerStatusUnknown -> "AVPlayerStatusUnknown"
  else -> "<$status:unknown>"
}

internal fun avTimeControlStatusString(status: Long): String = when (status) {
  AVPlayerTimeControlStatusPaused -> "AVPlayerTimeControlStatusPaused"
  AVPlayerTimeControlStatusPlaying -> "AVPlayerTimeControlStatusPlaying"
  AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate -> "AVPlayerTimeControlStatusWaitingToPlayAtSpecifiedRate"
  else -> "<$status: unknown>"
}

internal fun avPlayerItemStatusString(status: Long?) = when (status) {
  AVPlayerItemStatusReadyToPlay -> "AVPlayerItemStatusReadyToPlay"
  AVPlayerItemStatusFailed -> "AVPlayerItemStatusFailed"
  AVPlayerItemStatusUnknown -> "AVPlayerItemStatusUnknown"
  else -> "<$status: unknown>"
}

internal fun avReasonForWaitingToPlayString(reason: String): String = when (reason) {
  AVPlayerWaitingWhileEvaluatingBufferingRateReason -> "AVPlayerWaitingWhileEvaluatingBufferingRateReason"
  AVPlayerWaitingToMinimizeStallsReason -> "AVPlayerWaitingToMinimizeStallsReason"
  AVPlayerWaitingForCoordinatedPlaybackReason -> "AVPlayerWaitingForCoordinatedPlaybackReason"
  AVPlayerWaitingDuringInterstitialEventReason -> "AVPlayerWaitingDuringInterstitialEventReason"
  AVPlayerWaitingWithNoItemToPlayReason -> "AVPlayerWaitingWithNoItemToPlayReason"
  else -> "<unknown --> $reason>"
}
