plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
}

@OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
kotlin {
  sourceSets {
    commonMain {
      dependencies {
      }
    }

    androidMain {
      dependencies {
        implementation(libs.androidx.tracing)
        implementation(libs.androidx.tracing.android)
      }
    }
  }
}
