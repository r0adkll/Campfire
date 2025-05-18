plugins {
  id("app.campfire.multiplatform")
  alias(libs.plugins.metro)
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
