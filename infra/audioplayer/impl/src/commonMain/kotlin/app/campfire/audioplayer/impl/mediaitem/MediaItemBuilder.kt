package app.campfire.audioplayer.impl.mediaitem

import app.campfire.core.extensions.seconds
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.Session
import kotlin.math.abs

object MediaItemBuilder {

  fun build(session: Session): List<MediaItem> = build(session.libraryItem)

  fun build(item: LibraryItem): List<MediaItem> = with(item) {
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

      // bark { "MediaItem[~${diff}s](chapter=$chapter, track=$track)" }

      createMediaItem(chapter, track, likelyTrackPerChapter || diff < 0f, media)
    }
  }

  internal fun computerChapterTrackDiffInSeconds(
    chapter: Chapter,
    track: AudioTrack,
  ): Float {
    val startDiff = abs(chapter.start - track.startOffset)
    val durationDiff = abs((chapter.end - chapter.start) - track.duration)
    return startDiff + durationDiff
  }

  private fun createMediaItem(
    chapter: Chapter,
    track: AudioTrack,
    clipAudio: Boolean,
    media: Media,
  ): MediaItem {
    return MediaItem(
      id = "${media.id}_${chapter.id}",
      uri = track.contentUrlWithToken,
      mimeType = track.mimeType,
      clipping = if (clipAudio) {
        MediaItem.Clipping(
          startMs = chapter.start.seconds.inWholeMilliseconds,
          endMs = chapter.end.seconds.inWholeMilliseconds,
        )
      } else {
        null
      },
      metadata = createMediaMetadata(chapter, media),
    )
  }

  internal fun createMediaMetadata(
    chapter: Chapter,
    media: Media,
  ): MediaItem.Metadata {
    return MediaItem.Metadata(
      id = chapter.id,
      title = chapter.title,
      artist = media.metadata.authorName,
      description = media.metadata.description ?: "",
      subtitle = media.metadata.subtitle,
      albumTitle = media.metadata.seriesName,
      artworkUri = media.coverImageUrl,
      durationMs = chapter.duration.inWholeMilliseconds,
    )
  }
}
