import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.infra.updates.api)

        implementation(projects.core)
        implementation(projects.common.compose)
        implementation(projects.features.settings.api)

        implementation(libs.circuit.overlay)
        implementation(libs.circuitx.overlays)
        implementation(libs.compose.runtime)
        implementation(libs.compose.ui)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
