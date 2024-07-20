plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.compose")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(compose.foundation)
        api(libs.kotlinx.coroutines.core)
      }
    }
  }
}
