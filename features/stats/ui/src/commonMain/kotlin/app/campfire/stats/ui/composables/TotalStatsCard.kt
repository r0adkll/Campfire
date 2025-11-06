package app.campfire.stats.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import app.campfire.common.compose.icons.rounded.CalendarClock
import app.campfire.stats.ui.StatsUiModel
import kotlin.time.Clock
import kotlin.time.Duration
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime

@Composable
internal fun TotalStatsCard(
  totals: StatsUiModel.UserTotals,
  modifier: Modifier = Modifier,
  today: LocalDate = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date },
) {
  ElevatedCard(
    modifier = modifier
      .padding(
        horizontal = StatsDefaults.HorizontalPadding,
        vertical = StatsDefaults.VerticalPadding,
      )
      .fillMaxWidth(),
  ) {
    Row(
      Modifier
        .padding(
          vertical = 8.dp,
        ),
    ) {
      Icon(
        Icons.Rounded.CalendarClock,
        contentDescription = null,
        modifier = Modifier.padding(16.dp),
        tint = MaterialTheme.colorScheme.primary,
      )

      Column(
        modifier = Modifier
          .align(Alignment.CenterVertically)
          .weight(1f),
      ) {
        Text(
          text = buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
              append("${totals.totalTime.inWholeMinutes} minutes")
            }
            append(" total")
          },
        )

        if (totals.totalDays > 0) {
          Text(
            text = buildAnnotatedString {
              withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                append("${totals.totalDays} days")
              }
              append(" total")
            },
          )
        }
      }

      ListeningBarChart(
        days = totals.days,
        today = today,
        modifier = Modifier
          .height(40.dp)
          .padding(horizontal = 16.dp)
          .align(Alignment.CenterVertically),
      )
    }
  }
}

@Composable
private fun ListeningBarChart(
  days: ImmutableMap<LocalDate, Duration>,
  modifier: Modifier = Modifier,
  barColor: Color = MaterialTheme.colorScheme.secondary,
  barCount: Int = 15,
  today: LocalDate = remember { Clock.System.now().toLocalDateTime(TimeZone.UTC).date },
) {
  require(barCount > 0)

  val xAxis = remember(today, days) {
    val max = days.values.max()
    (barCount - 1 downTo 0).map { offset ->
      val day = today - DatePeriod(days = offset)
      val duration = days[day] ?: Duration.ZERO
      (duration / max).toFloat()
    }
  }

  val width = (BarWidth * barCount) + (BarSpacing * (barCount - 1))

  Canvas(
    modifier = modifier
      .width(width),
  ) {
    val availableBarHeight = size.height - MinBarHeight.roundToPx()
    val cornerRadius = BarWidth.toPx() / 2
    xAxis.forEachIndexed { index, duration ->
      val barHeight = availableBarHeight * duration

      val x = (BarWidth.toPx() * index) + (BarSpacing.toPx() * index)
      val y = availableBarHeight - barHeight

      drawRoundRect(
        color = barColor,
        topLeft = Offset(x, y),
        size = Size(BarWidth.toPx(), barHeight + MinBarHeight.toPx()),
        cornerRadius = CornerRadius(cornerRadius),
      )
    }
  }
}

private val BarWidth = 4.dp
private val BarSpacing = 1.dp
private val MinBarHeight = 4.dp
