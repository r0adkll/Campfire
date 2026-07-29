import app.campfire.convention.addKspDependencyForCommon
import app.campfire.convention.campfireVersionCode
import app.campfire.convention.campfireVersionName
import java.io.File

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.infra.whatsNew.api)

        implementation(projects.core)
        implementation(projects.features.settings.api)

        implementation(libs.kotlinx.serialization.json)

        implementation(libs.compose.components.resources)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)

val generatedResources = layout.buildDirectory.dir("generated/composeResources")

/**
 * Parses CHANGELOG.md into the `changelog.json` compose resource consumed by
 * WhatsNewRepositoryImpl, mirroring the output of `./campfire changelog -aj` without
 * spawning a nested Gradle build (which sandboxed builders like F-Droid's can't run).
 */
abstract class GenerateChangelogTask : DefaultTask() {

  @get:InputFile
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

    val versions = parse(changelogFile.get().asFile)
      .filter { includeUnreleased || it.version != "Unreleased" }

    val output = outputFile.get().asFile
    output.parentFile.mkdirs()
    output.writeText(versions.joinToString(separator = ",", prefix = "[", postfix = "]") { it.toJson() })
  }

  private data class Version(val version: String, val date: String?, val sets: List<ChangeSet>)
  private data class ChangeSet(val name: String?, val changes: List<String>)

  // Same algorithm as ChangelogParser in scripts/app (ParseChangelogCommand.kt) — keep
  // the two in sync if the CHANGELOG.md structure ever changes.
  private fun parse(file: File): List<Version> {
    val versions = mutableListOf<Version>()
    val changeSets = mutableListOf<ChangeSet>()

    var currentVersion: String? = null
    var currentDate: String? = null
    var currentChangeSet: String? = null
    val currentChanges = mutableListOf<String>()

    file.useLines { lines ->
      lines.forEach { line ->
        if (line.isBlank()) return@forEach
        if (line.startsWith("# ")) return@forEach
        if (VERSION_KEY_REGEX.matches(line)) return@forEach

        if (line.startsWith("## ")) {
          if (currentVersion != null) {
            changeSets += ChangeSet(currentChangeSet, currentChanges.toList())
            versions += Version(currentVersion!!, currentDate, changeSets.toList())

            currentVersion = null
            currentDate = null
            currentChangeSet = null
            changeSets.clear()
            currentChanges.clear()
          }

          val match = VERSION_REGEX.find(line)
          if (match == null || match.groupValues.size < 2) error("Unable to find version in '$line'")
          currentVersion = match.groupValues[1]
          currentDate = match.groupValues.getOrNull(3)
        } else if (currentVersion != null) {
          if (line.startsWith("### ")) {
            if (currentChangeSet != null) {
              changeSets += ChangeSet(currentChangeSet, currentChanges.toList())
              currentChanges.clear()
            }
            currentChangeSet = line.removePrefix("### ").trim()
          } else {
            currentChanges += line.trim().removePrefix("- ")
          }
        }
      }
    }

    return versions
  }

  private fun Version.toJson(): String = buildString {
    append("{\"version\":").append(version.toJsonString())
    append(",\"date\":").append(date?.toJsonString() ?: "null")
    append(",\"changes\":[")
    sets.forEachIndexed { i, set ->
      if (i > 0) append(',')
      append("{\"name\":").append(set.name?.toJsonString() ?: "null")
      append(",\"changes\":[")
      set.changes.forEachIndexed { j, change ->
        if (j > 0) append(',')
        append(change.toJsonString())
      }
      append("]}")
    }
    append("]}")
  }

  private fun String.toJsonString(): String = buildString {
    append('"')
    for (c in this@toJsonString) {
      when {
        c == '"' -> append("\\\"")
        c == '\\' -> append("\\\\")
        c == '\n' -> append("\\n")
        c == '\r' -> append("\\r")
        c == '\t' -> append("\\t")
        c < ' ' -> append("\\u%04x".format(c.code))
        else -> append(c)
      }
    }
    append('"')
  }

  private companion object {
    val VERSION_REGEX = "^## \\[([a-zA-Z0-9._-]+)\\]( - (\\d{4}-\\d{2}-\\d{2}))?$".toRegex()
    val VERSION_KEY_REGEX = "^\\[([a-zA-Z0-9._-]+)\\]:.*$".toRegex()
  }
}

val resolvedVersionCode = campfireVersionCode().toString()
val resolvedVersionName = campfireVersionName()

val generateChangelog by tasks.registering(GenerateChangelogTask::class) {
  changelogFile.set(rootProject.file("CHANGELOG.md"))
  versionCode.set(resolvedVersionCode)
  versionName.set(resolvedVersionName)
  outputFile.set(generatedResources.map { it.file("files/changelog.json") })

  outputs.cacheIf { true }
}

compose.resources {
  customDirectory(
    sourceSetName = "commonMain",
    directoryProvider = generatedResources,
  )
}

tasks.matching {
  it.name.startsWith("generateComposeResClass") ||
    it.name.startsWith("copyNonXmlValueResourcesFor") ||
    it.name.startsWith("prepareComposeResourcesTaskFor") ||
    it.name.startsWith("generateResourceAccessorsFor")
}.configureEach {
  dependsOn(generateChangelog)
}
