// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention.changelog

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Generates the `changelog.json` compose resource from CHANGELOG.md, mirroring the output
 * of `./campfire changelog -aj` without spawning a nested Gradle build (which sandboxed
 * builders like F-Droid's can't run).
 *
 * The `Unreleased` section is included only for alpha/dev builds (dev builds are
 * recognized by the all-nines placeholder version code).
 */
@CacheableTask
abstract class GenerateChangelogTask : DefaultTask() {

  @get:InputFile
  @get:PathSensitive(PathSensitivity.NONE)
  abstract val changelogFile: RegularFileProperty

  @get:Input
  abstract val versionCode: Property<String>

  @get:Input
  abstract val versionName: Property<String>

  @get:OutputFile
  abstract val outputFile: RegularFileProperty

  @TaskAction
  fun generate() {
    val code = versionCode.get()
    val name = versionName.get()
    val includeUnreleased = name.contains("alpha", ignoreCase = true) || code.all { it == '9' }

    val versions = ChangelogParser.parse(changelogFile.get().asFile)
      .filter { includeUnreleased || it.version != "Unreleased" }

    val output = outputFile.get().asFile
    output.parentFile.mkdirs()
    output.writeText(ChangelogParser.toJson(versions))
  }
}
