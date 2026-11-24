// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import org.gradle.api.Project

/**
 * Identifies the source a build came from, shown under Version in About.
 *
 * Both values come from the HEAD commit, never the wall clock, so a given commit always produces
 * the same output: F-Droid rebuilds the foss flavor from the release tag and must match our APK
 * byte for byte, and a per-build value would also defeat Gradle's build cache. There is
 * deliberately no "dirty" marker — F-Droid's `scandelete` leaves its checkout modified, which
 * would differ from ours.
 *
 * Git wins over `SOURCE_DATE_EPOCH`: F-Droid's builder sets that variable itself, and its value
 * need not match the commit. It is only a fallback for builds without a git checkout.
 */
data class BuildStamp(
  /** Commit time in epoch seconds, or 0 when unknown. */
  val commitEpochSeconds: Long,
  /** Short commit SHA, or empty when unknown. */
  val commitSha: String,
)

fun Project.campfireBuildStamp(): BuildStamp {
  val epoch = git("log", "-1", "--format=%ct")?.toLongOrNull()
    ?: providers.environmentVariable("SOURCE_DATE_EPOCH").orNull?.trim()?.toLongOrNull()
    ?: 0L
  val sha = git("rev-parse", "--short=7", "HEAD").orEmpty()
  return BuildStamp(commitEpochSeconds = epoch, commitSha = sha)
}

private fun Project.git(vararg args: String): String? = runCatching {
  val result = providers.exec {
    commandLine("git", *args)
    workingDir = rootDir
    isIgnoreExitValue = true
  }
  if (result.result.get().exitValue != 0) return null
  result.standardOutput.asText.get().trim().takeIf { it.isNotEmpty() }
}.getOrNull()
