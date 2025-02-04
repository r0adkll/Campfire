package app.campfire.stats.ui.composables

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.campfire.stats.ui.StatsUiModel
import kotlin.random.Random
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.toPersistentMap
import kotlinx.datetime.LocalDate

@Preview
@Composable
fun TotalStatsCardPreview() {
  PreviewScaffold {
    fun randomDuration(): Duration = Random.nextInt(0, 3600).seconds

    fun days(): ImmutableMap<LocalDate, Duration> = (8..15).associate {
      LocalDate(2025, 1, it) to randomDuration()
    }.toPersistentMap()

    TotalStatsCard(
      totals = StatsUiModel.UserTotals(
        totalDays = 13,
        totalTime = 823.minutes,
        days = days(),
      ),
      modifier = Modifier.padding(16.dp),
      today = LocalDate(2025, 1, 15),
    )
    TotalStatsCard(
      totals = StatsUiModel.UserTotals(
        totalDays = 0,
        totalTime = 1358.minutes,
        days = days(),
      ),
      modifier = Modifier.padding(16.dp),
      today = LocalDate(2025, 1, 15),
    )
  }
}
