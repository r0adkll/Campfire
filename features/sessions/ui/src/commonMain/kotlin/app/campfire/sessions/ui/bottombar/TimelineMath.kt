// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.bottombar

import app.campfire.core.extensions.seconds
import app.campfire.core.model.Bookmark
import app.campfire.core.model.Chapter
import kotlin.math.abs
import kotlin.time.Duration

/** A point of interest on the whole-book timeline the listener can hover and jump to. */
sealed interface TimelineMarker {
  val time: Duration
  val title: String

  data class ChapterMarker(val chapter: Chapter) : TimelineMarker {
    override val time: Duration get() = chapter.start.seconds
    override val title: String get() = chapter.title
  }

  data class BookmarkMarker(val bookmark: Bookmark) : TimelineMarker {
    override val time: Duration get() = bookmark.time
    override val title: String get() = bookmark.title
  }
}

/**
 * Geometry for [BookTimeline]: absolute book time <-> track fraction <-> pixel, plus marker
 * snapping. Pure so the hover/click behavior is unit-testable without a composition.
 */
object TimelineMath {

  fun fraction(time: Duration, total: Duration): Float {
    if (total <= Duration.ZERO || !time.isFinite()) return 0f
    return (time / total).toFloat().coerceIn(0f, 1f)
  }

  fun timeAt(fraction: Float, total: Duration): Duration {
    if (total <= Duration.ZERO) return Duration.ZERO
    return total * fraction.coerceIn(0f, 1f).toDouble()
  }

  fun xOf(time: Duration, total: Duration, widthPx: Float): Float = fraction(time, total) * widthPx

  fun fractionAt(xPx: Float, widthPx: Float): Float = if (widthPx <= 0f) 0f else (xPx / widthPx).coerceIn(0f, 1f)

  /**
   * Chapter boundaries worth marking: every chapter start except one at the very beginning of
   * the book, which would only decorate the track's left edge.
   */
  fun chapterMarkers(chapters: List<Chapter>): List<TimelineMarker.ChapterMarker> {
    return chapters
      .filter { it.start > 0f }
      .map { TimelineMarker.ChapterMarker(it) }
  }

  fun bookmarkMarkers(bookmarks: List<Bookmark>): List<TimelineMarker.BookmarkMarker> {
    return bookmarks.map { TimelineMarker.BookmarkMarker(it) }
  }

  /**
   * The marker under a pointer at [xPx], if one lies within [thresholdPx]. The closest wins;
   * on a tie a bookmark beats a chapter because it's the rarer, deliberate mark.
   */
  fun snap(
    xPx: Float,
    widthPx: Float,
    total: Duration,
    markers: List<TimelineMarker>,
    thresholdPx: Float,
  ): TimelineMarker? {
    if (widthPx <= 0f || total <= Duration.ZERO) return null
    return markers
      .map { marker -> marker to abs(xOf(marker.time, total, widthPx) - xPx) }
      .filter { (_, distance) -> distance <= thresholdPx }
      .minWithOrNull(
        compareBy<Pair<TimelineMarker, Float>> { (_, distance) -> distance }
          .thenBy { (marker, _) -> if (marker is TimelineMarker.BookmarkMarker) 0 else 1 },
      )
      ?.first
  }
}
