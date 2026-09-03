// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.ui.composables

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.stats.ui.StatsUiModel
import kotlin.random.Random
import kotlin.random.nextInt
import kotlin.time.Duration.Companion.minutes
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

@Preview
@Composable
fun ListeningHeatmapCardPreview() {
  PreviewScaffold(
    useDarkColors = false,
  ) {
    val today = remember { LocalDate(2025, 1, 30) }
    val days = remember {
      (240 downTo 0)
        .filter { Random.nextInt(0..100) < 60 }
        .associate { offset ->
          val day = today - DatePeriod(days = offset)
          day to Random.nextInt(5..240).minutes
        }
    }

    ListeningHeatmapCard(
      model = StatsUiModel.ListeningHeatmap(
        days = days.toPersistentMap(),
      ),
      today = today,
      modifier = Modifier
        .padding(top = 56.dp)
        .padding(horizontal = 16.dp),
    )
  }
}
