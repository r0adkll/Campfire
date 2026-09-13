// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.volume

import app.campfire.audioplayer.AudioOutputController
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.settings.api.AudioOutputSettings
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject

/**
 * Desktop's app-level volume.
 *
 * It owns no audio state of its own — [AudioOutputSettings] is the source of truth for the slider
 * position, and the player observes it. That indirection is what keeps the control alive across
 * session churn: `DesktopAudioPlayer` is built when a session starts and released when it stops,
 * and the native engine inside it is created lazily and reborn at full volume every time, so
 * neither is a durable home for a user preference.
 *
 * Mute is the exception and is held here, in memory only — see [AudioOutputController.isMuted].
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, replaces = [NoOpAudioOutputController::class])
@Inject
class DesktopAudioOutputController(
  private val settings: AudioOutputSettings,
) : AudioOutputController {

  override val isSupported: Boolean = true

  override val volume: StateFlow<Float> = settings.observeVolume()

  private val _isMuted = MutableStateFlow(false)
  override val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

  override fun setVolume(volume: Float) {
    val clamped = volume.coerceIn(0f, 1f)
    settings.volume = clamped
    // Dragging up off zero is how people expect to undo a mute, and leaving it muted would look
    // like a broken slider.
    if (clamped > 0f) _isMuted.value = false
  }

  override fun setMuted(muted: Boolean) {
    _isMuted.value = muted
  }
}
