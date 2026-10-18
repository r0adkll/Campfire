// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.data.mapping

import app.campfire.network.models.ExpandedBookMetadata
import app.campfire.network.models.MinifiedBookMetadata
import app.campfire.network.models.SeriesSequence
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test

class SeriesSequenceMappingTest {

  @Test
  fun minifiedSeries_keepsServerSequenceForDisplay() {
    val metadata = MinifiedBookMetadata(
      series = SeriesSequence("s", "Sessions", "2026.10.10"),
    ).asDomainModel()

    assertThat(metadata.series.map { it.formattedSequence }).containsExactly("2026.10.10")
  }

  @Test
  fun expandedSeries_keepsServerSequenceForDisplay() {
    val metadata = ExpandedBookMetadata(
      series = listOf(
        SeriesSequence("a", "Alpha", "2026.09.30"),
        SeriesSequence("b", "Beta", "3"),
        SeriesSequence("c", "Gamma", null),
      ),
    ).asDomainModel()

    assertThat(metadata.series.map { it.formattedSequence }).containsExactly("2026.09.30", "3", "")
  }
}
