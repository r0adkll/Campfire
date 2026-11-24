// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.app

import kotlin.time.Instant

/**
 * The commit a build came from. Either half can be unknown on its own: a build without a git
 * checkout may still have a time from `SOURCE_DATE_EPOCH`.
 */
data class BuildCommit(
  /** When the commit was made — not when the app was built. */
  val time: Instant?,
  /** Short SHA. */
  val sha: String?,
) {
  companion object {
    /**
     * Decodes the `COMMIT_TIME` / `COMMIT_SHA` build config fields each app module generates,
     * where `0` and `""` mean unknown. Null when both are.
     */
    fun fromBuildConfig(epochSeconds: Long, sha: String): BuildCommit? {
      val commit = BuildCommit(
        time = epochSeconds.takeIf { it > 0 }?.let(Instant::fromEpochSeconds),
        sha = sha.ifEmpty { null },
      )
      return commit.takeIf { it.time != null || it.sha != null }
    }
  }
}
