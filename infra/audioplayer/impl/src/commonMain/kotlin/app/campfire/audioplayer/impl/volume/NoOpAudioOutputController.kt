// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.volume

import app.campfire.audioplayer.AudioOutputController
import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import com.r0adkll.kimchi.annotations.ContributesBinding
import kotlinx.coroutines.flow.MutableStateFlow
import me.tatarka.inject.annotations.Inject

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
