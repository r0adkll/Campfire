@file:Suppress("ktlint:standard:filename")

package app.campfire.script

import app.campfire.script.di.ScriptComponent
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.subcommands
import kimchi.merge.app.campfire.script.di.createScriptComponent

suspend fun main(args: Array<String>) {
  val component = ScriptComponent::class.createScriptComponent()
  CampfireCli()
    .subcommands(component.commands)
    .main(args)
}
