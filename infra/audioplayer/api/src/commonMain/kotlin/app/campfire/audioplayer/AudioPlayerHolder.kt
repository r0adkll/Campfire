// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer

import kotlinx.coroutines.flow.StateFlow

/**
 * An interface that exists in the [app.campfire.core.di.AppScope] to
 * provide access to the current per-platform-instance for playback use.
 */
interface AudioPlayerHolder {
  val currentPlayer: StateFlow<AudioPlayer?>

  fun setCurrentPlayer(player: AudioPlayer?)
  fun release()
}
