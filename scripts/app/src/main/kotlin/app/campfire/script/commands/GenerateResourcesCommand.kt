package app.campfire.script.commands

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.campfire.script.CampfireConfig
import app.campfire.script.di.ScriptScope
import app.campfire.script.gradle.asGradleModulePath
import app.campfire.script.gradle.gradleModules
import app.campfire.script.gradle.gw
import app.campfire.script.ui.SingleOptionPicker
import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.optional
import com.github.ajalt.clikt.parameters.types.path
import com.jakewharton.mosaic.runMosaic
import com.r0adkll.kimchi.annotations.ContributesMultibinding
import java.nio.file.Path
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.tatarka.inject.annotations.Inject

@ContributesMultibinding(
  scope = ScriptScope::class,
  boundType = CoreSuspendingCliktCommand::class,
)
@Inject
class GenerateResourcesCommand : SuspendingCliktCommand(
  name = "resources",
) {

  private val module: Path? by argument(
    help = "The name of the module to generate resources for",
  ).path(
    mustExist = true,
    canBeFile = false,
    canBeDir = true,
  ).optional()

  private val config by requireObject<CampfireConfig>()

  override suspend fun run() {
    module?.let { modulePath ->
      // Validate that the path is a valid gradle module by checking for 'build.gradle.kts'
      val isGradleModule = modulePath.toFile().listFiles { _, name ->
        name.startsWith("build.gradle")
      }?.isNotEmpty() == true

      if (isGradleModule) {
        val module = modulePath.asGradleModulePath()
        generateResourcesForModule(module)
      }
    } ?: run {
      var selectedModule: String? = null
      runMosaic {
        var modules by remember { mutableStateOf(persistentListOf<String>()) }

        // Load available gradle modules that are CMP resource enabled
        LaunchedEffect(Unit) {
          modules = gradleModules()
            .filter {
              it.config.dependencies.any { dep -> dep.id == "compose.components.resources" }
            }
            .map { it.name }
            .sorted()
            .toPersistentList()
        }

        SingleOptionPicker(
          title = "Pick module to generate resources for",
          options = modules,
          onOptionSelected = { module ->
            selectedModule = module
          },
        )
      }

      if (selectedModule != null) {
        echo("Selected: '$selectedModule'")
        generateResourcesForModule(selectedModule!!)
      }
    }
  }

  private suspend fun generateResourcesForModule(
    module: String,
  ) = withContext(Dispatchers.IO) {
    if (config.verbose) echo("Generating resources for '$module'")

    // Find all the `generateResourceAccessorsFor` tasks in the module
    val result = gw("$module:tasks", "--all") {
      process.inputReader()
        .lineSequence()
        .filter { it.startsWith("generateResourceAccessorsFor") }
        .toList()
    }

    if (result.isSuccess) {
      val generateTasks = buildList {
        add("$module:generateComposeResClass")
        result.output.forEach {
          add("$module:${it.trim()}")
        }
      }.toTypedArray()

      if (config.verbose) {
        echo("Preparing generate tasks:")
        generateTasks.forEach {
          echo(" ~~> $it")
        }
      }

      gw(*generateTasks, quiet = true)
        .onSuccess {
          echo("Successfully generated resources for $module")
        }
        .onFailure {
          echo("Failed to generate resources for $module", err = true)
        }
    } else {
      echoFormattedHelp(
        CliktError(
          message = "Failed to find any generateResourceAccessorsFor tasks in $module",
          statusCode = result.code,
        ),
      )
    }
  }
}
