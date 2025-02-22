package app.campfire.script

import com.github.ajalt.clikt.command.SuspendingCliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option

data class CampfireConfig(
  var verbose: Boolean = false,
)

class CampfireCli : SuspendingCliktCommand(
  name = "campfire",
) {

  private val verbose by option(
    "-v",
    "--verbose",
    help = "Print out more verbose logging for commands",
  ).flag()
  private val config by findOrSetObject { CampfireConfig() }

  override fun help(context: Context): String = """
    This is the Campfire CLI for various related tasks, cleanup, and other utilities relating to this project.
  """.trimIndent()

  override suspend fun run() {
    config.verbose = verbose
  }
}
