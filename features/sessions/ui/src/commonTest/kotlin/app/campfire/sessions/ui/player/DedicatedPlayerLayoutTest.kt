// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.sessions.ui.player

import androidx.compose.ui.unit.dp
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class DedicatedPlayerLayoutTest {

  @Test
  fun `the mini-player window at its default size is tall, so it stacks`() {
    assertThat(dedicatedPlayerLayout(width = 400.dp, height = 680.dp)).isEqualTo(DedicatedPlayerLayout.Tall)
  }

  @Test
  fun `a foldable's lower half is wide and short, so it goes three-column`() {
    assertThat(dedicatedPlayerLayout(width = 900.dp, height = 430.dp)).isEqualTo(DedicatedPlayerLayout.Wide)
  }

  @Test
  fun `the mini-player window at its smallest is square, so it goes compact`() {
    assertThat(dedicatedPlayerLayout(width = 360.dp, height = 360.dp)).isEqualTo(DedicatedPlayerLayout.Compact)
  }

  @Test
  fun `short but too narrow for three columns goes compact rather than crushing them`() {
    assertThat(dedicatedPlayerLayout(width = 500.dp, height = 400.dp)).isEqualTo(DedicatedPlayerLayout.Compact)
  }

  @Test
  fun `wide but tall keeps the stacked layout's large cover`() {
    assertThat(dedicatedPlayerLayout(width = 900.dp, height = 700.dp)).isEqualTo(DedicatedPlayerLayout.Tall)
  }
}
