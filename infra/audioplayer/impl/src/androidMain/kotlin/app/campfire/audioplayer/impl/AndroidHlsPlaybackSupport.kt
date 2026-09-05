// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import app.campfire.core.di.AppScope
import app.campfire.sessions.api.HlsPlaybackSupport
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

/** ExoPlayer's HLS source authenticates every request through the app's data source. */
@ContributesBinding(AppScope::class)
@Inject
class AndroidHlsPlaybackSupport : HlsPlaybackSupport {
  override val supportsHls: Boolean = true
}
