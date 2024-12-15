package app.campfire.db

import app.cash.sqldelight.ColumnAdapter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime

object StringListAdapter : ColumnAdapter<List<String>, String> {
  override fun decode(databaseValue: String): List<String> {
    return if (databaseValue.isEmpty()) {
      listOf()
    } else {
      databaseValue.split(",")
    }
  }

  override fun encode(value: List<String>) = value.joinToString(separator = ",")
}

object StringSetAdapter : ColumnAdapter<Set<String>, String> {
  override fun decode(databaseValue: String): Set<String> {
    return if (databaseValue.isEmpty()) {
      setOf()
    } else {
      databaseValue.split(",").toSet()
    }
  }

  override fun encode(value: Set<String>) = value.joinToString(separator = ",")
}

object IntListAdapter : ColumnAdapter<List<Int>, String> {
  override fun decode(databaseValue: String): List<Int> {
    return if (databaseValue.isEmpty()) {
      listOf()
    } else {
      databaseValue.split(",").map { it.toInt() }
    }
  }

  override fun encode(value: List<Int>) = value.joinToString(separator = ",")
}

object LocalDateAdapter : ColumnAdapter<LocalDate, String> {
  override fun decode(databaseValue: String): LocalDate = LocalDate.parse(databaseValue)
  override fun encode(value: LocalDate): String = value.toString()
}

object LocalDateTimeAdapter : ColumnAdapter<LocalDateTime, String> {
  override fun decode(databaseValue: String): LocalDateTime = LocalDateTime.parse(databaseValue)
  override fun encode(value: LocalDateTime): String = value.toString()
}

object DurationAdapter : ColumnAdapter<Duration, Long> {
  override fun decode(databaseValue: Long): Duration = databaseValue.milliseconds
  override fun encode(value: Duration): Long = value.inWholeMilliseconds
}

object UuidAdapter : ColumnAdapter<Uuid, String> {
  override fun decode(databaseValue: String): Uuid = Uuid.parse(databaseValue)
  override fun encode(value: Uuid): String = value.toString()
}
