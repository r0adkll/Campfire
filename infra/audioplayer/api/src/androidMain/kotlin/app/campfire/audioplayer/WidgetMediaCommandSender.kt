// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

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
