// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import app.campfire.core.di.AppScope
import app.campfire.sessions.api.HlsPlaybackSupport
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

/** AVPlayer HLS playback hasn't been wired for authenticated segments yet. */
@ContributesBinding(AppScope::class)
@Inject
class IosHlsPlaybackSupport : HlsPlaybackSupport {
  override val supportsHls: Boolean = false
}
