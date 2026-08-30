// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.libraries.ui.detail.composables.slots

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class StarFillFractionTest {

  @Test
  fun `stars below the rating are fully filled`() {
    assertThat(starFillFraction(rating = 4.63, index = 0)).isEqualTo(1f)
    assertThat(starFillFraction(rating = 4.63, index = 3)).isEqualTo(1f)
  }

  @Test
  fun `the star at the rating boundary is partially filled`() {
    assertThat(starFillFraction(rating = 4.63, index = 4)).isEqualTo(0.63f)
    assertThat(starFillFraction(rating = 2.5, index = 2)).isEqualTo(0.5f)
  }

  @Test
  fun `stars above the rating are empty`() {
    assertThat(starFillFraction(rating = 2.5, index = 3)).isEqualTo(0f)
    assertThat(starFillFraction(rating = 0.0, index = 0)).isEqualTo(0f)
  }

  @Test
  fun `out of range ratings are clamped`() {
    assertThat(starFillFraction(rating = 7.2, index = 4)).isEqualTo(1f)
    assertThat(starFillFraction(rating = -1.0, index = 0)).isEqualTo(0f)
  }
}
