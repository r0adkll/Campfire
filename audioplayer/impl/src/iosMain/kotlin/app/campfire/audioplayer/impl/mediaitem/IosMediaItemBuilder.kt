package app.campfire.audioplayer.impl.mediaitem

import app.campfire.core.extensions.isIn
import app.campfire.core.extensions.seconds
import app.campfire.core.model.Session

object IosMediaItemBuilder {

  fun build(session: Session): List<IosMediaItem> = with(session.libraryItem) {
    val chapters = media.chapters
    val audioTracks = media.tracks

    // Its probably a safe assumption that if the # of chapters matches the # of audio tracks/files
    // then this item is file segmented by chapter and we can assume a 1:1 relationship
    val likelyTrackPerChapter = chapters.size == audioTracks.size

    return audioTracks.map { audio ->

      // If our tracks match our chapters, then we'll want to find a single 1:1 match
      // ;Otherwise, we'll need to find the group of chapters that fall within this item
      val tracks = mutableListOf<IosMediaItem.Track>()

      if (likelyTrackPerChapter) {
        val chapter = chapters.find { c ->
          val diff = MediaItemBuilder.computerChapterTrackDiffInSeconds(c, audio)
          diff < 0.1f
        } ?: error("Unable to find matching chapter for $audio")

        tracks += IosMediaItem.Track(
          id = chapter.id,
          startMs = chapter.start.seconds.inWholeMilliseconds,
          endMs = chapter.end.seconds.inWholeMilliseconds,
          metadata = MediaItemBuilder.createMediaMetadata(chapter, media),
        )
      } else {
        val audioFileRange = audio.startOffset.seconds..(audio.startOffset + audio.duration).seconds
        for (chapter in chapters) {
          val chapterRange = chapter.start.seconds..chapter.end.seconds
          if (chapterRange isIn audioFileRange) {
            tracks += IosMediaItem.Track(
              id = chapter.id,
              startMs = chapter.start.seconds.inWholeMilliseconds,
              endMs = chapter.end.seconds.inWholeMilliseconds,
              metadata = MediaItemBuilder.createMediaMetadata(chapter, media),
            )
          }
        }
      }

      IosMediaItem(
        id = audio.index.toString(),
        uri = audio.contentUrlWithToken,
        startOffset = audio.startOffset.seconds,
        duration = audio.duration.seconds,
        tracks = tracks,
      )
    }
  }
}
