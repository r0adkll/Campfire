package app.campfire.script.gradle

import com.github.ajalt.clikt.core.BaseCliktCommand
import java.nio.file.FileVisitResult
import java.nio.file.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.name
import kotlin.io.path.pathString
import kotlin.io.path.readLines
import kotlin.io.path.visitFileTree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class GradleModule(
  val name: String,
  val config: Configuration,
) {

  data class Configuration(
    val plugins: List<Plugin>,
    val dependencies: List<Dependency>,
  )

  sealed interface Plugin {
    val id: String

    data class Id(override val id: String) : Plugin
    data class Alias(override val id: String) : Plugin
  }

  sealed interface Dependency {
    val id: String

    data class Api(override val id: String) : Dependency
    data class Implementation(override val id: String) : Dependency
    data class Ksp(override val id: String) : Dependency
    data class Unknown(
      val type: String,
      override val id: String,
    ) : Dependency
  }
}

private val skippableFolders = listOf(
  "gradle",
  ".gradle",
  ".git",
  ".kotlin",
  ".idea",
  "spotless",
  "build",
  "src",
)

suspend fun BaseCliktCommand<*>.gradleModules(
  quiet: Boolean = true,
): List<GradleModule> = withContext(Dispatchers.IO) {
  val modules = mutableListOf<Path>()

  Path.of("")
    .also {
      if (!quiet) echo("Scanning '${it.absolutePathString()}'")
    }
    .visitFileTree {
      onPreVisitDirectory { directory, _ ->
        if (directory.name in skippableFolders) {
          FileVisitResult.SKIP_SUBTREE
        } else FileVisitResult.CONTINUE
      }

      onVisitFile { file, _ ->
        if (file.name == "build.gradle.kts") {
          val isRoot = file.parent == null
          if (!isRoot) {
            val gradleModuleName = file.parent.asGradleModulePath()
            if (!quiet) echo("${file.pathString} --> $gradleModuleName")

            modules.add(file)
            FileVisitResult.SKIP_SIBLINGS
          } else {
            FileVisitResult.CONTINUE
          }
        } else {
          FileVisitResult.CONTINUE
        }
      }
    }

  modules.map { gradleFile ->
    val moduleName = gradleFile.parent.asGradleModulePath()
    val configuration = parseGradleConfiguration(gradleFile)
    GradleModule(
      name = moduleName,
      config = configuration,
    )
  }
}

suspend fun parseGradleConfiguration(buildFile: Path): GradleModule.Configuration = withContext(Dispatchers.IO) {
  var isInPluginBlock = false
  var isInDependencyBlock = false

  val plugins = mutableListOf<GradleModule.Plugin>()
  val dependencies = mutableListOf<GradleModule.Dependency>()

  buildFile.readLines().forEach { line ->
    if (isInPluginBlock) {
      if (BlockEnd.matches(line)) {
        isInPluginBlock = false
      } else {
        Plugin.find(line)?.let { match ->
          val type = match.groups[1]!!.value
          val id = match.groups[2]!!.value
          plugins += when (type) {
            "id" -> GradleModule.Plugin.Id(id)
            "alias" -> GradleModule.Plugin.Alias(id)
            else -> throw IllegalStateException("Unknown plugin type: $type")
          }
        }
      }
    } else if (isInDependencyBlock) {
      if (BlockEnd.matches(line)) {
        isInDependencyBlock = false
      } else {
        Dependency.find(line)?.let { match ->
          val type = match.groups[1]!!.value
          val id = match.groups[2]!!.value
          dependencies += when (type) {
            "api" -> GradleModule.Dependency.Api(id)
            "implementation" -> GradleModule.Dependency.Implementation(id)
            "ksp" -> GradleModule.Dependency.Ksp(id)
            else -> GradleModule.Dependency.Unknown(type, id)
          }
        }
      }
    } else {
      if (PluginBlockStart.matches(line)) {
        isInPluginBlock = true
      } else if (DependencyBlockStart.matches(line)) {
        isInDependencyBlock = true
      }
    }
  }

  // return
  GradleModule.Configuration(
    plugins = plugins,
    dependencies = dependencies,
  )
}

private val PluginBlockStart = "^plugins \\{$".toRegex()
private val DependencyBlockStart = "\\s*dependencies \\{$".toRegex()
private val BlockEnd = "\\s*\\}$".toRegex()
private val Plugin = "(id|alias)\\(\"?([a-zA-Z0-9.]+)\"?\\)\$".toRegex()
private val Dependency = "(\\w+)\\(\"?([a-zA-Z0-9.]+)\"?\\)\$".toRegex()
