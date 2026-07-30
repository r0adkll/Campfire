// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.test

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import kotlinx.coroutines.flow.MutableStateFlow

class FakeAudioPlayerHolder : AudioPlayerHolder {

  override val currentPlayer = MutableStateFlow<AudioPlayer?>(null)

  override fun setCurrentPlayer(player: AudioPlayer?) {
    currentPlayer.value = player
  }

  override fun release() {
    currentPlayer.value?.release()
    currentPlayer.value = null
  }
}
