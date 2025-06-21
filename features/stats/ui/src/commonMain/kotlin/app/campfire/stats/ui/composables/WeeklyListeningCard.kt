package app.campfire.stats.ui.composables

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingFlat
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import app.campfire.common.compose.extensions.largestDurationUnit
import app.campfire.common.compose.extensions.thresholdReadoutFormat
import app.campfire.core.extensions.toString
import app.campfire.stats.ui.StatsUiModel.WeeklyListening
import campfire.features.stats.ui.generated.resources.Res
import campfire.features.stats.ui.generated.resources.weekly_listening_card_title
import kotlin.math.ceil
import kotlin.time.Duration
import kotlin.time.toDuration
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun WeeklyListeningCard(
  model: WeeklyListening,
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
          Icons.Rounded.Hearing,
          contentDescription = null,
        )
      },
      title = {
        Text(stringResource(Res.string.weekly_listening_card_title))
      },
    )

    TotalTime(
      totalTime = model.weekTime,
      weekOverWeekChange = model.weekOverWeekChange,
    )

    WeeklyGraph(
      thisWeek = model.thisWeek,
      lastWeek = model.lastWeek,
      today = today,
      contentPadding = PaddingValues(
        horizontal = 8.dp,
      ),
    )

    GraphLabels(
      today = today,
      modifier = Modifier
        .padding(
          horizontal = 8.dp,
          vertical = 8.dp,
        ),
    )
  }
}

@Composable
private fun TotalTime(
  totalTime: Duration,
  weekOverWeekChange: Double,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .padding(
        vertical = 8.dp,
        horizontal = 16.dp,
      ),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(
      text = totalTime.thresholdReadoutFormat(),
      style = MaterialTheme.typography.displayMedium,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.primary,
    )

    Spacer(Modifier.width(4.dp))

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val color = when {
        weekOverWeekChange > 0 -> MaterialTheme.colorScheme.secondary
        weekOverWeekChange < 0 -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
      }
      Icon(
        when {
          weekOverWeekChange > 0 -> Icons.AutoMirrored.Rounded.TrendingUp
          weekOverWeekChange < 0 -> Icons.AutoMirrored.Rounded.TrendingDown
          else -> Icons.AutoMirrored.Rounded.TrendingFlat
        },
        contentDescription = null,
        tint = color,
      )
      Text(
        text = "${weekOverWeekChange.toFloat().toString(1)}%",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
      )
    }
  }
}

@Composable
private fun WeeklyGraph(
  thisWeek: ImmutableMap<LocalDate, Duration>,
  lastWeek: ImmutableMap<LocalDate, Duration>,
  today: LocalDate,
  modifier: Modifier = Modifier,
  thisWeekColor: Color = MaterialTheme.colorScheme.primary,
  lastWeekColor: Color = MaterialTheme.colorScheme.secondaryContainer,
  contentPadding: PaddingValues = PaddingValues(0.dp),
) {
  // Calculate axis information
  val maxDuration = remember(thisWeek, lastWeek) {
    maxOf(
      thisWeek.values.maxOrNull() ?: Duration.ZERO,
      lastWeek.values.maxOrNull() ?: Duration.ZERO,
    )
  }
  val largestUnit = remember(maxDuration) { maxDuration.largestDurationUnit() }
  val yAxisValue = remember(maxDuration, largestUnit) { ceil(maxDuration.toDouble(largestUnit)) }

  // Compute this week and last week's path
  val thisWeekPath = remember { Path() }
  val lastWeekPath = remember { Path() }

  Box(
    modifier = modifier
      .fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier
        .matchParentSize()
        .padding(horizontal = 8.dp),
      horizontalAlignment = Alignment.End,
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      VerticalAxisLabel { Text(yAxisValue.toDuration(largestUnit).toString(largestUnit, 0)) }
      VerticalAxisLabel()
      VerticalAxisLabel()
    }

    Canvas(
      modifier = Modifier
        .height(180.dp)
        .fillMaxWidth()
        .padding(contentPadding)
        .zIndex(1f),
    ) {
      thisWeekPath.reset()
      lastWeekPath.reset()

      var lastThisWeekOffset: Offset? = null
      var lastLastWeekOffset: Offset? = null

      for (i in 6 downTo 0) {
        val thisWeekDayOffset = today - DatePeriod(days = i)
        val lastWeekDayOffset = today - DatePeriod(days = i + 7)

        val thisWeekDayDuration = thisWeek[thisWeekDayOffset] ?: Duration.ZERO
        val lastWeekDayDuration = lastWeek[lastWeekDayOffset] ?: Duration.ZERO

        val columnWidth = size.width / 7
        val x = (columnWidth / 2f) + (columnWidth * (6 - i))

        val pathHeight = size.height - TopGraphPadding.toPx()
        val thisWeekY = size.height - pathHeight * (thisWeekDayDuration / maxDuration)
        val lastWeekY = size.height - pathHeight * (lastWeekDayDuration / maxDuration)

        val thisWeekOffset = Offset(x, thisWeekY.toFloat())
        val lastWeekOffset = Offset(x, lastWeekY.toFloat())

        if (lastThisWeekOffset == null) {
          thisWeekPath.moveTo(thisWeekOffset.x, thisWeekOffset.y)
        } else {
          thisWeekPath.buildCurveLine(lastThisWeekOffset, thisWeekOffset)
        }

        if (lastLastWeekOffset == null) {
          lastWeekPath.moveTo(lastWeekOffset.x, lastWeekOffset.y)
        } else {
          lastWeekPath.buildCurveLine(lastLastWeekOffset, lastWeekOffset)
        }

        lastThisWeekOffset = thisWeekOffset
        lastLastWeekOffset = lastWeekOffset
      }

      drawPath(
        path = lastWeekPath,
        color = lastWeekColor,
        style = Stroke(
          width = LastWeekLineWidth.toPx(),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round,
          pathEffect = PathEffect.dashPathEffect(
            intervals = floatArrayOf(16.dp.toPx(), 16.dp.toPx()),
          ),
        ),
      )

      drawPath(
        path = thisWeekPath,
        color = thisWeekColor,
        style = Stroke(
          width = ThisWeekLineWidth.toPx(),
          cap = StrokeCap.Round,
          join = StrokeJoin.Round,
        ),
      )
    }
  }
}

private val TopGraphPadding = 16.dp
private val LastWeekLineWidth = 6.dp
private val ThisWeekLineWidth = 8.dp

private fun Path.buildCurveLine(startPoint: Offset, endPoint: Offset) {
  val firstControlPoint = Offset(
    x = startPoint.x + (endPoint.x - startPoint.x) / 2F,
    y = startPoint.y,
  )
  val secondControlPoint = Offset(
    x = startPoint.x + (endPoint.x - startPoint.x) / 2F,
    y = endPoint.y,
  )
  cubicTo(
    x1 = firstControlPoint.x,
    y1 = firstControlPoint.y,
    x2 = secondControlPoint.x,
    y2 = secondControlPoint.y,
    x3 = endPoint.x,
    y3 = endPoint.y,
  )
}

@Composable
private fun VerticalAxisLabel(
  modifier: Modifier = Modifier,
  label: @Composable () -> Unit = {},
) {
  Row(
    modifier = modifier.height(16.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
  ) {
    ProvideTextStyle(
      MaterialTheme.typography.labelSmall.copy(
        fontWeight = FontWeight.Bold,
      ),
    ) {
      label()
    }
    Box(
      modifier = Modifier
        .background(
          color = MaterialTheme.colorScheme.outline,
          shape = CircleShape,
        )
        .size(4.dp, 2.dp),
    )
  }
}

@Composable
private fun GraphLabels(
  today: LocalDate,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier
      .height(36.dp)
      .fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceAround,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    repeat(7) { offset ->
      val date = today - DatePeriod(days = 6 - offset)
      Text(
        text = date.dayOfWeek.name.take(3).uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
      )
    }
  }
}
