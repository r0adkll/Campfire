// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import app.campfire.core.extensions.progressOver
import app.campfire.core.extensions.seconds
import kotlin.time.Duration
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime

@OptIn(ExperimentalUuidApi::class)
data class Session(
  val id: Uuid,
  val libraryItem: LibraryItem,
  val userId: UserId,
  val isDeleted: Boolean,

  // Playback / Device Info
  val playMethod: PlayMethod,
  val mediaPlayer: String,

  // Current Playback State
  val timeListening: Duration,
  val startTime: Duration,
  val currentTime: Duration,
  val lastPlayedAt: LocalDateTime?,

  // Date / Time
  val startedAt: LocalDateTime,
  val updatedAt: LocalDateTime,

  // Podcast-only: the episode currently being played. Null for book sessions.
  val episodeId: PodcastEpisodeId? = null,
) {
  /**
   * If this is a podcast session and a matching episode is found on the library item,
   * returns the episode being played. Null for book sessions or when the episode can't
   * be resolved on the loaded library item.
   */
  val episode: PodcastEpisode?
    get() = (libraryItem.media as? Media.Podcast)
      ?.episodes
      ?.firstOrNull { it.id == episodeId }

  val duration: Duration
    get() = episode?.duration ?: libraryItem.media.duration

  val timeRemaining: Duration
    get() = duration - currentTime

  val progress: Float
    get() = currentTime.progressOver(duration)

  val isFinished: Boolean
    get() = currentTime >= duration

  val chapter: Chapter?
    get() = episode?.let { ep ->
      ep.chapters.find { c ->
        val startMs = c.start.seconds.inWholeMilliseconds
        val endMs = c.end.seconds.inWholeMilliseconds
        currentTime.inWholeMilliseconds in startMs..<endMs
      }
    } ?: libraryItem.getChapterForDuration(currentTime.inWholeMilliseconds)

  val audioTrack: AudioTrack?
    get() = episode?.audioTrack
      ?: libraryItem.getAudioTrackForDuration(currentTime.inWholeMilliseconds)

  /**
   * Get the "Current" duration that should be displayed to the user. Starting
   * with the current chapter duration, the current track if chapter is not available,
   * and using the total duration if that fails.
   */
  val currentDuration: Duration
    get() = chapter?.duration ?: audioTrack?.duration?.seconds ?: duration

  val title: String
    get() = chapter?.title
      ?: episode?.title
      ?: audioTrack?.taggedTitle
      ?: libraryItem.media.metadata.title
      ?: TITLE_PLACEHOLDER

  companion object {
    const val TITLE_PLACEHOLDER = "--"
  }
}
