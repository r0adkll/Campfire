// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.mediaitem

import app.campfire.core.extensions.isIn
import app.campfire.core.extensions.seconds
import app.campfire.core.model.LibraryItem
import app.campfire.core.model.PodcastEpisode
import app.campfire.core.model.Session
import kotlin.time.Duration

object IosMediaItemBuilder {

  fun build(session: Session): List<IosMediaItem> {
    val episode = session.episode
    if (episode != null) {
      return buildPodcastEpisode(session.libraryItem, episode)
    }
    return buildBook(session)
  }

  private fun buildPodcastEpisode(
    item: LibraryItem,
    episode: PodcastEpisode,
  ): List<IosMediaItem> {
    val track = episode.audioTrack ?: return emptyList()
    val durationMs = episode.duration.inWholeMilliseconds
    val metadata = MediaItem.Metadata(
      id = 0,
      title = episode.title,
      artist = item.media.metadata.author ?: item.media.metadata.title,
      description = episode.description ?: "",
      subtitle = episode.subtitle,
      albumTitle = item.media.metadata.title,
      artworkUri = item.media.coverImageUrl,
      durationMs = durationMs,
      libraryItemId = item.id,
    )
    return listOf(
      IosMediaItem(
        id = episode.id,
        uri = track.contentUrl,
        startOffset = Duration.ZERO,
        duration = episode.duration,
        tracks = listOf(
          IosMediaItem.Track(
            id = 0,
            startMs = 0L,
            endMs = durationMs,
            metadata = metadata,
          ),
        ),
      ),
    )
  }

  private fun buildBook(session: Session): List<IosMediaItem> = with(session.libraryItem) {
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
          metadata = MediaItemBuilder.createMediaMetadata(chapter, media, id),
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
              metadata = MediaItemBuilder.createMediaMetadata(chapter, media, id),
            )
          }
        }
      }

      IosMediaItem(
        id = audio.index.toString(),
        uri = audio.contentUrl,
        startOffset = audio.startOffset.seconds,
        duration = audio.duration.seconds,
        tracks = tracks,
      )
    }
  }
}
