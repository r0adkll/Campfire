// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.sleep

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.model.PlaybackTimer
import app.campfire.audioplayer.model.RunningTimer
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSleepTimerManager : SleepTimerManager {

  override val runningTimer = MutableStateFlow<RunningTimer?>(null)

  var sessionStarts = 0
  var endOfChapterCalls = 0

  /** What [endOfChapter] reports: true means an end-of-chapter timer fired and paused playback. */
  var endOfChapterResult = false

  val timers = mutableListOf<PlaybackTimer>()
  var cleared = 0

  val factory: SleepTimerManager.Factory = object : SleepTimerManager.Factory {
    override fun create(player: AudioPlayer): SleepTimerManager = this@FakeSleepTimerManager
  }

  override fun onSessionStart() {
    sessionStarts++
  }

  override fun setTimer(timer: PlaybackTimer) {
    timers += timer
  }

  override fun clearTimer() {
    cleared++
  }

  override fun endOfChapter(): Boolean {
    endOfChapterCalls++
    return endOfChapterResult
  }
}
