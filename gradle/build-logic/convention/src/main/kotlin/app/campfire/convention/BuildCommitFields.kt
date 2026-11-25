// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import org.gradle.api.Project

/**
 * The build commit, as the build config fields the app reads it from: the short SHA goes on the
 * version line everywhere, and Alpha builds also show its time and subject in App updates.
 *
 * Every value comes from the commit, never the wall clock, so a given commit always produces the
 * same output: F-Droid rebuilds the foss flavor from the release tag and must match our APK byte
 * for byte, and a per-build value would also defeat Gradle's build cache. There is deliberately no
 * "dirty" marker — F-Droid's `scandelete` leaves its checkout modified, which would differ from
 * ours. Without git there is no build commit at all: `SOURCE_DATE_EPOCH` isn't used, since
 * F-Droid's builder sets it itself and its value need not be the commit's time.
 */
data class BuildCommitFields(
  /** Commit time in epoch seconds, or 0 when unknown. */
  val commitEpochSeconds: Long,
  /** Short commit SHA, or empty when unknown. */
  val commitSha: String,
  /** The first line of the commit message, or empty when unknown. */
  val commitSubject: String,
) {

  /**
   * Declares `COMMIT_TIME` / `COMMIT_SHA` / `COMMIT_SUBJECT` through [field] (the Android or
   * BuildConfig-plugin `buildConfigField`). The app decodes them with `BuildCommit`.
   */
  fun declare(field: (type: String, name: String, value: String) -> Unit) {
    field("long", "COMMIT_TIME", "${commitEpochSeconds}L")
    field("String", "COMMIT_SHA", commitSha.toSourceLiteral())
    field("String", "COMMIT_SUBJECT", commitSubject.toSourceLiteral())
  }
}

/**
 * The build commit: HEAD, or the commit named by the `CAMPFIRE_BUILD_COMMIT` Gradle property. Pull
 * request CI builds GitHub's synthetic merge commit, whose SHA isn't on the branch and whose time
 * is when CI ran, so it passes the pull request's head commit instead.
 */
fun Project.campfireBuildCommit(): BuildCommitFields {
  val revision = providers.gradleProperty("CAMPFIRE_BUILD_COMMIT").orNull?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?.also { require(FULL_SHA_REGEX.matches(it)) { "CAMPFIRE_BUILD_COMMIT must be a full commit SHA, got '$it'" } }
    ?: "HEAD"

  // One process for every value, NUL-separated since the message has spaces and newlines. `%B`
  // rather than `%s`, which joins a first paragraph that wraps without a blank line into one line.
  // The full SHA is cut here rather than with `--short`, which lengthens the prefix when it's
  // ambiguous in the local clone — a shallow CI clone and F-Droid's full clone could then
  // disagree, and the foss APKs would differ.
  // showSignature off: with it set in the user's git config, signature text lands in the output.
  return parseBuildCommit(git("-c", "log.showSignature=false", "log", "-1", "--format=%ct%x00%H%x00%B", revision, "--"))
}

/** Reads `git log --format=%ct%x00%H%x00%B` output; null output (no git) is no build commit. */
internal fun parseBuildCommit(log: String?): BuildCommitFields {
  val fields = log?.split('\u0000', limit = 3)?.takeIf { it.size == 3 }
  return BuildCommitFields(
    commitEpochSeconds = fields?.get(0)?.trim()?.toLongOrNull() ?: 0L,
    commitSha = fields?.get(1)?.trim()?.take(SHORT_SHA_LENGTH).orEmpty(),
    commitSubject = fields?.get(2)?.trim()?.lineSequence()?.firstOrNull()?.trim().orEmpty(),
  )
}

/**
 * [this] as a string literal valid in both Java (Android's BuildConfig) and Kotlin (the BuildConfig
 * plugin's output). `$` would start a Kotlin template, so it's written as a unicode escape — safe
 * in Java too, which only resolves those to `$`. A quote or backslash must not be, since Java
 * resolves unicode escapes before reading the literal.
 */
internal fun String.toSourceLiteral(): String = buildString {
  append('"')
  for (char in this@toSourceLiteral) {
    when {
      char == '\\' -> append("\\\\")
      char == '"' -> append("\\\"")
      char == '$' -> append("\\u0024")
      char < ' ' -> append(' ')
      else -> append(char)
    }
  }
  append('"')
}

private const val SHORT_SHA_LENGTH = 7

private val FULL_SHA_REGEX = "[0-9a-f]{40}".toRegex()

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
