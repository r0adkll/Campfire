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
        implementation(projects.features.settings.api)
        implementation(projects.core)
        implementation(projects.data.db)
        implementation(projects.data.network.api)
        implementation(projects.data.account.api)
        implementation(projects.data.mapping)

        api(projects.features.auth.api)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
