package app.campfire.script.gradle

import app.campfire.script.util.ProcessResult
import app.campfire.script.util.ProcessScope
import app.campfire.script.util.runtime
import com.github.ajalt.clikt.core.BaseCliktCommand
import java.nio.file.Path

suspend fun <Output> BaseCliktCommand<*>.gw(
  vararg arguments: String,
  quiet: Boolean = false,
  block: suspend ProcessScope.() -> Output,
): ProcessResult<Output> {
  if (!quiet) echo("gw ${arguments.joinToString(" ")}", trailingNewline = true)
  val commands = arrayOf("./gradlew", *arguments)
  return runtime(commands = commands, block = block)
}

suspend fun BaseCliktCommand<*>.gw(
  vararg arguments: String,
  quiet: Boolean = false,
): ProcessResult<String> = gw(
  arguments = arguments,
  quiet = quiet,
  block = { process.inputStream.bufferedReader().readText() },
)

fun Path.asGradleModulePath(): String {
  return joinToString(":", prefix = ":")
}
