// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.get
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

class UiConventionPlugin : Plugin<Project> {
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  override fun apply(target: Project) = with(target) {
    // Apply other conventions
    with(pluginManager) {
      apply("app.campfire.android.library")
      apply("app.campfire.multiplatform")
      apply("app.campfire.compose")
      libs.findPlugin("ksp").ifPresent { apply(it.get().pluginId) }
    }

    extensions.configure<KotlinMultiplatformExtension> {
      compilerOptions {
        freeCompilerArgs.add("-opt-in=androidx.compose.material3.ExperimentalMaterial3Api")
      }

      sourceSets["commonMain"].dependencies {
        /*
         * This brings in the necessary transitive dependencies for building screens
         * and ui. It contains mostly compose-based deps as well as a few common/core project
         * modules.
         */
        implementation(project(":common:compose"))

        val compose = ComposePlugin.Dependencies(project)
        implementation(compose.runtime)
        implementation(compose.ui)

        // Add Circuit Dependencies
        libs.findLibrary("circuit-runtime").ifPresent { implementation(it) }

        // Add DI / Kimchi Dependencies
        libs.findLibrary("kimchi-annotations").ifPresent { implementation(it) }
        libs.findLibrary("kimchi-circuit-annotations").ifPresent { implementation(it) }
      }
      sourceSets["commonTest"].dependencies {
        libs.findLibrary("kotlin-test").ifPresent { implementation(it) }
      }
    }

    // Add DI / Kimchi KSP compilers
    libs.findLibrary("kimchi-compiler").ifPresent { addKspDependencyForAllTargets(it) }
    libs.findLibrary("kimchi-circuit-compiler").ifPresent { addKspDependencyForAllTargets(it) }
  }
}
