import app.campfire.convention.addKspDependencyForAllTargets

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.infra.socket.api)

        implementation(projects.core)
        implementation(projects.data.account.api)
        implementation(projects.features.settings.api)
        implementation(libs.kmp.socketio)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.serialization.json)
      }
    }
  }
}

addKspDependencyForAllTargets(libs.kimchi.compiler)
