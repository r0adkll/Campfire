plugins {
  id("app.campfire.multiplatform")
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.compose.compiler)
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)
        implementation(compose.runtime)
        implementation(compose.ui)

        api(libs.swatchbuckler.compose)
      }
    }
  }
}
