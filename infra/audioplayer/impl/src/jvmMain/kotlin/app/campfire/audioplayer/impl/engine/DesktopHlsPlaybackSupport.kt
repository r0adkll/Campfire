// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.engine

import app.campfire.core.di.AppScope
import app.campfire.sessions.api.HlsPlaybackSupport
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

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
