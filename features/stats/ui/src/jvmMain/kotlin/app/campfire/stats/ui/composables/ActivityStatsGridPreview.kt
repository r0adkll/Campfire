// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.ui.composables

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.stats.ui.StatsUiModel
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@Preview
@Composable
fun ActivityStatsGridPreview() {
  PreviewScaffold(
    useDarkColors = false,
  ) {
    ActivityStatsGrid(
      model = StatsUiModel.Activity(
        currentStreak = 12,
        bestStreak = 34,
        dailyAverage = 84.minutes,
        bestDay = 5.hours + 23.minutes,
        booksFinished = 87,
        booksFinishedThisYear = 23,
        hasPodcastActivity = true,
        episodesFinished = 142,
        episodesFinishedThisYear = 56,
      ),
      onFinishedThisYearClick = {},
      modifier = Modifier
        .padding(top = 56.dp)
        .padding(horizontal = 16.dp),
    )
  }
}
