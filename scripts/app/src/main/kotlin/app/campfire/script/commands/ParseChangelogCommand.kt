package app.campfire.script.commands

import app.campfire.script.CampfireConfig
import app.campfire.script.di.ScriptScope
import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import java.io.File
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(
  scope = ScriptScope::class,
  boundType = CoreSuspendingCliktCommand::class,
)
@Inject
class ParseChangelogCommand : SuspendingCliktCommand(
  name = "changelog",
) {

  private val version by option(
    "--version",
    help = "Output the changes ONLY for this version. Otherwise prints latest version",
  )
  private val json by option("-j", "--json", help = "Output the requested version in JSON format").flag()
  private val all by option("-a", "--all", help = "Output ALL versions").flag()

  private val config by requireObject<CampfireConfig>()

  override suspend fun run() {
    // Find CHANGELOG.md
    val changelogFile = File("CHANGELOG.md")
    if (!changelogFile.exists()) {
      throw IllegalArgumentException(
        "Unable to find 'CHANGELOG.md'. " +
          "Make sure you are executing this in the project directory",
      )
    }

    // Read file, and scan contents
    val changelog = ChangelogParser.parse(changelogFile)

    // Convert to JSON for output
    val output = ChangelogParser.format(
      changelog = changelog,
      verbose = config.verbose,
      jsonFormat = json,
      version = version?.let {
        ChangelogParser.OutputVersion.Version(it)
      } ?: if (all) {
        ChangelogParser.OutputVersion.All
      } else {
        ChangelogParser.OutputVersion.Latest
      },
    )
    echo(output)
  }
}

object ChangelogParser {

  fun parse(file: File): Changelog {
    val changes = mutableListOf<Changelog.Changes>()
    val changeSets = mutableListOf<Changelog.ChangeSet>()

    var currentVersion: String? = null
    var currentDate: String? = null
    var currentChangeSet: String? = null
    var currentChanges = mutableListOf<String>()

    file.useLines { lines ->
      lines.forEach { line ->
        // Check pre-requisite, or other ignored lines
        if (line.isBlank()) return@forEach
        if (line.startsWith("# ")) return@forEach
        if (VERSION_KEY_REGEX.matches(line)) return@forEach

        // Check if we are starting a new section. If so collect anyone perviously being built
        // and reset for new section
        if (line.startsWith("## ")) {
          // Add current change builder
          if (currentVersion != null) {
            changeSets += Changelog.ChangeSet(
              name = currentChangeSet,
              changes = currentChanges.toList(),
            )

            changes += Changelog.Changes(
              version = currentVersion!!,
              date = currentDate,
              changes = changeSets.toList(),
            )

            currentVersion = null
            currentDate = null
            currentChangeSet = null
            changeSets.clear()
            currentChanges.clear()
          }

          // Use Regex to parse version / date
          val match = VERSION_REGEX.find(line)
          if (match == null || match.groupValues.size < 2) error("Unable to find version in '$line'")
          currentVersion = match.groupValues[1]
          currentDate = match.groupValues.getOrNull(3)
        } else if (currentVersion != null) {
          // Check for change sets in a given version
          if (line.startsWith("### ")) {
            // If we were previously building a change set, cap it and reset for next
            if (currentChangeSet != null) {
              changeSets += Changelog.ChangeSet(
                name = currentChangeSet,
                changes = currentChanges.toList(),
              )

              currentChangeSet = null
              currentChanges.clear()
            }

            currentChangeSet = line.removePrefix("### ").trim()
          } else {
            currentChanges += line.trim().removePrefix("- ")
          }
        }
      }
    }

    return Changelog(changes)
  }

  sealed interface OutputVersion {
    data object All : OutputVersion
    data object Latest : OutputVersion
    data class Version(val version: String) : OutputVersion
  }

  sealed interface Output {
    data class Single(val changes: Changelog.Changes) : Output
    data class Multiple(val changes: List<Changelog.Changes>) : Output
  }

  @OptIn(ExperimentalSerializationApi::class)
  fun format(
    changelog: Changelog,
    verbose: Boolean = false,
    jsonFormat: Boolean = false,
    version: OutputVersion = OutputVersion.Latest,
  ): String? {
    val output = when (version) {
      OutputVersion.All -> Output.Multiple(changelog.changes.filter { it.version != "Unreleased" })
      OutputVersion.Latest -> changelog.changes.firstOrNull { it.version != "Unreleased" }?.let { Output.Single(it) }
      is OutputVersion.Version -> changelog.changes.find { it.version == version.version }?.let { Output.Single(it) }
    } ?: return null

    return when (output) {
      is Output.Single -> {
        if (jsonFormat) {
          val json = Json {
            prettyPrint = verbose
            if (verbose) prettyPrintIndent = "  "
          }
          json.encodeToString(output.changes)
        } else {
          output.changes.asMarkdownString()
        }
      }
      is Output.Multiple -> {
        if (jsonFormat) {
          val json = Json {
            prettyPrint = verbose
            if (verbose) prettyPrintIndent = "  "
          }
          json.encodeToString(output.changes)
        } else {
          output.changes.joinToString("\n") {
            it.asMarkdownString()
          }
        }
      }
    }
  }

  private fun Changelog.Changes.asMarkdownString(): String = buildString {
    val nonEmptyChanges = changes.filter { it.changes.isNotEmpty() }
    nonEmptyChanges.forEachIndexed { index, (name, changes) ->
      appendLine("### $name")
      appendLine("  ")
      changes.forEach { change ->
        appendLine("- $change")
      }
      if (index != nonEmptyChanges.lastIndex) {
        appendLine("  ")
      }
    }
  }
}

@Serializable
data class Changelog(
  val changes: List<Changes>,
) {

  @Serializable
  data class Changes(
    val version: String,
    val date: String?,
    val changes: List<ChangeSet>,
  )

  @Serializable
  data class ChangeSet(
    val name: String?,
    val changes: List<String>,
  )
}

private val VERSION_REGEX = "^## \\[([a-zA-Z0-9._-]+)\\]( - (\\d{4}-\\d{2}-\\d{2}))?$".toRegex()
private val VERSION_KEY_REGEX = "^\\[([a-zA-Z0-9._-]+)\\]:.*$".toRegex()
