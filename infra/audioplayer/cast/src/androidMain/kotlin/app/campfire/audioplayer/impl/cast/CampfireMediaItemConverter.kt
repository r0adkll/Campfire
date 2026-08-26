// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import android.net.Uri
import androidx.media3.cast.DefaultMediaItemConverter
import androidx.media3.cast.MediaItemConverter
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.google.android.gms.cast.MediaQueueItem

/**
 * [MediaItemConverter] that authenticates outgoing media for the Cast receiver.
 *
 * The receiver fetches media and artwork URLs itself, so the sender's Authorization header (the
 * local player's ResolvingDataSource) never applies. Media URLs prefer the credential-free
 * public session form staged by [CastPlaySessionHolder] (no token, immune to token expiry);
 * when no session is staged they fall back to the `?token=` query parameter, appended at
 * conversion time from the freshest snapshot [CastMediaTokenHolder] has. Artwork has no public
 * session form and always uses the token.
 */
@UnstableApi
internal class CampfireMediaItemConverter(
  private val tokenHolder: CastMediaTokenHolder,
  private val playSessionHolder: CastPlaySessionHolder,
) : MediaItemConverter {

  private val delegate = DefaultMediaItemConverter()

  override fun toMediaQueueItem(mediaItem: MediaItem): MediaQueueItem {
    val localConfiguration = mediaItem.localConfiguration ?: return delegate.toMediaQueueItem(mediaItem)
    val token = tokenHolder.accessToken

    val mediaUri = playSessionHolder.publicUrlFor(localConfiguration.uri.toString())
      ?.let(Uri::parse)
      ?: token?.let { localConfiguration.uri.withAccessToken(it) }
      ?: return delegate.toMediaQueueItem(mediaItem)

    val authenticated = mediaItem.buildUpon()
      .setUri(mediaUri)
      .setMediaMetadata(
        mediaItem.mediaMetadata.buildUpon()
          .setArtworkUri(
            token?.let { mediaItem.mediaMetadata.artworkUri?.withAccessToken(it) }
              ?: mediaItem.mediaMetadata.artworkUri,
          )
          .build(),
      )
      .build()
    return delegate.toMediaQueueItem(authenticated)
  }

  override fun toMediaItem(mediaQueueItem: MediaQueueItem): MediaItem {
    return delegate.toMediaItem(mediaQueueItem)
  }
}
