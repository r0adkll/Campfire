plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(libs.kotlinx.serialization.json)
        api(libs.kotlinx.io.bytestring)
      }
    }
  }
}
