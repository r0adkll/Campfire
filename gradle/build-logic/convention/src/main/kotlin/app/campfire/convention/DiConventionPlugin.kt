// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import dev.zacsweers.metro.gradle.ExperimentalMetroGradleApi
import dev.zacsweers.metro.gradle.MetroPluginExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Applies the Metro dependency injection compiler plugin, with Metro's built-in Circuit code
 * generation for `@CircuitInject` presenters and UIs.
 *
 * Apply this to every module that declares or contributes bindings. Pass
 * `-Pcampfire.metro.reports=true` to write Metro's graph reports to `build/reports/metro`.
 */
class DiConventionPlugin : Plugin<Project> {
  @OptIn(ExperimentalMetroGradleApi::class)
  override fun apply(target: Project) = with(target) {
    pluginManager.apply("dev.zacsweers.metro")

    extensions.configure<MetroPluginExtension> {
      enableCircuitCodegen.set(true)

      if (providers.gradleProperty("campfire.metro.reports").orNull == "true") {
        reportsDestination.set(layout.buildDirectory.dir("reports/metro"))
      }
    }
  }
}
