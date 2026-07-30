// Copyright 2026, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: GPL-3.0-only

package app.campfire.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType

class ParcelizeConventionPlugin : Plugin<Project> {
  override fun apply(target: Project) = with(target) {
    pluginManager.apply("org.jetbrains.kotlin.plugin.parcelize")

    extensions.configure<KotlinMultiplatformExtension> {
      targets.configureEach {
        val isAndroidTarget = platformType == KotlinPlatformType.androidJvm
        compilations.configureEach {
          compileTaskProvider.configure {
            compilerOptions {
              if (isAndroidTarget) {
                freeCompilerArgs.addAll(
                  "-P",
                  "plugin:org.jetbrains.kotlin.parcelize:additionalAnnotation=app.campfire.core.parcelize.Parcelize",
                )
              }
            }
          }
        }
      }
    }
  }
}
