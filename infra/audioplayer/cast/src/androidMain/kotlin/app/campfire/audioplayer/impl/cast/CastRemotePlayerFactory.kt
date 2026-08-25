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
 * itself. Returns null on devices without the Play Services Cast module, or when the
 * async [CastContext][com.google.android.gms.cast.framework.CastContext] initialization
 * hasn't completed yet (e.g. a cold start straight into the playback service).
 */
@OptIn(UnstableApi::class)
@ContributesBinding(AppScope::class)
@Inject
class CastRemotePlayerFactory : RemotePlayerFactory {

  override fun create(context: Context, localPlayer: ExoPlayer): Player? {
    // The application context, not the service: media3's Cast singleton retains this Context
    // in a process-wide static for its lifetime.
    val appContext = context.applicationContext
    SafeCastContext.initialize(appContext)
    return SafeCastContext.getIfReady()?.let {
      CastPlayer.Builder(appContext)
        .setLocalPlayer(localPlayer)
        .build()
    }
  }
}
