package app.campfire.audioplayer.impl.player

import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerStatusReadyToPlay
import platform.AVFoundation.currentItem
import platform.AVFoundation.play

internal val AVPlayer.isReadyToPlay: Boolean
  get() = status == AVPlayerStatusReadyToPlay

internal val AVPlayer.isCurrentItemReadyToPlay: Boolean
  get() = currentItem?.status == AVPlayerItemStatusReadyToPlay

internal fun AVPlayer.playIfReady() {
  if (isReadyToPlay && isCurrentItemReadyToPlay) play()
}
