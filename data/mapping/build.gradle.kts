import app.campfire.convention.addKspDependencyForCommon

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
        implementation(projects.core)
        implementation(projects.data.account.api)
        implementation(projects.data.db)
        implementation(projects.data.network.public)
        implementation(libs.store)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
