// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.chapters

import app.campfire.core.extensions.seconds
import app.campfire.core.model.Chapter
import app.campfire.core.model.Session
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * Chapter semantics derived from a session's absolute timeline.
 *
 * The player's queue is not always chapter-granular: remote (Cast) playback uses one item per
 * audio track, and HLS playback uses a single playlist item. In those *coarse-queue* modes the
 * player can't lean on media-item boundaries for chapter behavior, so everything the UI and
 * sleep timer need — the current chapter, chapter-relative position, next/previous seek
 * targets, and boundary crossings — is derived here from the absolute position against the
 * session's chapter and track tables. One mechanism, every consumer, so per-chapter rendering
 * stays consistent regardless of queue shape.
 */
class ChapterTimeline(session: Session) {

  /** Chapters on the absolute timeline; empty when the item has none. */
  private val chapters: List<Chapter> =
    session.episode?.chapters ?: session.libraryItem.media.chapters

  /** Audio tracks on the absolute timeline (a podcast episode is a single implicit track). */
  private val tracks: List<TrackSpan> = session.episode
    ?.audioTrack
    ?.let { track ->
      listOf(TrackSpan(index = 0, startMs = 0L, durationMs = track.duration.seconds.inWholeMilliseconds))
    }
    ?: session.libraryItem.media.tracks.mapIndexed { index, track ->
      TrackSpan(
        index = index,
        startMs = track.startOffset.seconds.inWholeMilliseconds,
        durationMs = track.duration.seconds.inWholeMilliseconds,
      )
    }

  val hasChapters: Boolean get() = chapters.isNotEmpty()

  /** The chapter containing [time], or null when the item has no chapters or [time] is past the end. */
  fun chapterAt(time: Duration): Chapter? {
    val timeMs = time.inWholeMilliseconds
    return chapters.find { chapter ->
      timeMs >= chapter.startMs && timeMs < chapter.endMs
    }
  }

  /** Chapter-relative progress at [time], for rendering identical to chapter-granular queues. */
  fun progressAt(time: Duration): ChapterProgress? {
    val chapter = chapterAt(time) ?: return null
    return ChapterProgress(
      chapter = chapter,
      position = (time - chapter.start.seconds).coerceAtLeast(Duration.ZERO),
      duration = chapter.duration,
    )
  }

  /** The absolute start of the chapter after the one containing [time], or null at the last chapter. */
  fun nextChapterStart(time: Duration): Duration? {
    val chapter = chapterAt(time) ?: return null
    val nextStart = chapter.end.seconds
    // The final chapter's end is the end of the item — there is nothing to skip to
    return nextStart.takeIf { chapterAt(it) != null }
  }

  /**
   * The skip-previous target at [time]: restart the current chapter when more than
   * [resetThreshold] into it, otherwise the start of the previous chapter (clamped to the
   * current chapter's start at the beginning of the item).
   */
  fun previousChapterTarget(time: Duration, resetThreshold: Duration): Duration? {
    val chapter = chapterAt(time) ?: return null
    val progressInChapter = (time - chapter.start.seconds).coerceAtLeast(Duration.ZERO)
    return if (progressInChapter > resetThreshold) {
      chapter.start.seconds
    } else {
      chapters.lastOrNull { it.endMs <= chapter.startMs }?.start?.seconds ?: chapter.start.seconds
    }
  }

  /**
   * The absolute start time addressed by a *local-queue* index — chapter ordinal when the
   * item has chapters, track ordinal otherwise. This is how the UI addresses seeks; coarse
   * queues translate it to absolute time first.
   */
  fun startOfLocalQueueIndex(itemIndex: Int): Duration? {
    return if (hasChapters) {
      chapters.find { it.id == itemIndex }?.start?.seconds
    } else {
      tracks.getOrNull(itemIndex)?.startMs?.milliseconds
    }
  }

  /** The (queue index, offset) of [time] on a per-track queue, or null when out of range. */
  fun trackPositionAt(time: Duration): TrackPosition? {
    val timeMs = time.inWholeMilliseconds
    val track = tracks.find { timeMs >= it.startMs && timeMs < it.startMs + it.durationMs }
      ?: tracks.lastOrNull()?.takeIf { timeMs >= it.startMs }
      ?: return null
    return TrackPosition(
      queueIndex = track.index,
      offset = (timeMs - track.startMs).coerceAtLeast(0L).milliseconds,
    )
  }

  /** The absolute position represented by (per-track queue index, in-item position). */
  fun timeAtTrackPosition(queueIndex: Int, positionInTrack: Duration): Duration? {
    val track = tracks.getOrNull(queueIndex) ?: return null
    return track.startMs.milliseconds + positionInTrack
  }

  /**
   * True when moving from [previous] to [current] crossed a chapter boundary going forward —
   * the coarse-queue substitute for the media-item-transition event the end-of-chapter sleep
   * timer listens to. Backward jumps (seeks) are not boundary crossings.
   */
  fun crossedChapterBoundary(previous: Duration, current: Duration): Boolean {
    if (!hasChapters || current <= previous) return false
    val previousChapter = chapterAt(previous) ?: return false
    val currentChapter = chapterAt(current)
    return currentChapter?.id != previousChapter.id
  }

  data class ChapterProgress(
    val chapter: Chapter,
    val position: Duration,
    val duration: Duration,
  )

  data class TrackPosition(
    val queueIndex: Int,
    val offset: Duration,
  )

  private data class TrackSpan(
    val index: Int,
    val startMs: Long,
    val durationMs: Long,
  )

  private val Chapter.startMs: Long get() = start.seconds.inWholeMilliseconds
  private val Chapter.endMs: Long get() = end.seconds.inWholeMilliseconds
}
