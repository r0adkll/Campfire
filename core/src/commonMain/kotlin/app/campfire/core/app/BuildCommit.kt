// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.core.app

import kotlin.time.Instant

/**
 * The commit a build came from. Builds without git have none; any part can still be missing on
 * its own, e.g. a commit with an empty message has no subject.
 */
data class BuildCommit(
  /** When the commit was made — not when the app was built. */
  val time: Instant?,
  /** Short SHA. */
  val sha: String?,
  /** The commit message's first line. */
  val subject: String? = null,
) {
  companion object {
    /**
     * Decodes the `COMMIT_TIME` / `COMMIT_SHA` / `COMMIT_SUBJECT` build config fields each app
     * module generates, where `0` and `""` mean unknown. Null when all are.
     */
    fun fromBuildConfig(epochSeconds: Long, sha: String, subject: String): BuildCommit? {
      val commit = BuildCommit(
        time = epochSeconds.takeIf { it > 0 }?.let(Instant::fromEpochSeconds),
        sha = sha.ifEmpty { null },
        subject = subject.ifBlank { null },
      )
      return commit.takeIf { it.time != null || it.sha != null || it.subject != null }
    }
  }
}
