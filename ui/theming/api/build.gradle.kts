plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.parcelize")
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)
        implementation(projects.common.screens)
        implementation(projects.common.compose)
        implementation(compose.runtime)
        implementation(compose.ui)

        api(libs.swatchbuckler.compose)
      }
    }
  }
}
