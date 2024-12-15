// Copyright 2023, Drew Heavner and the Campfire project contributors
// SPDX-License-Identifier: Apache-2.0

package app.campfire.convention

import app.campfire.convention.util.capitalized
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import org.jetbrains.kotlin.gradle.tasks.KotlinCompileCommon

class KotlinMultiplatformConventionPlugin : Plugin<Project> {
  @OptIn(ExperimentalKotlinGradlePluginApi::class)
  override fun apply(target: Project) = with(target) {
    with(pluginManager) {
      apply("org.jetbrains.kotlin.multiplatform")
    }

    extensions.configure<KotlinMultiplatformExtension> {
      compilerOptions {
        freeCompilerArgs.add("-opt-in=kotlin.uuid.ExperimentalUuidApi")
      }

      applyDefaultHierarchyTemplate()

      jvm()

      if (pluginManager.hasPlugin("com.android.library")) {
        androidTarget()
      }

      iosArm64()
      iosSimulatorArm64()

      targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework { baseName = target.path.substring(1).replace(":", "_") }

        binaries.configureEach {
          // Add linker flag for SQLite. See:
          // https://github.com/touchlab/SQLiter/issues/77
          linkerOpts("-lsqlite3")

          // Workaround for https://youtrack.jetbrains.com/issue/KT-64508
          freeCompilerArgs += "-Xdisable-phases=RemoveRedundantCallsToStaticInitializersPhase"
        }

        compilations.configureEach {
          compileTaskProvider.configure {
            compilerOptions {
              // Various opt-ins
              freeCompilerArgs.addAll(
                "-opt-in=kotlinx.cinterop.ExperimentalForeignApi",
                "-opt-in=kotlinx.cinterop.BetaInteropApi",
                "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
              )
            }
          }
        }
      }

      targets.configureEach {
        compilations.configureEach {
          compileTaskProvider.configure {
            compilerOptions {
              freeCompilerArgs.add("-Xexpect-actual-classes")
            }
          }
        }
      }

      metadata {
        compilations.configureEach {
          if (name == KotlinSourceSet.COMMON_MAIN_SOURCE_SET_NAME) {
            compileTaskProvider.configure {
              // We replace the default library names with something more unique (the project path).
              // This allows us to avoid the annoying issue of `duplicate library name: foo_commonMain`
              // https://youtrack.jetbrains.com/issue/KT-57914
              val projectPath = this@with.path.substring(1).replace(":", "_")
              this as KotlinCompileCommon
              moduleName.set("${projectPath}_commonMain")
            }
          }
        }
      }

      configureSpotless()
      configureKotlin()
    }
  }
}

fun Project.addKspDependencyForAllTargets(dependencyNotation: Any) =
  addKspDependencyForAllTargets("", dependencyNotation)

fun Project.addKspTestDependencyForAllTargets(dependencyNotation: Any) =
  addKspDependencyForAllTargets("Test", dependencyNotation)

fun Project.addKspDependencyForCommon(dependencyNotation: Any) {
  dependencies {
    add("kspCommonMainMetadata", dependencyNotation)
  }

  tasks.withType<KotlinCompilationTask<*>>().configureEach {
    if (name != "kspCommonMainKotlinMetadata") {
      dependsOn("kspCommonMainKotlinMetadata")
    }
  }

  extensions.configure<KotlinMultiplatformExtension> {
    sourceSets["commonMain"].apply {
      kotlin.srcDir("build/generated/ksp/metadata/commonMain/kotlin")
    }
  }
}

private fun Project.addKspDependencyForAllTargets(
  configurationNameSuffix: String,
  dependencyNotation: Any,
) {
  val kmpExtension = extensions.getByType<KotlinMultiplatformExtension>()
  dependencies {
    kmpExtension.targets
      .asSequence()
      .filter { target ->
        // Don't add KSP for common target, only final platforms
        target.platformType != KotlinPlatformType.common
      }
      .forEach { target ->
        add(
          "ksp${target.targetName.capitalized()}$configurationNameSuffix",
          dependencyNotation,
        )
      }
  }
}
