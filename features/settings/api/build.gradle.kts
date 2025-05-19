plugins {
  id("app.campfire.multiplatform")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.core)
        api(libs.multiplatformsettings.core)
        api(libs.multiplatformsettings.coroutines)
      }
    }
  }
}
