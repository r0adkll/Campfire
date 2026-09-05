// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.macos

import app.campfire.audioplayer.AudioPlayer
import app.campfire.audioplayer.AudioPlayerHolder
import app.campfire.core.app.AppInitializer
import app.campfire.core.di.AppScope
import app.campfire.core.di.qualifier.ForScope
import app.campfire.core.logging.Cork
import app.campfire.settings.api.PlaybackSettings
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import kotlinx.coroutines.CoroutineScope
import me.tatarka.inject.annotations.Inject

/**
 * Wires the macOS media integrations on startup: Now Playing + media keys, pause when the
 * output device the user was listening on disappears, and Dock transport controls.
 *
 * Every native piece is optional — a failure to load a framework logs and leaves playback alone.
 */
@ContributesMultibinding(AppScope::class)
@Inject
class MacMediaIntegrationInitializer(
  private val holder: AudioPlayerHolder,
  private val settings: PlaybackSettings,
  @ForScope(AppScope::class) private val applicationScope: CoroutineScope,
) : AppInitializer {

  override val priority: Int = AppInitializer.LOWEST_PRIORITY

  override suspend fun onInitialize() {
    if (!isMacOs()) return

    runCatching {
      NowPlayingCoordinator(holder, settings, MacNowPlayingBridge(), applicationScope).start()
    }.onFailure { ebark(it) { "Now Playing integration unavailable" } }

    runCatching {
      OutputDeviceMonitor { previous, current ->
        val player = holder.currentPlayer.value ?: return@OutputDeviceMonitor
        val playing = player.state.value == AudioPlayer.State.Playing
        if (OutputDevicePolicy.shouldPause(previous, current, playing)) {
          ibark { "Output device $previous went away; pausing" }
          player.pause()
        }
      }.start()
    }.onFailure { ebark(it) { "Output device monitoring unavailable" } }

    runCatching { MacDockMenu(holder).install() }
      .onFailure { ebark(it) { "Dock menu unavailable" } }
  }

  private fun isMacOs(): Boolean = System.getProperty("os.name").orEmpty().lowercase().contains("mac")

  companion object : Cork {
    override val tag: String = "MacMediaIntegration"
    override val enabled: Boolean = true
  }
}
