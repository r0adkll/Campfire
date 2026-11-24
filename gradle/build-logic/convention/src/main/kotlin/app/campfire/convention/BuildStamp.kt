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
) {

  /**
   * Declares the stamp as `COMMIT_TIME` / `COMMIT_SHA` build config fields through [field] (the
   * Android or BuildConfig-plugin `buildConfigField`). The app decodes them with `BuildCommit`.
   */
  fun declareFields(field: (type: String, name: String, value: String) -> Unit) {
    field("long", "COMMIT_TIME", "${commitEpochSeconds}L")
    field("String", "COMMIT_SHA", "\"$commitSha\"")
  }
}

fun Project.campfireBuildStamp(): BuildStamp {
  // One process for both values. The full SHA is cut here rather than with `--short`, which
  // lengthens the prefix when it's ambiguous in the local clone — a shallow CI clone and F-Droid's
  // full clone could then disagree, and the foss APKs would differ.
  // showSignature off: with it set in the user's git config, signature text lands in the output.
  val (epoch, sha) = git("-c", "log.showSignature=false", "log", "-1", "--format=%ct %H")
    ?.split(' ')
    ?.takeIf { it.size == 2 }
    ?.let { (time, hash) -> time.toLongOrNull() to hash.take(SHORT_SHA_LENGTH) }
    ?: (null to "")
  return BuildStamp(
    commitEpochSeconds = epoch
      ?: providers.environmentVariable("SOURCE_DATE_EPOCH").orNull?.trim()?.toLongOrNull()
      ?: 0L,
    commitSha = sha,
  )
}

private const val SHORT_SHA_LENGTH = 7

private fun Project.git(vararg args: String): String? = runCatching {
  val result = providers.exec {
    commandLine("git", *args)
    workingDir = rootDir
    // Look for a repository at the project root only: a source tarball unpacked inside some other
    // checkout must not pick up that checkout's HEAD.
    rootDir.parentFile?.let { environment("GIT_CEILING_DIRECTORIES", it.absolutePath) }
    isIgnoreExitValue = true
  }
  if (result.result.get().exitValue != 0) return null
  result.standardOutput.asText.get().trim().takeIf { it.isNotEmpty() }
}.getOrNull()
