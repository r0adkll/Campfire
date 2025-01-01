package app.campfire.audioplayer.impl

import androidx.annotation.OptIn
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaItem.ClippingConfiguration
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_AUDIO_BOOK_CHAPTER
import androidx.media3.common.util.UnstableApi
import app.campfire.core.extensions.seconds
import app.campfire.core.logging.bark
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.Media
import app.campfire.core.model.Session
import kotlin.math.abs

object MediaItemBuilder {

  fun build(session: Session): List<MediaItem> = with(session.libraryItem) {
    val chapters = media.chapters
    val audioTracks = media.tracks

    // Its probably a safe assumption that if the # of chapters matches the # of audio tracks/files
    // then this item is file segmented by chapter and we can assume a 1:1 relationship
    val likelyTrackPerChapter = chapters.size != audioTracks.size

    return chapters.map { chapter ->
      // Chapters may not sync with audio tracks, so we should attempt to find the track
      // that contains this chapter
      val track = audioTracks.find {
        val trackStart = it.startOffset.seconds.inWholeSeconds
        val trackEnd = (it.startOffset + it.duration).seconds.inWholeSeconds
        chapter.start.seconds.inWholeSeconds in trackStart.rangeUntil(trackEnd)
      } ?: error("Unable to find track for chapter ${chapter.title}")

      // Determine now if the audio track needs to be clipped for this item\
      val diff = computerChapterTrackDiffInSeconds(chapter, track)

      bark { "MediaItem[~${diff}s](chapter=$chapter, track=$track)" }

      createMediaItem(chapter, track, likelyTrackPerChapter || diff < 0f, media)
    }
  }

  private fun computerChapterTrackDiffInSeconds(
    chapter: Chapter,
    track: AudioTrack,
  ): Float {
    val startDiff = abs(chapter.start - track.startOffset)
    val durationDiff = abs((chapter.end - chapter.start) - track.duration)
    return startDiff + durationDiff
  }

  @OptIn(UnstableApi::class)
  private fun createMediaItem(
    chapter: Chapter,
    track: AudioTrack,
    clipAudio: Boolean,
    media: Media,
  ): MediaItem {
    return MediaItem.Builder()
      .setMediaId("${media.id}_${chapter.id}")
      .setUri(track.contentUrlWithToken)
      .setMimeType(track.mimeType)
      .apply {
        // If the item audio tracks and chapters line up (i.e. it has multiple audio files for the entire media)
        // then we don't need to add a clipping configuration
        if (clipAudio) {
          val startPositionMs = chapter.start.seconds.inWholeMilliseconds
          val endPositionMs = chapter.end.seconds.inWholeMilliseconds
          setClippingConfiguration(
            ClippingConfiguration.Builder()
              .setStartPositionMs(startPositionMs)
              .setEndPositionMs(endPositionMs)
              .build(),
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
          .setDurationMs(chapter.duration.inWholeMilliseconds)
          .build(),
      )
      .build()
  }
}
