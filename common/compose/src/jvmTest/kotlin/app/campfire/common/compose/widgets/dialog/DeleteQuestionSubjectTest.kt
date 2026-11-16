// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.common.compose.widgets.dialog

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class DeleteQuestionSubjectTest {

  @Test
  fun `a title that asks something doesn't end the question twice`() {
    assertThat("Is 'Gentle Parenting' Ruining French Children?".asDeleteQuestionSubject())
      .isEqualTo("Is 'Gentle Parenting' Ruining French Children")
  }

  @Test
  fun `other titles are left as they are`() {
    assertThat("Patient Zero".asDeleteQuestionSubject()).isEqualTo("Patient Zero")
    assertThat("Wow!".asDeleteQuestionSubject()).isEqualTo("Wow!")
  }

  @Test
  fun `trailing space before the question mark doesn't keep it`() {
    assertThat("Why Now? ".asDeleteQuestionSubject()).isEqualTo("Why Now")
  }
}
