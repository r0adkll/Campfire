package app.campfire.core.time

import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface FatherTime {

  fun now(): LocalDateTime
  fun today(): LocalDate
  fun nowInEpochMillis(): Long
}

object GrandFatherTime : FatherTime {

  override fun now(): LocalDateTime {
    return Clock.System.now()
      .toLocalDateTime(TimeZone.currentSystemDefault())
  }

  override fun today(): LocalDate {
    return now().date
  }

  override fun nowInEpochMillis(): Long {
    return Clock.System.now().toEpochMilliseconds()
  }
}
