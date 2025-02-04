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
fun WeeklyListeningCardPreview() {
  PreviewScaffold(
    useDarkColors = false,
  ) {
    val today = remember { LocalDate(2025, 1, 30) }
    val thisWeek = (6 downTo 0).associate {
      val day = today - DatePeriod(days = it)
      day to (Random.nextInt(0..240).minutes)
    }
    val lastWeek = (6 downTo 0).associate {
      val day = today - DatePeriod(days = it + 7)
      day to (Random.nextInt(0..240).minutes)
    }

    WeeklyListeningCard(
      model = StatsUiModel.WeeklyListening(
        weekTime = 843.minutes,
        weekOverWeekChange = 1.5,
        thisWeek = thisWeek.toPersistentMap(),
        lastWeek = lastWeek.toPersistentMap(),
      ),
      today = today,
      modifier = Modifier
        .padding(top = 56.dp)
        .padding(horizontal = 16.dp),
    )
  }
}
