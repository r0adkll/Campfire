plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.playlists.api)
        implementation(projects.features.sessions.api)
        implementation(projects.infra.audioplayer.api)
        implementation(projects.ui.appbar)
        implementation(projects.ui.navigation.api)

        implementation(libs.circuitx.overlays)
        implementation(libs.reorderable)
      }
    }
  }
}
