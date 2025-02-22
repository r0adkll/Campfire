@file:Suppress("ktlint:standard:filename")

package app.campfire.script

import app.campfire.script.di.ScriptComponent
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import kimchi.merge.app.campfire.script.di.createScriptComponent

class Hello : CliktCommand() {

  private val name by argument(
    help = "The person to greet",
  )

  override fun run() {
    echo("Hello $name!")
  }
}

suspend fun main(args: Array<String>) {
  val component = ScriptComponent::class.createScriptComponent()
  CampfireCli()
    .subcommands(component.commands)
    .main(args)
}
