package app.campfire.common.compose.extensions

import androidx.compose.runtime.Composable
import campfire.common.compose.generated.resources.Res
import campfire.common.compose.generated.resources.time_ago_days
import campfire.common.compose.generated.resources.time_ago_hours
import campfire.common.compose.generated.resources.time_ago_minutes
import campfire.common.compose.generated.resources.time_ago_months
import campfire.common.compose.generated.resources.time_ago_now
import campfire.common.compose.generated.resources.time_ago_years
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.stringResource

val LocalDateTime.timeAgo: String
  @Composable get() {
    val thisMs = toInstant(TimeZone.UTC).toEpochMilliseconds()
    return thisMs.timeAgo
  }

val Long.timeAgo: String
  @Composable get() {
    val nowMs = Clock.System.now().toEpochMilliseconds()
    val elapsedMs = nowMs - this
    val elapsedDuration = elapsedMs.milliseconds

    return when {
      elapsedDuration < 5.minutes -> stringResource(Res.string.time_ago_now)
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
