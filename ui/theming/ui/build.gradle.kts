plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.ui.theming.api)
        implementation(projects.infra.socket.api)

        implementation(libs.compose.components.resources)

        implementation(libs.color.picker.compose)
        implementation(libs.circuitx.overlays)
        implementation(libs.cadence)
        implementation(libs.androidx.shapes)
      }
    }
  }
}
