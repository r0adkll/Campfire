// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.convention

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.jetbrains.compose.ComposePlugin
import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetTree

class UiConventionPlugin : Plugin<Project> {
  @OptIn(ExperimentalKotlinGradlePluginApi::class, ExperimentalComposeLibrary::class)
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

      // Setup android instrumented tests for commonTest UI tests
      androidTarget {
        instrumentedTestVariant.sourceSetTree.set(KotlinSourceSetTree.test)
      }

      val compose = ComposePlugin.Dependencies(project)

      sourceSets["commonMain"].dependencies {
        /*
         * This brings in the necessary transitive dependencies for building screens
         * and ui. It contains mostly compose-based deps as well as a few common/core project
         * modules.
         */
        implementation(project(":common:compose"))

        implementation(compose.runtime)
        implementation(compose.ui)
        implementation(compose.components.resources)
        implementation(compose.components.uiToolingPreview)

        // Add Circuit Dependencies
        libs.findLibrary("circuit-runtime").ifPresent { implementation(it) }

        // Add DI / Kimchi Dependencies
        libs.findLibrary("kimchi-annotations").ifPresent { implementation(it) }
        libs.findLibrary("kimchi-circuit-annotations").ifPresent { implementation(it) }
      }

      sourceSets["commonTest"].dependencies {
        implementation(project(":common:test"))
        libs.findBundle("test-common").ifPresent { implementation(it) }
        libs.findBundle("test-ui").ifPresent { implementation(it) }
        implementation(compose.uiTest)
      }

      sourceSets["jvmTest"].dependencies {
        implementation(compose.desktop.currentOs)
      }
    }

    // Add DI / Kimchi KSP compilers
    libs.findLibrary("kimchi-compiler").ifPresent { addKspDependencyForAllTargets(it) }
    libs.findLibrary("kimchi-circuit-compiler").ifPresent { addKspDependencyForAllTargets(it) }

    // Setup Android instrumentation test dependencies
    dependencies {
      libs.findLibrary("androidx-compose-ui-test-junit4").ifPresent { add("androidTestImplementation", it) }
      libs.findLibrary("androidx-compose-ui-test-manifest").ifPresent { add("debugImplementation", it) }
    }
  }
}
