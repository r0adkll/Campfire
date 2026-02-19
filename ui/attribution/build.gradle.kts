plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.common.compose)

        implementation(libs.about.libraries.compose.m3)
        implementation(libs.compose.components.resources)
      }
    }
  }
}
