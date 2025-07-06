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

        implementation(compose.runtime)
        implementation(compose.ui)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
