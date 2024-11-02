package app.campfire.audioplayer.model

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER
import app.campfire.core.model.Media

fun Track.toMediaItem(): MediaItem {
  return MediaItem.Builder()
    .setMediaId(id)
    .setUri(contentUri)
    .setMimeType(mimeType)
    .setMediaMetadata(metadata.toMediaMetadata())
    .build()
}

fun Media.Metadata.toMediaMetadata(): MediaMetadata {
  return MediaMetadata.Builder()
    .setTitle(title)
    .setArtist(authorName)
    .setMediaType(MEDIA_TYPE_AUDIO_BOOK_CHAPTER)
    .setDescription(description)
    .setSubtitle(subtitle)
    .setAlbumTitle(seriesName)
    .build()
}
