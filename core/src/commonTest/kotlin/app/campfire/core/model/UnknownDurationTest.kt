// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import app.campfire.core.model.preview.libraryItem
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDateTime

/**
 * A zero duration means the server couldn't read the file's length, which must not count as
 * having listened to all of it.
 */
@OptIn(ExperimentalUuidApi::class)
class UnknownDurationTest {

  @Test
  fun sessionWithUnknownDuration_isNotFinished() {
    val session = session(duration = Duration.ZERO, currentTime = 5.minutes)

    assertThat(session.isFinished).isFalse()
    assertThat(session.timeRemaining).isEqualTo(Duration.ZERO)
  }

  @Test
  fun sessionAtItsEnd_isFinished() {
    val session = session(duration = 30.minutes, currentTime = 30.minutes)

    assertThat(session.isFinished).isTrue()
  }

  @Test
  fun progressWithUnknownDuration_fallsBackToReportedProgress() {
    val progress = mediaProgress(duration = 0f, currentTime = 300f, progress = 0f)

    assertThat(progress.actualProgress).isEqualTo(0f)
  }

  @Test
  fun progressWithKnownDuration_isTimeOverDuration() {
    val progress = mediaProgress(duration = 600f, currentTime = 300f, progress = 0f)

    assertThat(progress.actualProgress).isEqualTo(0.5f)
  }

  private fun session(duration: Duration, currentTime: Duration): Session {
    val now = LocalDateTime(2026, 10, 9, 12, 0)
    return Session(
      id = Uuid.random(),
      libraryItem = libraryItem(duration = duration),
      userId = "user_1",
      isDeleted = false,
      playMethod = PlayMethod.DirectPlay,
      mediaPlayer = "test",
      timeListening = currentTime,
      startTime = Duration.ZERO,
      currentTime = currentTime,
      lastPlayedAt = now,
      startedAt = now,
      updatedAt = now,
    )
  }

  private fun mediaProgress(duration: Float, currentTime: Float, progress: Float) = MediaProgress(
    id = "progress_1",
    userId = "user_1",
    libraryItemId = "li_1",
    mediaItemId = "media_1",
    mediaItemType = MediaType.Book,
    duration = duration,
    progress = progress,
    currentTime = currentTime,
    isFinished = false,
    hideFromContinueListening = false,
    lastUpdate = 0,
    startedAt = 0,
    source = MediaProgress.Source.Local,
  )
}
