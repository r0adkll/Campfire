// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.volume

import app.campfire.audioplayer.AudioOutputController
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * The binding on platforms with no app-level volume — Android and iOS, where volume belongs to
 * the system and the media session. Reports unsupported so the UI renders nothing.
 *
 * Desktop replaces this with
 * [app.campfire.audioplayer.impl.volume.DesktopAudioOutputController].
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class NoOpAudioOutputController : AudioOutputController {
  override val isSupported: Boolean = false
  override val volume = MutableStateFlow(1f)
  override val isMuted = MutableStateFlow(false)

  override fun setVolume(volume: Float) = Unit
  override fun setMuted(muted: Boolean) = Unit
}
