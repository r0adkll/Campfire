package app.campfire.audioplayer.impl.mediaitem

import app.campfire.core.extensions.seconds
import app.campfire.core.logging.Corked
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.Session
import kotlin.math.abs

object MediaItemBuilder : Corked("MediaItemBuilders") {

  fun build(session: Session): List<MediaItem> = build(session.libraryItem)

  fun build(item: LibraryItem): List<MediaItem> = with(item) {
    val chapters = media.chapters
    val audioTracks = media.tracks

    // If the library item doesn't have chapters then we'll want to best-guess create the media
    // items from its audio tracks.
    if (chapters.isEmpty()) {
      return audioTracks.map { track ->
        createMediaItem(track, media)
      }
    }

    // Its probably a safe assumption that if the # of chapters matches the # of audio tracks/files
    // then this item is file segmented by chapter and we can assume a 1:1 relationship
    val likelyTrackPerChapter = chapters.size == audioTracks.size

    return chapters.mapIndexed { index, chapter ->
      if (likelyTrackPerChapter) {
        val track = audioTracks[index]
        val trackStart = track.startOffset.seconds.inWholeSeconds
        val trackEnd = (track.startOffset + track.duration).seconds.inWholeSeconds
        val diff = chapter.start.seconds.inWholeSeconds in trackStart.rangeUntil(trackEnd)
        return@mapIndexed createMediaItem(chapter, track, false, media)
      }

      // Chapters may not sync with audio tracks, so we should attempt to find the track
      // that contains this chapter
      val track = audioTracks.find {
        val trackStart = it.startOffset.seconds.inWholeSeconds
        val trackEnd = (it.startOffset + it.duration).seconds.inWholeSeconds
        chapter.start.seconds.inWholeSeconds in trackStart.rangeUntil(trackEnd)
      } ?: throwMediaItemException(index, "Unable to find track for chapter '${chapter.title}'", item)

      // Determine now if the audio track needs to be clipped for this item\
      val diff = computerChapterTrackDiffInSeconds(chapter, track)

      createMediaItem(chapter, track, true, media)
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
      uri = track.contentUrl,
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

  private fun createMediaItem(
    track: AudioTrack,
    media: Media,
  ): MediaItem {
    return MediaItem(
      id = "${media.id}_${track.index}",
      uri = track.contentUrl,
      mimeType = track.mimeType,
      metadata = createMediaMetadata(track, media),
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

  internal fun createMediaMetadata(
    track: AudioTrack,
    media: Media,
  ): MediaItem.Metadata {
    return MediaItem.Metadata(
      id = track.index,
      title = track.taggedTitle,
      artist = media.metadata.authorName,
      description = media.metadata.description ?: "",
      subtitle = media.metadata.subtitle,
      albumTitle = media.metadata.seriesName,
      artworkUri = media.coverImageUrl,
      durationMs = track.duration.seconds.inWholeMilliseconds,
    )
  }

  private fun throwMediaItemException(
    chapterIndex: Int,
    message: String,
    item: LibraryItem,
  ): Nothing {
    // Dump item state to logs
    ebark { "MediaItem Compute Exception @ Index[$chapterIndex]" }
    ebark {
      """
        LibraryItem[
          chapters = [expected: ${item.media.numChapters}, actual: ${item.media.chapters.size}],
          tracks = [expected: ${item.media.numTracks}, actual: ${item.media.tracks.size}],
          audioFiles = [expected: ${item.media.numAudioFiles}, actual: ${item.media.audioFiles.size}],
          invalid = [audioFiles: ${item.media.numInvalidAudioFiles}, missing: ${item.media.numMissingParts}],
        ]
      """.trimIndent()
    }
    ebark {
      buildString {
        appendLine("Chapters[")
        item.media.chapters.forEachIndexed { index, chapter ->
          appendLine("  [$index::${chapter.id}]: ${chapter.start.seconds} --> ${chapter.end.seconds}")
        }
        append("]")
      }
    }
    ebark {
      buildString {
        appendLine("Tracks[")
        item.media.tracks.forEachIndexed { index, track ->
          appendLine(
            "  [$index::${track.index}]: ${track.startOffset.seconds} --> " +
              "${(track.startOffset + track.duration).seconds}",
          )
        }
        append("]")
      }
    }

    throw MediaItemException(message, item)
  }
}
