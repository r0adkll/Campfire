// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.campfire.core.model.SeriesSequence
import assertk.assertThat
import assertk.assertions.containsExactly
import kotlin.test.Test

class SeriesSequenceListAdapterTest {

  @Test
  fun roundTripsSequenceLabel() {
    val series = listOf(
      SeriesSequence("a", "Sessions", 2026.1, label = "2026.10.10"),
      SeriesSequence("b", "Other", 2.0),
    )

    val decoded = SeriesSequenceListAdapter.decode(SeriesSequenceListAdapter.encode(series))

    assertThat(decoded).containsExactly(*series.toTypedArray())
  }

  @Test
  fun decodesRowsWrittenBeforeLabels() {
    val decoded = SeriesSequenceListAdapter.decode("""[{"id":"a","name":"Sessions","sequence":2026.1}]""")

    assertThat(decoded).containsExactly(SeriesSequence("a", "Sessions", 2026.1))
  }
}
