// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention.changelog

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

data class ChangelogVersion(
  val version: String,
  val date: String?,
  val changeSets: List<ChangeSet>,
) {
  data class ChangeSet(
    val name: String?,
    val changes: List<Change>,
  )

  data class Change(
    val text: String,
    val platforms: Set<ChangelogPlatform>,
  )
}

/**
 * Parses a Keep-a-Changelog style CHANGELOG.md into the JSON consumed by the whats-new
 * feature (`changelog.json`), resolving every entry to the platforms it applies to.
 *
 * Entries may start with a platform tag list — `- [Desktop] …`, `- [Android, iOS] …` — and an
 * unknown tag fails the parse. Untagged entries apply to the platforms that had shipped by that
 * section's release (see [ChangelogPlatform.firstRelease]).
 */
object ChangelogParser {

  const val UNRELEASED = "Unreleased"

  fun parse(file: File): List<ChangelogVersion> = parse(file.readLines())

  fun parse(lines: List<String>): List<ChangelogVersion> {
    val sections = parseSections(lines)
    val versions = sections.map { it.version }
    return sections.mapIndexed { index, section ->
      val untagged = ChangelogPlatform.entries.filterTo(mutableSetOf()) { platform ->
        section.version == UNRELEASED ||
          platform.firstRelease?.let { versions.indexOf(it) }?.let { it >= index } == true
      }
      ChangelogVersion(
        version = section.version,
        date = section.date,
        changeSets = section.changeSets.map { (name, entries) ->
          ChangelogVersion.ChangeSet(
            name = name,
            changes = entries.map { it.resolve(untagged) },
          )
        },
      )
    }
  }

  private fun parseSections(lines: List<String>): List<Section> {
    val sections = mutableListOf<Section>()
    var current: Section? = null

    lines.forEach { line ->
      if (line.isBlank()) return@forEach
      if (line.startsWith("# ")) return@forEach
      if (VERSION_KEY_REGEX.matches(line)) return@forEach

      if (line.startsWith("## ")) {
        val match = VERSION_REGEX.find(line) ?: error("Unable to find version in '$line'")
        current = Section(
          version = match.groupValues[1],
          date = match.groups[3]?.value,
        ).also { sections += it }
      } else {
        val section = current ?: return@forEach
        if (line.startsWith("### ")) {
          section.changeSets += line.removePrefix("### ").trim() to mutableListOf()
        } else {
          if (section.changeSets.isEmpty()) section.changeSets += null to mutableListOf()
          section.changeSets.last().second += line.trim().removePrefix("- ")
        }
      }
    }

    return sections
  }

  private fun String.resolve(untagged: Set<ChangelogPlatform>): ChangelogVersion.Change {
    val match = TAG_REGEX.matchEntire(this) ?: return ChangelogVersion.Change(this, untagged)
    val platforms = match.groupValues[1].split(",").mapTo(mutableSetOf()) { tag ->
      ChangelogPlatform.fromTag(tag.trim()) ?: error(
        "Unknown platform '${tag.trim()}' in CHANGELOG.md entry '$this'. " +
          "Expected one of: ${ChangelogPlatform.entries.joinToString { it.tag }}",
      )
    }
    return ChangelogVersion.Change(match.groupValues[2], platforms)
  }

  /** Encodes to the compact JSON array read by `WhatsNewRepositoryImpl`. */
  fun toJson(versions: List<ChangelogVersion>): String {
    val array = buildJsonArray {
      versions.forEach { add(it.toJsonElement()) }
    }
    return Json.encodeToString(JsonElement.serializer(), array)
  }

  private fun ChangelogVersion.toJsonElement(): JsonElement = buildJsonObject {
    put("version", version)
    put("date", date)
    putJsonArray("changes") {
      changeSets.forEach { set ->
        addJsonObject {
          put("name", set.name)
          putJsonArray("changes") {
            set.changes.forEach { change ->
              addJsonObject {
                put("text", change.text)
                putJsonArray("platforms") {
                  ChangelogPlatform.entries
                    .filter { it in change.platforms }
                    .forEach { add(it.name) }
                }
              }
            }
          }
        }
      }
    }
  }

  private class Section(
    val version: String,
    val date: String?,
  ) {
    val changeSets = mutableListOf<Pair<String?, MutableList<String>>>()
  }

  private val VERSION_REGEX = "^## \\[([a-zA-Z0-9._-]+)\\]( - (\\d{4}-\\d{2}-\\d{2}))?$".toRegex()
  private val VERSION_KEY_REGEX = "^\\[([a-zA-Z0-9._-]+)\\]:.*$".toRegex()
  private val TAG_REGEX = "^\\[([^\\]]+)\\]\\s+(.+)$".toRegex()
}
