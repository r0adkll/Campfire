plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        implementation(projects.features.playlists.api)
        implementation(projects.ui.appbar)
        implementation(projects.ui.navigation)
      }
    }
  }
}
