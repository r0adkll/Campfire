// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.stats.ui.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import app.campfire.stats.ui.StatsUiModel.ListeningHeatmap
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.heatmap_legend_less
import campfire.features.stats.ui.generated.resources.heatmap_legend_more
import campfire.features.stats.ui.generated.resources.listening_heatmap_card_title
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

internal val MaxDayListeningDuration = 4.hours

@Composable
internal fun ListeningHeatmapCard(
  model: ListeningHeatmap,
  modifier: Modifier = Modifier,
  today: LocalDate = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date },
) {
  ElevatedCard(
    modifier = modifier
      .padding(
        horizontal = StatsDefaults.HorizontalPadding,
        vertical = StatsDefaults.VerticalPadding,
      ),
  ) {
    CardHeader(
      icon = {
        Icon(
          Icons.Rounded.CalendarMonth,
          contentDescription = null,
        )
      },
      title = {
        Text(stringResource(Res.string.listening_heatmap_card_title))
      },
    )

    HeatmapGrid(
      days = model.days,
      today = today,
      modifier = Modifier
        .padding(horizontal = 16.dp),
    )

    HeatmapLegend(
      modifier = Modifier
        .fillMaxWidth()
        .padding(
          horizontal = 16.dp,
          vertical = 12.dp,
        ),
    )
  }
}

@Composable
private fun HeatmapGrid(
  days: ImmutableMap<LocalDate, Duration>,
  today: LocalDate,
  modifier: Modifier = Modifier,
) {
  // Week columns, newest first so the reversed LazyRow starts on the current week
  val weekStarts = remember(days, today) {
    val currentWeekStart = today - DatePeriod(days = today.dayOfWeek.isoDayNumber - 1)
    val earliest = days.keys.minOrNull() ?: today
    val earliestWeekStart = earliest - DatePeriod(days = earliest.dayOfWeek.isoDayNumber - 1)
    val weekCount = (earliestWeekStart.daysUntil(currentWeekStart) / 7 + 1)
      .coerceIn(MinWeekCount, MaxWeekCount)
    List(weekCount) { currentWeekStart - DatePeriod(days = it * 7) }
  }

  Row(
    modifier = modifier,
  ) {
    DayOfWeekLabels()

    Spacer(Modifier.width(HeatmapCellSpacing))

    LazyRow(
      reverseLayout = true,
      horizontalArrangement = Arrangement.spacedBy(HeatmapCellSpacing),
      modifier = Modifier.weight(1f),
    ) {
      items(
        items = weekStarts,
        key = { it.toEpochDays() },
      ) { weekStart ->
        WeekColumn(
          weekStart = weekStart,
          days = days,
          maxDuration = MaxDayListeningDuration,
          today = today,
        )
      }
    }
  }
}

@Composable
private fun WeekColumn(
  weekStart: LocalDate,
  days: ImmutableMap<LocalDate, Duration>,
  maxDuration: Duration,
  today: LocalDate,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    verticalArrangement = Arrangement.spacedBy(HeatmapCellSpacing),
  ) {
    // Label the column that contains the first day of a month
    val monthStart = (0 until 7)
      .map { weekStart + DatePeriod(days = it) }
      .firstOrNull { it.day == 1 }
    Box(
      modifier = Modifier
        .height(MonthLabelHeight)
        .width(HeatmapCellSize),
    ) {
      if (monthStart != null) {
        Text(
          text = monthStart.month.name.take(3),
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          softWrap = false,
          modifier = Modifier
            .wrapContentWidth(Alignment.Start, unbounded = true)
            .align(Alignment.CenterStart),
        )
      }
    }

    repeat(7) { dayOffset ->
      val date = weekStart + DatePeriod(days = dayOffset)
      if (date > today) {
        Spacer(Modifier.size(HeatmapCellSize))
      } else {
        HeatmapCell(
          fraction = if (maxDuration == Duration.ZERO) {
            0f
          } else {
            ((days[date] ?: Duration.ZERO) / maxDuration).toFloat().coerceAtMost(1f)
          },
        )
      }
    }
  }
}

@Composable
private fun HeatmapCell(
  fraction: Float,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier = modifier
      .size(HeatmapCellSize)
      .background(
        color = heatmapCellColor(fraction),
        shape = RoundedCornerShape(3.dp),
      ),
  )
}

@Composable
private fun heatmapCellColor(fraction: Float): Color {
  return if (fraction <= 0f) {
    MaterialTheme.colorScheme.surfaceContainerHighest
  } else {
    lerp(
      MaterialTheme.colorScheme.primaryContainer,
      MaterialTheme.colorScheme.primary,
      fraction,
    )
  }
}

@Composable
private fun DayOfWeekLabels(
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(top = MonthLabelHeight + HeatmapCellSpacing),
    verticalArrangement = Arrangement.spacedBy(HeatmapCellSpacing),
  ) {
    DayOfWeek.entries.forEachIndexed { index, dayOfWeek ->
      Box(
        modifier = Modifier.height(HeatmapCellSize),
        contentAlignment = Alignment.CenterStart,
      ) {
        if (index % 2 == 0) {
          Text(
            text = dayOfWeek.name.take(3),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
          )
        }
      }
    }
  }
}

@Composable
private fun HeatmapLegend(
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(HeatmapCellSpacing, Alignment.End),
  ) {
    Text(
      text = stringResource(Res.string.heatmap_legend_less),
      style = MaterialTheme.typography.labelSmall,
    )
    HeatmapCell(fraction = 0f)
    HeatmapCell(fraction = 0.25f)
    HeatmapCell(fraction = 0.5f)
    HeatmapCell(fraction = 0.75f)
    HeatmapCell(fraction = 1f)
    Text(
      text = stringResource(Res.string.heatmap_legend_more),
      style = MaterialTheme.typography.labelSmall,
    )
  }
}

private val HeatmapCellSize = 12.dp
private val HeatmapCellSpacing = 3.dp
private val MonthLabelHeight = 16.dp
private const val MinWeekCount = 12
private const val MaxWeekCount = 52
