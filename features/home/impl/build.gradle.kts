import app.campfire.convention.addKspDependencyForCommon

plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.common.settings)
        implementation(projects.core)
        implementation(projects.data.db)
        implementation(projects.data.network.public)
        implementation(projects.data.account.api)

        api(projects.features.home.api)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
