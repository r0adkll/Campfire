plugins {
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)
        implementation(compose.runtime)
      }
    }
  }
}
