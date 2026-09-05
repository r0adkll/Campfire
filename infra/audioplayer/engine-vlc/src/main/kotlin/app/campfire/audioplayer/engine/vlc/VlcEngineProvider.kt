// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.engine.vlc

import app.campfire.audioplayer.impl.engine.DesktopAudioEngineProvider
import app.campfire.audioplayer.impl.engine.PlaybackEngine
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

/** Offers libvlc as a desktop engine whenever this module is on the classpath. */
@ContributesMultibinding(AppScope::class)
@Inject
class VlcEngineProvider : DesktopAudioEngineProvider {
  override val name: String = DesktopAudioEngineProvider.VLC
  override val factory: PlaybackEngine.Factory = VlcPlaybackEngine.Factory()
}
