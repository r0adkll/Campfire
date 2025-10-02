plugins {
  id("app.campfire.android.library")
  id("app.campfire.multiplatform")
  id("app.campfire.parcelize")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.core)
        api(libs.circuit.runtime)
        api(libs.circuit.retained)
      }
    }
  }
}
