plugins {
  id("app.campfire.multiplatform")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.androidx.paging.common)

        implementation(projects.core)
      }
    }
  }
}
