// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:DependsOn("com.gianluz:danger-kotlin-android-lint-plugin:0.1.0")

import com.gianluz.dangerkotlin.androidlint.AndroidLint
import com.gianluz.dangerkotlin.androidlint.androidLint
import systems.danger.kotlin.*
import systems.danger.kotlin.models.github.GitHubUserType

private val MIGRATION_VERSION_REGEX = "([0-9]+)\\.sqm\$".toRegex()

// CHANGELOG.md platform tags — keep in sync with ChangelogPlatform in gradle/build-logic.
private val ANDROID = "Android"
private val DESKTOP = "Desktop"
private val IOS = "iOS"
private val PLATFORMS = listOf(ANDROID, DESKTOP, IOS)
private val PLATFORM_TAG_REGEX = "^- \\[[^\\]]+\\]\\s+.+".toRegex()

// Which platforms ship the code under a path fragment (matched against "/<file path>").
private val PLATFORM_SOURCES = listOf(
  "/src/commonMain/" to PLATFORMS,
  "/src/skikoMain/" to listOf(DESKTOP, IOS),
  "/src/jvmShared/" to listOf(ANDROID, DESKTOP),
  "/src/androidMain/" to listOf(ANDROID),
  "/app/android/src/" to listOf(ANDROID),
  "/src/jvmMain/" to listOf(DESKTOP),
  "/app/desktop/src/" to listOf(DESKTOP),
  "/infra/audioplayer/engine-ffmpeg/src/main/" to listOf(DESKTOP),
  "/infra/audioplayer/engine-vlc/src/main/" to listOf(DESKTOP),
  "/src/iosMain/" to listOf(IOS),
  "/src/appleMain/" to listOf(IOS),
  "/src/nativeMain/" to listOf(IOS),
  "/app/ios/" to listOf(IOS),
)

/** The platforms whose shipped code [files] touch, in [PLATFORMS] order. */
private fun platformsTouchedBy(files: List<String>): List<String> {
  val touched = files.flatMapTo(mutableSetOf()) { file ->
    PLATFORM_SOURCES.filter { (fragment, _) -> "/$file".contains(fragment) }.flatMap { it.second }
  }
  return PLATFORMS.filter { it in touched }
}

/** Entry lines this PR adds to CHANGELOG.md, or null when git can't diff the PR. */
private fun addedChangelogEntries(baseSha: String, headSha: String): List<String>? {
  val process = ProcessBuilder("git", "diff", "--unified=0", "$baseSha...$headSha", "--", "CHANGELOG.md")
    .redirectErrorStream(true)
    .start()
  val output = process.inputStream.bufferedReader().readText()
  if (process.waitFor() != 0) return null
  return output.lines()
    .filter { it.startsWith("+- ") }
    .map { it.removePrefix("+") }
}

Danger register AndroidLint

/**
 * Install the 'Danger Kotlin' IntelliJ Plugin for easy editing of this file!
 * See https://r0adkll.github.io/danger-kotlin/intellij-plugin
 */
danger(args) {

  val allSourceFiles = git.modifiedFiles + git.createdFiles
  val changelogChanged = allSourceFiles.contains("CHANGELOG.md")
  val sourceChanges = allSourceFiles.firstOrNull { it.contains("src") }

  onGitHub {
    if (pullRequest.user.type == GitHubUserType.BOT) {
      message("This PR is part of some automation, skip...")
      return@danger
    }

    val isTrivial = pullRequest.title.contains("#trivial")

    message("This PR has been checked by Danger")

    // Changelog
    if (!isTrivial && !changelogChanged && sourceChanges != null) {
      warn(
        "Any changes to library code should be reflected in the Changelog.\n\n" +
          "Please consider adding a note there and adhere to the " +
          "[Changelog Guidelines](https://github.com/Moya/contributors/blob/master/Changelog%20Guidelines.md).",
      )
    }

    // Changelog platform tags: a PR that only ships code for some platforms shouldn't add
    // untagged entries, which the app and store notes show on every platform.
    val touchedPlatforms = platformsTouchedBy(allSourceFiles + git.deletedFiles)
    if (changelogChanged && touchedPlatforms.isNotEmpty() && touchedPlatforms != PLATFORMS) {
      val entries = addedChangelogEntries(pullRequest.base.sha, pullRequest.head.sha)
      if (entries == null) {
        message("Couldn't diff CHANGELOG.md to check its platform tags")
      } else {
        val untagged = entries.filterNot { PLATFORM_TAG_REGEX.matches(it) }
        if (untagged.isNotEmpty()) {
          val platforms = touchedPlatforms.joinToString(" and ")
          warn(
            "This PR only changes $platforms code, but these CHANGELOG.md entries have no platform tag, " +
              "so they'll be listed on every platform:\n\n" +
              untagged.joinToString("\n") +
              "\n\nIf they only apply to $platforms, tag them: `- [${touchedPlatforms.joinToString()}] …`",
          )
        }
      }
    }

    // Big PR Check
    if ((pullRequest.additions ?: 0) - (pullRequest.deletions ?: 0) > 300) {
      warn("Big PR, try to keep changes smaller if you can")
    }

    // Work in progress check
    if (pullRequest.title.contains("WIP", false)) {
      warn("PR is classed as Work in Progress")
    }

    if (git.linesOfCode > 500) {
      warn("This PR is original Xbox Huge! Consider breaking into smaller PRs")
    }

    // Check if the user has made any DB schema modifications
    val ignoreDbChanges = issue.labels.any { it.name == "ignore-db-change" }
    val hasSchemaChanges = allSourceFiles.any { it.endsWith(".sq") }
    if (hasSchemaChanges && !ignoreDbChanges) {
      // Check for Migration
      val migration = allSourceFiles.find { it.endsWith(".sqm") }
      if (migration == null) {
        fail(
          "Changes have been made to the DB schema, but no migration has been found. " +
            "Please create one using the [SQDelight Migration Documentation]" +
            "(https://sqldelight.github.io/sqldelight/2.0.2/multiplatform_sqlite/migrations/)",
        )
      } else {
        val oldVersion = MIGRATION_VERSION_REGEX.find(migration)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (oldVersion != null) {
          message("ℹ\uFE0F Migrating Database from `$oldVersion` to `${oldVersion + 1}`")
        }
      }
    } else if (ignoreDbChanges) {
      warn("Ignoring DB schema changes due to `ignore-db-change` label")
    }
  }

  androidLint {
    val moduleLintFilesPaths = find(
      projectDir = ".",
      "lint-results-debug.xml",
      "lint-results-release.xml",
    ).toTypedArray()

    reportDistinct(*moduleLintFilesPaths)
  }
}
