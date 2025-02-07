package app.campfire.audioplayer.impl.vlcj

import uk.co.caprica.vlcj.player.base.MediaPlayer

/**
 * Run the [block] if the media player is actually playable
 */
inline fun MediaPlayer.ifPlayable(block: MediaPlayer.() -> Unit) {
  if (status().isPlayable) block()
}

val MediaPlayer.isPlaying: Boolean get() = status().isPlaying

fun MediaPlayer.play() = controls().play()
fun MediaPlayer.pause() = controls().pause()
fun MediaPlayer.stop() = controls().stop()
fun MediaPlayer.seekForward(millis: Long) = controls().skipTime(millis)
fun MediaPlayer.seekBackward(millis: Long) = controls().skipTime(-millis)
