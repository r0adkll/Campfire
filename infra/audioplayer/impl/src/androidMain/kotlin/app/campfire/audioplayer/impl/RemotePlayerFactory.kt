// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer

/**
 * Seam for remote-playback support (Google Cast). The optional `:infra:audioplayer:cast`
 * module contributes an implementation that wraps [ExoPlayer] in a media3 `CastPlayer`;
 * builds without that module (e.g. the foss flavor) fall back to [NoOp] via the injection
 * site's default argument and play everything locally.
 */
interface RemotePlayerFactory {

  /**
   * Create a remote-capable [Player] that uses [localPlayer] for local playback, or
   * `null` when remote playback isn't available in this build or on this device.
   */
  fun create(context: Context, localPlayer: ExoPlayer): Player?

  object NoOp : RemotePlayerFactory {
    override fun create(context: Context, localPlayer: ExoPlayer): Player? = null
  }
}
