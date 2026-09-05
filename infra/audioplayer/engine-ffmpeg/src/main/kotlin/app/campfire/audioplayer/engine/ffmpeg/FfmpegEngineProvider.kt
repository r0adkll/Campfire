// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.engine.ffmpeg

import app.campfire.audioplayer.impl.engine.DesktopAudioEngineProvider
import app.campfire.audioplayer.impl.engine.PlaybackEngine
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import me.tatarka.inject.annotations.Inject

/** Offers the bundled FFmpeg engine whenever this module is on the classpath. */
@ContributesMultibinding(AppScope::class)
@Inject
class FfmpegEngineProvider : DesktopAudioEngineProvider {
  override val name: String = DesktopAudioEngineProvider.FFMPEG
  override val factory: PlaybackEngine.Factory = PlaybackEngine.Factory { FfmpegPlaybackEngine() }
}
