// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

@file:Suppress("ktlint:standard:filename")

package app.campfire.script

import app.campfire.script.di.ScriptComponent
import com.github.ajalt.clikt.command.main
import com.github.ajalt.clikt.core.subcommands
import dev.zacsweers.metro.createGraph

suspend fun main(args: Array<String>) {
  val component = createGraph<ScriptComponent>()
  CampfireCli()
    .subcommands(component.commands)
    .main(args)
}
