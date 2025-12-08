plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.ui.theming.api)

        implementation(compose.components.resources)

        implementation(libs.color.picker.compose)
        implementation(libs.circuitx.overlays)
      }
    }
  }
}
