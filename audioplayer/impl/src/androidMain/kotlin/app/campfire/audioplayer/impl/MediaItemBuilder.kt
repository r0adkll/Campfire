package app.campfire.audioplayer.impl

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.bark
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.Media
import app.campfire.core.model.Session

object MediaItemBuilder {

  fun build(session: Session): List<MediaItem> = with (session.libraryItem) {
    val chapters = media.chapters
    val audioTracks = media.tracks

    return chapters.map { chapter ->
      // Chapters may not sync with audio tracks, so we should attempt to find the track
      // that contains this chapter
      val track = audioTracks.find {
        val trackStart = it.startOffset
        val trackEnd = trackStart + it.duration
        chapter.start in trackStart.rangeUntil(trackEnd)
      } ?: error("Unable to find track for chapter ${chapter.title}")

      bark { "MediaItem(chapter=$chapter, track=$track)" }

      createMediaItem(chapter, track, media)
    }
  }

  private fun createMediaItem(
    chapter: Chapter,
    track: AudioTrack,
    media: Media
  ): MediaItem {
    return MediaItem.Builder()
      .setMediaId("${media.id}_${chapter.id}")
      .setUri(track.contentUrl)
      .setMimeType(track.mimeType)
      .apply {
        // If the item audio tracks and chapters line up (i.e. it has multiple audio files for the entire media)
        // then we don't need to add a clipping configuration
        if (
          chapter.start != track.startOffset ||
          chapter.end != (track.startOffset + track.duration)
        ) {
          setClippingConfiguration(
            ClippingConfiguration.Builder()
              .setStartPositionMs(chapter.start.seconds.inWholeMilliseconds)
              .setEndPositionMs(chapter.end.seconds.inWholeMilliseconds)
              .build()
          )
        }
      }
      .setMediaMetadata(
        MediaMetadata.Builder()
          .setTitle(chapter.title)
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
}
