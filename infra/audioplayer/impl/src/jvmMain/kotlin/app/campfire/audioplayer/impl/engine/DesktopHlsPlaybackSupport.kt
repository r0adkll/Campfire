// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import app.campfire.core.di.AppScope
import app.campfire.core.di.SingleIn
import app.campfire.sessions.api.HlsPlaybackSupport
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

/** HLS on desktop follows the engine that will actually play: FFmpeg yes, libvlc no. */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class DesktopHlsPlaybackSupport(
  private val engineProviders: Set<DesktopAudioEngineProvider>,
) : HlsPlaybackSupport {
  override val supportsHls: Boolean
    get() = DesktopEngineSelection.select(engineProviders, DesktopEngineSelection.requested())?.supportsHls == true
}
