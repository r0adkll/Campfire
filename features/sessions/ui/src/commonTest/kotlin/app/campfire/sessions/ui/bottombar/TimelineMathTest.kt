// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.bottombar

import app.campfire.core.model.Bookmark
import app.campfire.core.model.Chapter
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.datetime.LocalDateTime

class TimelineMathTest {

  private val total = 10.hours
  private val width = 1000f

  private fun chapter(id: Int, startSeconds: Float, endSeconds: Float) =
    Chapter(id = id, start = startSeconds, end = endSeconds, title = "Chapter ${id + 1}")

  private fun bookmark(time: Duration, title: String = "Mark") = Bookmark(
    userId = "u",
    libraryItemId = "i",
    title = title,
    time = time,
    createdAt = LocalDateTime(2026, 1, 1, 0, 0),
  )

  @Test
  fun `fractions and times round-trip and clamp`() {
    assertThat(TimelineMath.fraction(5.hours, total)).isEqualTo(0.5f)
    assertThat(TimelineMath.fraction(12.hours, total)).isEqualTo(1f)
    assertThat(TimelineMath.fraction(1.hours, Duration.ZERO)).isEqualTo(0f)
    assertThat(TimelineMath.timeAt(0.25f, total)).isEqualTo(2.5.hours)
    assertThat(TimelineMath.timeAt(1.5f, total)).isEqualTo(total)
    assertThat(TimelineMath.xOf(2.5.hours, total, width)).isEqualTo(250f)
    assertThat(TimelineMath.fractionAt(250f, width)).isEqualTo(0.25f)
    assertThat(TimelineMath.fractionAt(250f, 0f)).isEqualTo(0f)
  }

  @Test
  fun `the chapter starting at zero gets no marker`() {
    val markers = TimelineMath.chapterMarkers(
      listOf(chapter(0, 0f, 600f), chapter(1, 600f, 1800f), chapter(2, 1800f, 3600f)),
    )
    assertThat(markers.map { it.time }).isEqualTo(listOf(10.minutes, 30.minutes))
    assertThat(markers.first().title).isEqualTo("Chapter 2")
  }

  @Test
  fun `snapping picks the nearest marker inside the threshold, bookmarks winning ties`() {
    val chapterAt5h = TimelineMarker.ChapterMarker(chapter(1, 18_000f, 20_000f))
    val bookmarkAt5h = TimelineMarker.BookmarkMarker(bookmark(5.hours))
    val bookmarkAt6h = TimelineMarker.BookmarkMarker(bookmark(6.hours, "Later"))
    val markers = listOf(chapterAt5h, bookmarkAt5h, bookmarkAt6h)

    // 5h is x=500; 6h is x=600
    assertThat(TimelineMath.snap(504f, width, total, markers, thresholdPx = 6f)).isEqualTo(bookmarkAt5h)
    assertThat(TimelineMath.snap(597f, width, total, markers, thresholdPx = 6f)).isEqualTo(bookmarkAt6h)
    assertThat(TimelineMath.snap(550f, width, total, markers, thresholdPx = 6f)).isNull()
    assertThat(TimelineMath.snap(504f, width, Duration.ZERO, markers, thresholdPx = 6f)).isNull()
  }

  @Test
  fun `a chapter marker snaps when it is the only nearby mark`() {
    val chapterAt5h = TimelineMarker.ChapterMarker(chapter(1, 18_000f, 20_000f))
    val bookmarkAt6h = TimelineMarker.BookmarkMarker(bookmark(6.hours))
    assertThat(TimelineMath.snap(497f, width, total, listOf(chapterAt5h, bookmarkAt6h), thresholdPx = 6f))
      .isEqualTo(chapterAt5h)
    assertThat(chapterAt5h.time).isEqualTo(18_000.seconds)
  }
}
