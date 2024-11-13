package app.campfire.audioplayer.impl.model

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Media

/**
 * Represents a hydrated [app.campfire.core.model.AudioTrack] item that can be interpreted by the underlying
 * [app.campfire.audioplayer.AudioPlayer] platform implementation.
 */
data class Track(
  val id: String,
  val track: AudioTrack,
  val media: Media,
) {

  /**
   * Get this track as a [MediaItem] to use with [androidx.media3.exoplayer.ExoPlayer]
   */
  val asMediaItem: MediaItem
    get() = MediaItem.Builder()
      .setMediaId(id)
      .setUri(track.contentUrl)
      .setMimeType(track.mimeType)
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle(media.metadata.title)
          .setArtist(media.metadata.authorName)
          .setMediaType(MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
          .setDescription(media.metadata.description)
          .setSubtitle(media.metadata.subtitle)
          .setAlbumTitle(media.metadata.seriesName)
          .setArtworkUri(media.coverImageUrl.toUri())
          .build(),
      )
      .build()
}
