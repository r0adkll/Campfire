// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention.changelog

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

data class ChangelogVersion(
  val version: String,
  val date: String?,
  val changeSets: List<ChangeSet>,
) {
  data class ChangeSet(
    val name: String?,
    val changes: List<String>,
  )
}

/**
 * Parses a Keep-a-Changelog style CHANGELOG.md into the JSON consumed by the whats-new
 * feature (`app.campfire.whatsnew.api.VersionChanges` via `changelog.json`).
 *
 * Same algorithm as `ChangelogParser` in `scripts/app` (ParseChangelogCommand.kt) — keep
 * the two in sync if the CHANGELOG.md structure ever changes. Two long-standing quirks
 * are intentionally preserved for output parity with the CLI (see ChangelogParserTest):
 * a version header without a date yields an empty-string date (not null), and the final
 * version section in the file is only captured when followed by another `## ` header —
 * trailing link-reference lines (`[1.0.0]: https://…`) do not flush it.
 */
object ChangelogParser {

  fun parse(file: File): List<ChangelogVersion> = parse(file.readLines())

  fun parse(lines: List<String>): List<ChangelogVersion> {
    val versions = mutableListOf<ChangelogVersion>()
    val changeSets = mutableListOf<ChangelogVersion.ChangeSet>()

    var currentVersion: String? = null
    var currentDate: String? = null
    var currentChangeSet: String? = null
    val currentChanges = mutableListOf<String>()

    lines.forEach { line ->
      if (line.isBlank()) return@forEach
      if (line.startsWith("# ")) return@forEach
      if (VERSION_KEY_REGEX.matches(line)) return@forEach

      if (line.startsWith("## ")) {
        if (currentVersion != null) {
          changeSets += ChangelogVersion.ChangeSet(currentChangeSet, currentChanges.toList())
          versions += ChangelogVersion(currentVersion!!, currentDate, changeSets.toList())

          currentVersion = null
          currentDate = null
          currentChangeSet = null
          changeSets.clear()
          currentChanges.clear()
        }

        val match = VERSION_REGEX.find(line)
        if (match == null || match.groupValues.size < 2) error("Unable to find version in '$line'")
        currentVersion = match.groupValues[1]
        currentDate = match.groupValues.getOrNull(3)
      } else if (currentVersion != null) {
        if (line.startsWith("### ")) {
          if (currentChangeSet != null) {
            changeSets += ChangelogVersion.ChangeSet(currentChangeSet, currentChanges.toList())
            currentChanges.clear()
          }
          currentChangeSet = line.removePrefix("### ").trim()
        } else {
          currentChanges += line.trim().removePrefix("- ")
        }
      }
    }

    return versions
  }

  /**
   * Encodes to the exact wire format `./campfire changelog -aj` produces: a compact JSON
   * array with explicit nulls, in declaration field order.
   */
  fun toJson(versions: List<ChangelogVersion>): String {
    val array = buildJsonArray {
      versions.forEach { add(it.toJsonElement()) }
    }
    return Json.encodeToString(JsonElement.serializer(), array)
  }

  private fun ChangelogVersion.toJsonElement(): JsonElement = buildJsonObject {
    put("version", version)
    put("date", date)
    put(
      "changes",
      buildJsonArray {
        changeSets.forEach { set ->
          add(
            buildJsonObject {
              put("name", set.name)
              put(
                "changes",
                buildJsonArray {
                  set.changes.forEach { add(JsonPrimitive(it)) }
                },
              )
            },
          )
        }
      },
    )
  }

  private val VERSION_REGEX = "^## \\[([a-zA-Z0-9._-]+)\\]( - (\\d{4}-\\d{2}-\\d{2}))?$".toRegex()
  private val VERSION_KEY_REGEX = "^\\[([a-zA-Z0-9._-]+)\\]:.*$".toRegex()
}
