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
        api(projects.data.analytics.api)

        implementation(projects.core)
        implementation(projects.features.settings.api)
      }
    }

    commonTest {
      dependencies {
        implementation(libs.kotlin.test)
      }
    }
  }
}

addKspDependencyForCommon(libs.kimchi.compiler)
