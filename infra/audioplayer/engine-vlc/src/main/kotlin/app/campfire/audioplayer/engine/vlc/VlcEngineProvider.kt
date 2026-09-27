// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.engine.vlc

import app.campfire.audioplayer.impl.engine.DesktopAudioEngineProvider
import app.campfire.audioplayer.impl.engine.PlaybackEngine
import app.campfire.core.di.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject

/** Offers libvlc as a desktop engine whenever this module is on the classpath. */
@ContributesIntoSet(AppScope::class)
@Inject
class VlcEngineProvider : DesktopAudioEngineProvider {
  override val name: String = DesktopAudioEngineProvider.VLC
  override val factory: PlaybackEngine.Factory = VlcPlaybackEngine.Factory()
  override val supportsHls: Boolean = false
}
