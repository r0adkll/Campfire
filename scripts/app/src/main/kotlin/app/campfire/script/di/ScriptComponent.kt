package app.campfire.script.di

import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import com.r0adkll.kimchi.annotations.MergeComponent

@MergeComponent(ScriptScope::class)
interface ScriptComponent {

  val commands: Set<CoreSuspendingCliktCommand>
}
