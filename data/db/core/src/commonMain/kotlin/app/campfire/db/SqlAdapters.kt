// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.db

import app.campfire.core.model.BasicSearchResult
import app.campfire.core.model.SeriesSequence
import app.cash.sqldelight.ColumnAdapter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

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

// Series names can contain arbitrary characters, so encode as JSON rather than
// a delimiter-separated string.
object SeriesSequenceListAdapter : ColumnAdapter<List<SeriesSequence>, String> {
  override fun decode(databaseValue: String): List<SeriesSequence> {
    if (databaseValue.isEmpty()) return emptyList()
    return Json.parseToJsonElement(databaseValue).jsonArray.mapNotNull { element ->
      val obj = element.jsonObject
      SeriesSequence(
        id = obj["id"]?.jsonPrimitive?.content ?: return@mapNotNull null,
        name = obj["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
        sequence = obj["sequence"]?.jsonPrimitive?.doubleOrNull ?: SeriesSequence.UNKNOWN_SEQUENCE,
      )
    }
  }

  override fun encode(value: List<SeriesSequence>): String {
    return buildJsonArray {
      value.forEach { series ->
        addJsonObject {
          put("id", series.id)
          put("name", series.name)
          put("sequence", series.sequence)
        }
      }
    }.toString()
  }
}

object BasicSearchResultListAdapter : ColumnAdapter<List<BasicSearchResult>, String> {
  private const val ITEM_SEPARATOR = "|"
  private const val FIELD_SEPARATOR = ";;"

  override fun decode(databaseValue: String): List<BasicSearchResult> {
    return databaseValue.split(ITEM_SEPARATOR).mapNotNull { rawValue ->
      val parts = rawValue.split(FIELD_SEPARATOR)
      if (parts.size != 2) return@mapNotNull null
      BasicSearchResult(
        name = parts[0],
        numItems = parts[1].toIntOrNull() ?: 0,
      )
    }
  }

  override fun encode(value: List<BasicSearchResult>): String {
    return value.joinToString(ITEM_SEPARATOR) {
      "${it.name}$FIELD_SEPARATOR${it.numItems}"
    }
  }
}
