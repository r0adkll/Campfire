// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaQueueItem

/**
 * [MediaItemConverter] that authenticates outgoing media for the Cast receiver.
 *
 * The receiver fetches media and artwork URLs itself, so the sender's Authorization header (the
 * local player's ResolvingDataSource) never applies. Audiobookshelf accepts the access token as
 * a `?token=` query parameter instead, appended here — at conversion time, so each load uses the
 * freshest snapshot [CastMediaTokenHolder] has.
 */
@UnstableApi
internal class CampfireMediaItemConverter(
  private val tokenHolder: CastMediaTokenHolder,
) : MediaItemConverter {

  private val delegate = DefaultMediaItemConverter()

  override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
    val token = tokenHolder.accessToken ?: return delegate.toMediaQueueItem(mediaItem)
    val localConfiguration = mediaItem.localConfiguration ?: return delegate.toMediaQueueItem(mediaItem)

    val authenticated = mediaItem.buildUpon()
      .setUri(localConfiguration.uri.withAccessToken(token))
      .setMediaMetadata(
        mediaItem.mediaMetadata.buildUpon()
          .setArtworkUri(mediaItem.mediaMetadata.artworkUri?.withAccessToken(token))
          .build(),
      )
      .build()
    return delegate.toMediaQueueItem(authenticated)
  }

  override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem {
    return delegate.toMediaItem(mediaQueueItem)
  }
}
