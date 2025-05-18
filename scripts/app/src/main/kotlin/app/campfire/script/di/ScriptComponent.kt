package app.campfire.script.di

import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import dev.zacsweers.metro.DependencyGraph

@DependencyGraph(ScriptScope::class)
interface ScriptComponent {

  val commands: Set<CoreSuspendingCliktCommand>
}
