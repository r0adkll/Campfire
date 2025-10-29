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
        implementation(projects.data.mapping)
        implementation(projects.data.network.api)
        implementation(projects.data.account.api)
        implementation(projects.features.user.api)

        implementation(libs.store)

        api(projects.features.home.api)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
