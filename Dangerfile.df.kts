@file:DependsOn("com.gianluz:danger-kotlin-android-lint-plugin:0.1.0")

import com.gianluz.dangerkotlin.androidlint.AndroidLint
import com.gianluz.dangerkotlin.androidlint.androidLint
import java.io.File
import systems.danger.kotlin.*
import systems.danger.kotlin.models.github.GitHubUserType

private val MIGRATION_VERSION_REGEX = "([0-9]+)\\.sqm\$".toRegex()
private val OLD_DB_VERSION_REGEX = "OLD_DB_VERSION = (\\d+)\$".toRegex(RegexOption.MULTILINE)

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

        // Check if the user has updated the migration code
        val databaseFactory = File(".", "data/db/core/src/commonMain/kotlin/app/campfire/db/DatabaseFactory.kt")
        val dbFactoryContent = databaseFactory.readText()

        val oldDbVersion = OLD_DB_VERSION_REGEX.find(dbFactoryContent)?.groupValues?.getOrNull(1)?.toIntOrNull()
        if (oldDbVersion == null || oldDbVersion != oldVersion) {
          // Either we failed to find the old version, or it doesn't match the schema migration
          fail(
            "Migration version not updated in `DatabaseFactory.kt`: Found [$oldDbVersion] but expected [$oldVersion]",
          )
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
