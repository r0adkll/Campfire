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
        api(projects.features.search.api)
        implementation(projects.features.user.api)

        implementation(projects.common.settings)
        implementation(projects.core)
        implementation(projects.data.db)
        implementation(projects.data.network.api)
        implementation(projects.data.account.api)
        implementation(projects.data.mapping)
        implementation(libs.store)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
