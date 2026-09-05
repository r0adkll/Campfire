// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import app.campfire.audioplayer.PlaybackEngineUnavailableException

/**
 * A desktop audio engine offered by an engine module (`:infra:audioplayer:engine-*`). The build
 * decides which modules ship; when more than one is present, [DesktopEngineSelection] picks.
 */
interface DesktopAudioEngineProvider {
  /** Stable name used by the build property and the `campfire.audio.engine` system property. */
  val name: String
  val factory: PlaybackEngine.Factory

  companion object {
    const val VLC = "vlc"
    const val FFMPEG = "ffmpeg"
  }
}

object DesktopEngineSelection {
  /**
   * Chooses among the bundled engines: the one named by [requested] when present; otherwise the
   * only engine available (a release build carries exactly one). Null when nothing is bundled
   * or the request is ambiguous.
   */
  fun select(providers: Collection<DesktopAudioEngineProvider>, requested: String?): DesktopAudioEngineProvider? {
    requested?.takeIf { it.isNotBlank() }?.let { name ->
      providers.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { return it }
    }
    return providers.singleOrNull()
  }

  /** A factory that fails with a clear message when no engine module was bundled. */
  fun unavailable(requested: String?, available: Collection<DesktopAudioEngineProvider>): PlaybackEngine.Factory =
    PlaybackEngine.Factory {
      throw PlaybackEngineUnavailableException(
        "No desktop audio engine for \"$requested\"; bundled: ${available.map { it.name }}",
      )
    }
}
