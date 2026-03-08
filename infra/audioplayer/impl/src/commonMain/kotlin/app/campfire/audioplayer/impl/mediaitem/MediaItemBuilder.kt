package app.campfire.audioplayer.impl.mediaitem

import app.campfire.core.extensions.seconds
import app.campfire.core.logging.Corked
import app.campfire.core.model.AudioTrack
import app.campfire.core.model.Chapter
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.Media
import app.campfire.core.model.Session
import app.campfire.crashreporting.CrashReporter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.time.Duration.Companion.seconds

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

    // It's probably a safe assumption that if the # of chapters matches the # of audio tracks/files
    // then this item is file segmented by chapter, and we can assume a 1:1 relationship
    if (chapters.size == audioTracks.size) {
      return audioTracks.mapIndexed { index, track ->
        val chapter = chapters[index]
        createMediaItem(chapter, track, false, media)
      }
    }

    // If there is only one track, then this is likely the other common setup where one track (i.e File)
    // is segmented by N-Chapters. Here we can make the assumption to just
    if (audioTracks.size == 1) {
      val track = audioTracks.first()
      val trackStart = track.startOffset.seconds
      val trackEnd = (track.startOffset + track.duration).seconds

      // Filter Chapter to just those that "fit" in the track
      val chaptersForTrack = chapters.filter { chapter ->
        val chapterStart = chapter.start.seconds
        chapterStart in trackStart.rangeTo(trackEnd)
      }

      // If there are fewer chapters to slice by then in the item, then the chapter meta
      // is busted. Record an exception and then slice only the ones that matter
      if (chaptersForTrack.size != chapters.size) {
        recordMediaItemException("Chapters are mis-aligned and not all fit onto the track", item)
      } else {
        // We can assume all chapters cover this track,
        // but let's validate that all the chapters provide full track coverage
        val lastChapter = chapters.last()
        val remainingTrackDuration = (track.startOffset + track.duration).seconds - lastChapter.end.seconds
        if (remainingTrackDuration > RemainingTrackDurationThreshold) {
          recordMediaItemException("Remaining track after last chapter is too long", item)
        }
      }

      // Regardless of state, slice the track to the chapters and let the user
      // see our error UI to correct the playback of this item
      return@with chaptersForTrack.map { chapter ->
        createMediaItem(chapter, track, true, media)
      }
    }

    // If we have multiple tracks that are not the same # of chapters, then lets do our best
    // to slice them accordingly.
    return audioTracks.flatMap { track ->
      // Filter chapters that _start_ in this track
      val chaptersForTrack = chapters.filter { chapter ->
        val chapterStart = chapter.start.seconds
        val chapterEnd = chapter.end.seconds
        val trackStart = track.startOffset.seconds
        val trackEnd = (track.startOffset + track.duration).seconds

        chapterStart in trackStart.rangeTo(trackEnd) ||
          chapterEnd in trackStart.rangeTo(trackEnd)
      }

      if (chaptersForTrack.isNotEmpty()) {
        // Validate that chapters actually cover the track.
        val endOfChapters = chaptersForTrack.last().end.seconds
        val remainingTrackDuration = (track.startOffset + track.duration).seconds - endOfChapters
        if (remainingTrackDuration > RemainingTrackDurationThreshold) {
          // If the chapter fail to fully cover the track, then just return the track and let the user fix
          // their item on the server
          recordMediaItemException("Remaining track after last chapter is too long", item)
          listOf(createMediaItem(track, media))
        } else {
          // For each chapter filter to this track, cut a media item for each
          chaptersForTrack.map { chapter ->
            createMediaItem(chapter, track, true, media)
          }
        }
      } else {
        // Otherwise, if we are unable to associate chapter data to this track,
        // just create a media item of just the track. This way we ensure playback occurs
        recordMediaItemException("Unable to find any chapters for the track[${track.index}]", item)
        listOf(createMediaItem(track, media))
      }
    }
  }

  private val RemainingTrackDurationThreshold = 10.seconds

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
        // There COULD be a scenario where the chapter starts before the track, but
        // ends in the track. Therefore, we can't clip a track BEFORE it starts so
        // make sure we start at either the chapter, or track start
        val startSeconds = max(chapter.start, track.startOffset)
        // There COULD be a scenario where the chapter extends beyond
        // the duration of a track. In this case we can't clip it so
        // make sure the end is the lesser of the two
        val endSeconds = min(chapter.end, track.startOffset + track.duration)
        MediaItem.Clipping(
          startMs = startSeconds.seconds.inWholeMilliseconds,
          endMs = endSeconds.seconds.inWholeMilliseconds,
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

  private fun recordMediaItemException(
    message: String,
    item: LibraryItem,
  ) {
    // Dump item state to logs
    ebark { "MediaItem Compute Exception" }
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

    CrashReporter.record(MediaItemException(message, item))
  }
}
