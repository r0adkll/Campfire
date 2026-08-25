// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.cast

import android.net.Uri
import androidx.media3.common.MediaItem as PlatformMediaItem
import androidx.media3.common.MediaMetadata
import app.campfire.audioplayer.impl.mediaitem.MediaItem as CampfireMediaItem

/**
 * Converts a common [CampfireMediaItem] into a platform media3 item shaped for the Cast
 * receiver. Unlike the local conversion this keeps the server HTTPS cover URL (a receiver
 * cannot resolve the app's `content://` artwork provider) and never applies clipping.
 */
internal fun CampfireMediaItem.asCastMediaItem(): PlatformMediaItem {
  val metadata = metadata
  return PlatformMediaItem.Builder()
    .setMediaId(id)
    .setUri(uri)
    .setMimeType(mimeType)
    .apply {
      if (metadata != null) {
        setMediaMetadata(
          MediaMetadata.Builder()
            .setTitle(metadata.title)
            .setArtist(metadata.artist)
            .setMediaType(MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
            .setDescription(metadata.description)
            .setSubtitle(metadata.subtitle)
            .setAlbumTitle(metadata.albumTitle)
            .setArtworkUri(metadata.artworkUri?.let(Uri::parse))
            .setDurationMs(metadata.durationMs)
            .build(),
        )
      }
    }
    .build()
}

/**
 * Appends the ABS `?token=` query parameter so the receiver can authenticate its own fetch of
 * this URL. Only applies to http(s) URLs, and never twice.
 */
internal fun Uri.withAccessToken(token: String): Uri {
  if (scheme != "http" && scheme != "https") return this
  if (getQueryParameter("token") != null) return this
  return buildUpon()
    .appendQueryParameter("token", token)
    .build()
}
