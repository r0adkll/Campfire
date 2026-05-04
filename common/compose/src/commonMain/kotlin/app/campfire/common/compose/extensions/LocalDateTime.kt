package app.campfire.common.compose.extensions

import androidx.compose.runtime.Composable
import app.campfire.core.extensions.capitalized
import app.campfire.core.extensions.epochMilliseconds
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.relative_day_today
import campfire.common.compose.generated.resources.relative_day_yesterday
import campfire.common.compose.generated.resources.time_ago_days
import campfire.common.compose.generated.resources.time_ago_hours
import campfire.common.compose.generated.resources.time_ago_minutes
import campfire.common.compose.generated.resources.time_ago_months
import campfire.common.compose.generated.resources.time_ago_now
import campfire.common.compose.generated.resources.time_ago_years
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

val LocalDateTime.timeAgo: String
  @Composable get() {
    return epochMilliseconds.timeAgo
  }

val Long.timeAgo: String
  @Composable get() {
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val elapsedMs = nowMs - this
    val elapsedDuration = elapsedMs.milliseconds

    return when {
      elapsedDuration < 1.minutes -> stringResource(Res.string.time_ago_now)
      elapsedDuration < 1.hours -> stringResource(
        Res.string.time_ago_minutes,
        (elapsedDuration.inWholeMinutes % 60).toInt(),
      )

      elapsedDuration < 1.days -> stringResource(Res.string.time_ago_hours, (elapsedDuration.inWholeHours % 24).toInt())
      elapsedDuration < 30.days -> stringResource(Res.string.time_ago_days, elapsedDuration.inWholeDays.toInt())
      elapsedDuration < 365.days -> stringResource(
        Res.string.time_ago_months,
        (elapsedDuration.inWholeDays / 30).toInt(),
      )

      else -> stringResource(Res.string.time_ago_years, (elapsedDuration.inWholeDays / 365).toInt())
    }
  }

val LocalDate.relativeDayLabel: String
  @Composable get() {
    val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
    val yesterday = today.minus(1, DateTimeUnit.DAY)
    return when (this) {
      today -> stringResource(Res.string.relative_day_today)
      yesterday -> stringResource(Res.string.relative_day_yesterday)
      else -> if (year == today.year) {
        "${month.name.capitalized()} $day"
      } else {
        "${month.name.capitalized()} $day, $year"
      }
    }
  }

@Composable
fun LocalDate.asRelativeDayLabel(
  monthStyle: ReadoutStyle,
): String {
  val today = Clock.System.now().toLocalDateTime(TimeZone.UTC).date
  val yesterday = today.minus(1, DateTimeUnit.DAY)

  val monthName = { month: Month ->
    when (monthStyle) {
      ReadoutStyle.Long -> month.name.capitalized()
      ReadoutStyle.Short -> month.name.capitalized().take(3)
    }
  }

  return when (this) {
    today -> stringResource(Res.string.relative_day_today)
    yesterday -> stringResource(Res.string.relative_day_yesterday)
    else -> if (year == today.year) {
      "${monthName(month)} $day"
    } else {
      "${monthName(month)} $day, $year"
    }
  }
}
