package app.campfire.audioplayer

/**
 * Interface for sending media playback commands from an AppWidget context.
 *
 * Uses [MediaController] under the hood to communicate with [AudioPlayerService],
 * which is reliable even when the app process is cold or the [AudioPlayer] isn't
 * in memory.
 */
interface WidgetMediaCommandSender {
  suspend fun playPause()
  suspend fun seekForward()
  suspend fun seekBackward()
  suspend fun skipToNext()
  suspend fun skipToPrevious()
  suspend fun cyclePlaybackSpeed()
  suspend fun setSleepTimer(minutes: Int)
  suspend fun clearSleepTimer()
}
