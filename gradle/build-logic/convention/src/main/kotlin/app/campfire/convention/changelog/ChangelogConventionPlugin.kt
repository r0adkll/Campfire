// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.convention.changelog

import app.campfire.convention.campfireVersionCode
import app.campfire.convention.campfireVersionName
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.register
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension

/**
 * Wires CHANGELOG.md into the applying module as a generated `changelog.json` compose
 * resource: registers [GenerateChangelogTask], exposes its output directory as a custom
 * commonMain compose resource directory, and orders it before the compose resource tasks.
 *
 * Requires the `app.campfire.compose` convention (or the `org.jetbrains.compose` plugin)
 * on the same module.
 */
class ChangelogConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = with(target) {
    val generatedResources = layout.buildDirectory.dir("generated/composeResources")

    val resolvedVersionCode = campfireVersionCode().toString()
    val resolvedVersionName = campfireVersionName()

    val generateChangelog = tasks.register<GenerateChangelogTask>("generateChangelog") {
      changelogFile.set(rootProject.file("CHANGELOG.md"))
      versionCode.set(resolvedVersionCode)
      versionName.set(resolvedVersionName)
      outputFile.set(generatedResources.map { it.file("files/changelog.json") })
    }

    pluginManager.withPlugin("org.jetbrains.compose") {
      extensions.configure<ComposeExtension> {
        extensions.configure<ResourcesExtension> {
          customDirectory(
            sourceSetName = "commonMain",
            directoryProvider = generatedResources,
          )
        }
      }
    }

    tasks.matching {
      it.name.startsWith("generateComposeResClass") ||
        it.name.startsWith("copyNonXmlValueResourcesFor") ||
        it.name.startsWith("prepareComposeResourcesTaskFor") ||
        it.name.startsWith("generateResourceAccessorsFor")
    }.configureEach {
      dependsOn(generateChangelog)
    }
  }
}
