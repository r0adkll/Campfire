// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.audioplayer.impl.chapters

import app.campfire.audioplayer.test.fixtures.chapter
import app.campfire.audioplayer.test.fixtures.session
import app.campfire.audioplayer.test.fixtures.track
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ChapterTimelineTest {

  // region Fixtures

  // Two tracks of 30min; three chapters at 0-10, 10-30, 30-60 minutes
  private fun timeline() = ChapterTimeline(
    session(
      chapters = listOf(
        chapter(0, 0f, 600f),
        chapter(1, 600f, 1800f),
        chapter(2, 1800f, 3600f),
      ),
      tracks = listOf(
        track(1, 0f, 1800f),
        track(2, 1800f, 1800f),
      ),
    ),
  )

  // endregion

  @Test
  fun `chapterAt start is inclusive and end is exclusive`() {
    val timeline = timeline()
    assertThat(timeline.chapterAt(0.seconds)?.id).isEqualTo(0)
    assertThat(timeline.chapterAt(10.minutes)?.id).isEqualTo(1)
    assertThat(timeline.chapterAt(10.minutes - 1.seconds)?.id).isEqualTo(0)
    assertThat(timeline.chapterAt(60.minutes)).isNull()
  }

  @Test
  fun `progressAt is chapter-relative`() {
    val progress = timeline().progressAt(15.minutes)
    assertThat(progress).isNotNull()
    assertThat(progress!!.chapter.id).isEqualTo(1)
    assertThat(progress.position).isEqualTo(5.minutes)
    assertThat(progress.duration).isEqualTo(20.minutes)
  }

  @Test
  fun `nextChapterStart returns following chapter and null at the last`() {
    val timeline = timeline()
    assertThat(timeline.nextChapterStart(5.minutes)).isEqualTo(10.minutes)
    assertThat(timeline.nextChapterStart(45.minutes)).isNull()
  }

  @Test
  fun `previousChapterTarget restarts deep into a chapter and steps back near its start`() {
    val timeline = timeline()
    // 15min = 5min into chapter 1, beyond a 5s threshold -> restart chapter 1
    assertThat(timeline.previousChapterTarget(15.minutes, resetThreshold = 5.seconds)).isEqualTo(10.minutes)
    // 2s into chapter 1 -> previous chapter's start
    assertThat(timeline.previousChapterTarget(10.minutes + 2.seconds, resetThreshold = 5.seconds))
      .isEqualTo(0.seconds)
    // 2s into the first chapter clamps to its own start
    assertThat(timeline.previousChapterTarget(2.seconds, resetThreshold = 5.seconds)).isEqualTo(0.seconds)
  }

  @Test
  fun `startOfLocalQueueIndex uses chapter ids when chapters exist`() {
    assertThat(timeline().startOfLocalQueueIndex(2)).isEqualTo(30.minutes)
  }

  @Test
  fun `startOfLocalQueueIndex uses track ordinals when chapterless`() {
    val timeline = ChapterTimeline(
      session(tracks = listOf(track(1, 0f, 1800f), track(2, 1800f, 1800f))),
    )
    assertThat(timeline.startOfLocalQueueIndex(1)).isEqualTo(30.minutes)
    assertThat(timeline.startOfLocalQueueIndex(5)).isNull()
  }

  @Test
  fun `trackPositionAt maps absolute time onto the per-track queue`() {
    val position = timeline().trackPositionAt(45.minutes)
    assertThat(position).isNotNull()
    assertThat(position!!.queueIndex).isEqualTo(1)
    assertThat(position.offset).isEqualTo(15.minutes)
  }

  @Test
  fun `trackPositionAt clamps past-the-end times into the last track`() {
    val position = timeline().trackPositionAt(61.minutes)
    assertThat(position).isNotNull()
    assertThat(position!!.queueIndex).isEqualTo(1)
  }

  @Test
  fun `timeAtTrackPosition is the inverse mapping`() {
    assertThat(timeline().timeAtTrackPosition(1, 15.minutes)).isEqualTo(45.minutes)
    assertThat(timeline().timeAtTrackPosition(7, 0.seconds)).isNull()
  }

  @Test
  fun `crossedChapterBoundary detects forward crossings only`() {
    val timeline = timeline()
    assertThat(timeline.crossedChapterBoundary(previous = 9.minutes + 59.seconds, current = 10.minutes)).isTrue()
    assertThat(timeline.crossedChapterBoundary(previous = 5.minutes, current = 6.minutes)).isFalse()
    // Backward jump (a seek) is not a boundary crossing
    assertThat(timeline.crossedChapterBoundary(previous = 15.minutes, current = 5.minutes)).isFalse()
    // Crossing past the final chapter's end counts (chapterAt returns null there)
    assertThat(timeline.crossedChapterBoundary(previous = 59.minutes, current = 60.minutes)).isTrue()
  }

  @Test
  fun `chapterless items never report boundaries or chapters`() {
    val timeline = ChapterTimeline(session(tracks = listOf(track(1, 0f, 1800f))))
    assertThat(timeline.hasChapters).isFalse()
    assertThat(timeline.chapterAt(5.minutes)).isNull()
    assertThat(timeline.crossedChapterBoundary(1.minutes, 2.minutes)).isFalse()
  }
}
