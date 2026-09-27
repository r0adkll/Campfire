// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.script.di

import com.github.ajalt.clikt.command.CoreSuspendingCliktCommand
import dev.zacsweers.metro.DependencyGraph

@DependencyGraph(ScriptScope::class)
interface ScriptComponent {

  val commands: Set<CoreSuspendingCliktCommand>
}
