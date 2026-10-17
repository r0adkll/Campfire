// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.extensions

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalTestApi::class)
class TimeAgoTest {

  @Test
  fun `single units read in the singular`() {
    assertThat(
      timeAgoOf(
        listOf(
          1.minutes + 5.seconds,
          1.hours + 5.minutes,
          1.days + 1.hours,
          31.days,
          366.days,
        ),
      ),
    ).containsExactly(
      "1 minute ago",
      "1 hour ago",
      "1 day ago",
      "1 month ago",
      "1 year ago",
    )
  }

  @Test
  fun `multiple units read in the plural`() {
    assertThat(
      timeAgoOf(
        listOf(
          5.minutes + 5.seconds,
          3.hours + 5.minutes,
          2.days + 1.hours,
          61.days,
          731.days,
        ),
      ),
    ).containsExactly(
      "5 minutes ago",
      "3 hours ago",
      "2 days ago",
      "2 months ago",
      "2 years ago",
    )
  }

  private fun timeAgoOf(elapsed: List<Duration>): List<String> {
    val readouts = mutableListOf<String>()
    runComposeUiTest {
      setContent {
        val now = Clock.System.now().toEpochMilliseconds()
        elapsed.forEach { readouts += (now - it.inWholeMilliseconds).timeAgo }
      }
      waitForIdle()
    }
    return readouts
  }
}
