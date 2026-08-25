// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import android.content.Context
import androidx.media3.cast.DefaultCastOptionsProvider
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import com.google.android.gms.cast.framework.media.CastMediaOptions

class CastOptionsProvider : OptionsProvider {

  @UnstableApi
  override fun getCastOptions(context: Context): CastOptions {
    return CastOptions.Builder()
      .setReceiverApplicationId(DefaultCastOptionsProvider.APP_ID_DEFAULT_RECEIVER_WITH_DRM)
      .setCastMediaOptions(
        CastMediaOptions.Builder()
          .setMediaSessionEnabled(false)
          .setNotificationOptions(null)
          .build(),
      )
      // Match media3's DefaultCastOptionsProvider: media3 owns playback state, so saved-session
      // resume and the GMS reconnection service only resurrect sessions it doesn't know about.
      .setResumeSavedSession(false)
      .setEnableReconnectionService(false)
      .setRemoteToLocalEnabled(true)
      .setStopReceiverApplicationWhenEndingSession(true)
      .build()
  }

  override fun getAdditionalSessionProviders(p0: Context): List<SessionProvider> {
    return emptyList()
  }
}
