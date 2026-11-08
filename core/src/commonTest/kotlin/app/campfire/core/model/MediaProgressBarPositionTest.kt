// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import kotlin.test.Test

class MediaProgressBarPositionTest {

  private fun progress(currentTime: Float, duration: Float = 36_000f, isFinished: Boolean = false) = MediaProgress(
    id = "p",
    userId = "u",
    libraryItemId = "item",
    mediaItemId = "media",
    mediaItemType = MediaType.Book,
    duration = duration,
    progress = currentTime / duration,
    currentTime = currentTime,
    isFinished = isFinished,
    hideFromContinueListening = false,
    lastUpdate = 0L,
    startedAt = 0L,
    source = MediaProgress.Source.Local,
  )

  @Test
  fun `a few seconds into a long book draws the same`() {
    // 10 hours: a step is 72 seconds
    assertThat(progress(currentTime = 3_600f).barPosition).isEqualTo(progress(currentTime = 3_605f).barPosition)
    assertThat(progress(currentTime = 3_600f).barPosition).isEqualTo(50)
  }

  @Test
  fun `moving a step draws differently`() {
    assertThat(progress(currentTime = 3_600f).barPosition).isNotEqualTo(progress(currentTime = 3_680f).barPosition)
  }

  @Test
  fun `finished draws as finished whatever the position`() {
    assertThat(progress(currentTime = 100f, isFinished = true).barPosition)
      .isEqualTo(MediaProgress.FINISHED_BAR_POSITION)
    assertThat(progress(currentTime = 36_000f).barPosition).isEqualTo(MediaProgress.BAR_STEPS)
  }
}
