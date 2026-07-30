// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import android.os.Bundle
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.MediaSession

/**
 * Observation hooks into the media session internals for debug tooling (Livewire).
 * Release builds keep the no-op default; debug tooling swaps in an implementation
 * via [Holder] at app initialization.
 */
interface AudioPlayerDebugHooks {
  fun onExoPlayerCreated(exoPlayer: ExoPlayer) {}
  fun onSessionCreated(session: MediaLibrarySession) {}
  fun onSessionReleased() {}
  fun onControllerConnected(session: MediaSession, controller: MediaSession.ControllerInfo) {}
  fun onCustomCommand(controller: MediaSession.ControllerInfo, action: String, args: Bundle) {}
  fun onMediaButtonEvent(packageName: String, keyCode: Int?) {}

  object Holder {
    @Volatile
    var hooks: AudioPlayerDebugHooks = object : AudioPlayerDebugHooks {}
  }
}
