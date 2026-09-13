// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui

import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import kotlin.test.Test

/**
 * The docked bar's tool row cannot show everything at the 840dp breakpoint where the bar first
 * appears: an even split of the flexible space gives that side ~284dp, and the full set wants far
 * more. These pin the order things fold away in.
 */
class ActionOverflowTest {

  @Test
  fun `nothing folds away when there is room for everything`() {
    assertThat(
      actionOverflow(available = 600.dp, hasEqualizer = true, hasChapters = true, timerRunning = false),
    ).isEmpty()
  }

  @Test
  fun `equalizer and chapters fold away first`() {
    // Enough for bookmark + speed + timer + volume + the overflow button, but not the extra two
    assertThat(
      actionOverflow(available = 330.dp, hasEqualizer = true, hasChapters = true, timerRunning = false),
    ).containsExactlyInAnyOrder(OverflowAction.Equalizer, OverflowAction.Chapters)
  }

  @Test
  fun `the timer follows when even that is not enough`() {
    // 284dp is what the row actually gets at the 840dp window width this bar starts at
    assertThat(
      actionOverflow(available = 284.dp, hasEqualizer = true, hasChapters = true, timerRunning = false),
    ).containsExactlyInAnyOrder(OverflowAction.Equalizer, OverflowAction.Chapters, OverflowAction.Timer)
  }

  @Test
  fun `a running timer takes its countdown width into account`() {
    // The same width that fits a stopped timer cannot fit the running pill, so it folds sooner
    val width = 380.dp
    assertThat(
      actionOverflow(available = width, hasEqualizer = false, hasChapters = false, timerRunning = false),
    ).isEmpty()
    assertThat(
      actionOverflow(available = width, hasEqualizer = false, hasChapters = false, timerRunning = true),
    ).isEmpty()

    assertThat(
      actionOverflow(available = 200.dp, hasEqualizer = false, hasChapters = false, timerRunning = true),
    ).containsExactlyInAnyOrder(OverflowAction.Timer)
  }

  @Test
  fun `the output device picker folds away before anything else`() {
    // 404dp is what the tool row gets at a 1080dp window — a common desktop size, and the width
    // at which the picker is reached through the overflow rather than inline.
    val folded = actionOverflow(
      available = 404.dp,
      hasEqualizer = true,
      hasChapters = true,
      hasOutputDevices = true,
      timerRunning = false,
    )
    assertThat(folded).containsExactlyInAnyOrder(
      OverflowAction.OutputDevice,
      OverflowAction.Equalizer,
      OverflowAction.Chapters,
    )

    // Wide enough and it stays in the row itself
    assertThat(
      actionOverflow(
        available = 600.dp,
        hasEqualizer = true,
        hasChapters = true,
        hasOutputDevices = true,
        timerRunning = false,
      ),
    ).isEmpty()
  }

  @Test
  fun `actions the session does not have never appear in the overflow`() {
    // A book with no chapters and an engine with no equalizer only ever folds the timer
    assertThat(
      actionOverflow(available = 100.dp, hasEqualizer = false, hasChapters = false, timerRunning = false),
    ).containsExactlyInAnyOrder(OverflowAction.Timer)
  }
}
