// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class SeriesSequenceTest {

  @Test
  fun parsesWholeAndDecimalSequences() {
    assertThat("3".toSeriesSequenceOrNull()).isEqualTo(3.0)
    assertThat("1.1".toSeriesSequenceOrNull()).isEqualTo(1.1)
    assertThat(" 2.5 ".toSeriesSequenceOrNull()).isEqualTo(2.5)
  }

  @Test
  fun parsesLeadingNumberFromSuffixedSequences() {
    assertThat("2a".toSeriesSequenceOrNull()).isEqualTo(2.0)
    assertThat("4-5".toSeriesSequenceOrNull()).isEqualTo(4.0)
    assertThat("1.5b".toSeriesSequenceOrNull()).isEqualTo(1.5)
  }

  @Test
  fun returnsNullForBlankOrNonNumeric() {
    assertThat(null.toSeriesSequenceOrNull()).isNull()
    assertThat("".toSeriesSequenceOrNull()).isNull()
    assertThat("   ".toSeriesSequenceOrNull()).isNull()
    assertThat("prequel".toSeriesSequenceOrNull()).isNull()
  }

  @Test
  fun decimalSequencesSortBetweenWholeNumbers() {
    val sorted = listOf("2", "1.2", "1", "1.1", "10", "prequel")
      .map { SeriesSequence(it, it, it.toSeriesSequenceOrNull() ?: SeriesSequence.UNKNOWN_SEQUENCE) }
      .sortedBy { it.sequence }
      .map { it.id }

    assertThat(sorted).containsExactly("1", "1.1", "1.2", "2", "10", "prequel")
  }

  @Test
  fun formatsSequenceLikeTheServer() {
    assertThat(formatSeriesSequence(3.0)).isEqualTo("3")
    assertThat(formatSeriesSequence(1.5)).isEqualTo("1.5")
    assertThat(formatSeriesSequence(SeriesSequence.UNKNOWN_SEQUENCE)).isEqualTo("")
    assertThat(SeriesSequence("id", "name", 2.0).formattedSequence).isEqualTo("2")
  }
}
