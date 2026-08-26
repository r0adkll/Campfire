// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.model

/**
 * A comparable Audiobookshelf server version, parsed from [Server.Settings.version].
 *
 * Server version strings are semver-ish and may carry a `v` prefix or a git-describe style
 * suffix (e.g. `v2.22.0`, `2.34.0-4-ge39e8d8c`); anything past major.minor.patch is ignored
 * for comparison purposes.
 */
data class ServerVersion(
  val major: Int,
  val minor: Int,
  val patch: Int,
) : Comparable<ServerVersion> {

  override fun compareTo(other: ServerVersion): Int {
    return compareValuesBy(this, other, { it.major }, { it.minor }, { it.patch })
  }

  override fun toString(): String = "$major.$minor.$patch"

  companion object {
    /**
     * Parses a server version string, returning null when it doesn't lead with a
     * major.minor(.patch) core. A missing patch component is treated as 0.
     */
    fun parse(raw: String): ServerVersion? {
      val core = raw.trim()
        .removePrefix("v")
        .takeWhile { it.isDigit() || it == '.' }
        .trimEnd('.')
      if (core.isEmpty()) return null

      val parts = core.split('.')
      val major = parts.getOrNull(0)?.toIntOrNull() ?: return null
      val minor = parts.getOrNull(1)?.toIntOrNull() ?: return null
      val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
      return ServerVersion(major, minor, patch)
    }
  }
}
