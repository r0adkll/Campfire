// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl

import android.content.Context
import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem as PlatformMediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER
import androidx.media3.common.util.UnstableApi
import app.campfire.audioplayer.impl.content.coverContentUriForItem
import app.campfire.audioplayer.impl.mediaitem.MediaItem

/**
 * Convert our common [MediaItem] data class/holder into the Android
 * Media3 Platform version to be consumed by the likes of an [androidx.media3.exoplayer.ExoPlayer]
 * instance.
 *
 * The [context] is required so artwork can be exposed via the cover [android.content.ContentProvider]
 * which the Android Auto host process and the system Media notification can read across processes.
 */
@OptIn(UnstableApi::class)
fun MediaItem.asPlatformMediaItem(context: Context): PlatformMediaItem {
  return PlatformMediaItem.Builder()
    .setMediaId(id)
    .setUri(uri)
    .setMimeType(mimeType)
    .setTag(tag)
    .apply {
      // If the item audio tracks and chapters line up (i.e. it has multiple audio files for the entire media)
      // then we don't need to add a clipping configuration
      if (clipping != null) {
        setClippingConfiguration(
          ClippingConfiguration.Builder()
            .setStartPositionMs(clipping.startMs)
            .setEndPositionMs(clipping.endMs)
            .build(),
        )
      }

      if (metadata != null) {
        val artworkUri = metadata.libraryItemId
          ?.let { coverContentUriForItem(context, it) }
          ?: metadata.artworkUri?.toUri()
        setMediaMetadata(
          MediaMetadata.Builder()
            .setTitle(metadata.title)
            .setArtist(metadata.artist)
            .setMediaType(MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
            .setDescription(metadata.description)
            .setSubtitle(metadata.subtitle)
            .setAlbumTitle(metadata.albumTitle)
            .setArtworkUri(artworkUri)
            .setDurationMs(metadata.durationMs)
            .build(),
        )
      }
    }
    .build()
}

fun List<MediaItem>.asPlatformMediaItems(context: Context): List<PlatformMediaItem> =
  map { it.asPlatformMediaItem(context) }
