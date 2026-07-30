// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.cast.CastPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import app.campfire.audioplayer.impl.RemotePlayerFactory
import app.campfire.core.di.AppScope
import com.r0adkll.kimchi.annotations.ContributesBinding
import me.tatarka.inject.annotations.Inject

/**
 * Real [RemotePlayerFactory] contributed when this module is included in the build,
 * wrapping the local player in a media3 [CastPlayer] that handles local/remote switching
 * itself. Returns null on devices without the Play Services Cast module.
 */
@OptIn(UnstableApi::class)
@ContributesBinding(AppScope::class)
@Inject
class CastRemotePlayerFactory : RemotePlayerFactory {

  override fun create(context: Context, localPlayer: ExoPlayer): Player? {
    return SafeCastContext.getContext(context)?.let {
      CastPlayer.Builder(context)
        .setLocalPlayer(localPlayer)
        .build()
    }
  }
}
