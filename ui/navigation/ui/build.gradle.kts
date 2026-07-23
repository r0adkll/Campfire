plugins {
  id("app.campfire.ui")
}

kotlin {
  sourceSets {
    commonMain {
      dependencies {
        api(projects.ui.navigation.api)

        implementation(projects.data.account.api)
        implementation(projects.infra.updates.api)
        implementation(projects.infra.whatsNew.api)
        implementation(projects.features.libraries.api)
        implementation(projects.features.playlists.api)
        implementation(projects.features.podcasts.api)
        implementation(projects.ui.theming.api)

        implementation(libs.compose.components.resources)

        implementation(libs.reorderable)
      }
    }
  }
}
