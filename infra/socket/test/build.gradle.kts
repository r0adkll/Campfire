plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.infra.socket.api)

        implementation(libs.kotlinx.coroutines.core)
      }
    }
  }
}
